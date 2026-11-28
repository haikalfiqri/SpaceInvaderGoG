package entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * GOG-105: standalone combat model for the stationary post-Level-40 weapon.
 * All timings are logical frames at 60 FPS; no wall clock or save dependencies.
 * The preview owns this model; normal Story Mode is not wired to it yet.
 */
public final class SuperweaponFight {
    public static final int SHIELD_HP = 22;
    public static final int HULL_HP = 36;
    public static final int NORMAL_LASER_CHARGE = 66;
    public static final int MEGA_LASER_CHARGE = 120;
    public static final int NORMAL_LASER_ACTIVE = 24;
    public static final int MEGA_LASER_ACTIVE = 48;
    public static final int MAX_DRONES = 5;
    public static final int BOMB_RADIUS = 36;
    public static final int PLAYER_INVULNERABILITY = 60;
    public static final int PLAYER_WIDTH = 26, PLAYER_HEIGHT = 16;
    private static final int[] PORT_X = {7,36,63,87,136,160,187,217};
    private static final int[] PORT_Y = {49,62,73,82,82,73,62,49};
    private static final int[] LASER_ORDER = {0,1,2,3,4,5,6,7};
    private static final Color RED = new Color(239,49,54);
    private static final Color ICE = new Color(74,185,232);
    private static final Color VIOLET = new Color(185,138,255);
    private static final Color AMBER = new Color(255,172,65);

    public enum Attack {
        SINGLE_LASER, ALL_LASERS, MEGA_LASER, BOMB, DRONES, HOMING,
        SWEEP_RIGHT, SWEEP_LEFT, CONVERGE, REFLECT_SHIELD
    }
    public enum Kind { NORMAL, FREEZE, FAST, SPLIT, VULNERABLE, ACCELERATED, REFLECTED, HOMING }
    public static final class Shot {
        public double x,y,vx,vy;
        public final Kind kind;
        private int age;
        Shot(double x,double y,double vx,double vy,Kind kind) {
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.kind=kind;
        }
    }
    public static final class Beam {
        public final int port, start, fire, end;
        public final boolean mega;
        Beam(int port,int start,boolean mega) {
            this.port=port;this.start=start;this.mega=mega;
            this.fire=start+(mega?MEGA_LASER_CHARGE:NORMAL_LASER_CHARGE);
            this.end=fire+(mega?MEGA_LASER_ACTIVE:NORMAL_LASER_ACTIVE);
        }
    }
    public static final class Bomb {
        public double x,y;
        public final double ground;
        Bomb(double x,double y,double ground) {this.x=x;this.y=y;this.ground=ground;}
    }
    public static final class Drone {
        public double x,y;
        public final Kind kind;
        private final double homeY;
        private double vx;
        private int nextShot;
        Drone(double x,double y,double homeY,double vx,Kind kind,int nextShot) {
            this.x=x;this.y=y;this.homeY=homeY;this.vx=vx;this.kind=kind;this.nextShot=nextShot;
        }
    }
    private static final class PlayerShot {
        double x,y;
        PlayerShot(double x,double y){this.x=x;this.y=y;}
    }
    private static final class Blast {
        final double x,y; final int end; final boolean harmless;
        Blast(double x,double y,int end,boolean harmless){this.x=x;this.y=y;this.end=end;this.harmless=harmless;}
    }

    private final int width,height;
    private final boolean invincible;
    private final Random random;
    private final List<Shot> hostile = new ArrayList<>();
    private final List<PlayerShot> friendly = new ArrayList<>();
    private final List<Beam> beams = new ArrayList<>();
    private final List<Bomb> bombs = new ArrayList<>();
    private final List<Drone> drones = new ArrayList<>();
    private final List<Blast> blasts = new ArrayList<>();
    private int shield=SHIELD_HP,hull=HULL_HP,lives=3;
    private int frame, nextVolley=45, nextSpecial=120, nextPlayerShot;
    private int protectedUntil, frozenUntil, vulnerableUntil, reflectiveUntil, phaseFlashUntil;
    private int hits, bombsDisarmed, reflectionCount;
    private boolean automatic=true, megaStarted;
    private double playerX;
    private String message="SHIELD PHASE";
    private int messageUntil=180;

    public SuperweaponFight(int width,int height,boolean invincible,long seed) {
        if(width<320 || height<440) throw new IllegalArgumentException("Playfield too small");
        this.width=width;this.height=height;this.invincible=invincible;
        this.random=new Random(seed);playerX=(width-PLAYER_WIDTH)/2.0;
    }
    public int getShield(){return shield;}
    public int getHull(){return hull;}
    public int getFrame(){return frame;}
    public int getLives(){return !invincible && isVulnerable() && lives>0?1:lives;}
    public int getHits(){return hits;}
    public int getBombsDisarmed(){return bombsDisarmed;}
    public int getReflectionCount(){return reflectionCount;}
    public int getPlayerX(){return (int)Math.round(playerX);}
    public int getPlayerY(){return height-30;}
    public boolean isPhaseTwo(){return shield==0;}
    public boolean isWon(){return hull==0;}
    public boolean isLost(){return lives==0;}
    public boolean isInvincible(){return invincible;}
    public boolean isRecovering(){return frame<protectedUntil;}
    public boolean isFrozen(){return frame<frozenUntil;}
    public boolean isVulnerable(){return frame<vulnerableUntil;}
    public boolean isReflecting(){return frame<reflectiveUntil && !isWon();}
    public boolean isAutomatic(){return automatic;}
    public void setAutomatic(boolean value){automatic=value;}
    public List<Shot> getHostileShots(){return Collections.unmodifiableList(hostile);}
    public List<Beam> getBeams(){return Collections.unmodifiableList(beams);}
    public List<Bomb> getBombs(){return Collections.unmodifiableList(bombs);}
    public List<Drone> getDrones(){return Collections.unmodifiableList(drones);}
    public int getPortX(int port){return (int)Math.round(PORT_X[port]*width/(double)Superweapon.ART_WIDTH);}
    public int getPortY(int port){return Superweapon.TOP_Y+(int)Math.round((PORT_Y[port]+1)*width/(double)Superweapon.ART_WIDTH);}
    public int getBossBottom(){return Superweapon.TOP_Y+(int)Math.round(Superweapon.ART_HEIGHT*width/(double)Superweapon.ART_WIDTH);}
    private double playerCenter(){return playerX+PLAYER_WIDTH/2.0;}
    private void announce(String s,int frames){message=s;messageUntil=frame+frames;}
    private boolean ended(){return isWon()||isLost();}

    /** Debug shortcut: expose the hull without modifying its HP. */
    public void skipShield(){if(!ended() && shield>0){shield=0;beginPhaseTwo();}}
    private void beginPhaseTwo(){
        phaseFlashUntil=frame+90;
        announce("SHIELD BROKEN - WEAPON OVERDRIVE",150);
        nextSpecial=Math.max(nextSpecial,frame+90);
    }
    /** A single player hit, returning false if the reflective barrier blocks it. */
    public boolean hitBoss(double x,double y){
        if(ended())return false;
        if(isReflecting()){
            addAimed(x,y+8,playerCenter(),getPlayerY()+8,4,Kind.REFLECTED);
            reflectionCount++;
            return false;
        }
        if(shield>0){if(--shield==0)beginPhaseTwo();}
        else if(--hull==0){
            hostile.clear();beams.clear();bombs.clear();drones.clear();friendly.clear();
            reflectiveUntil=0;megaStarted=false;
            announce("SUPERWEAPON DESTROYED",Integer.MAX_VALUE-frame);
        }
        return true;
    }
    /** True throughout the central laser charge and firing, not earlier in a combo. */
    public boolean isMegaLocked(){
        for(Beam b:beams)if(b.mega && frame>=b.start && frame<b.end)return true;
        return false;
    }
    private boolean phaseTwoOnly(Attack a){return a==Attack.DRONES||a==Attack.HOMING||a==Attack.SWEEP_RIGHT||a==Attack.SWEEP_LEFT||a==Attack.CONVERGE||a==Attack.REFLECT_SHIELD;}

    /** Force one skill in the test arena. Phase two skills need the shield broken. */
    public boolean trigger(Attack attack){
        if(ended()||isMegaLocked()||(!isPhaseTwo()&&phaseTwoOnly(attack)))return false;
        // Do not interrupt an existing laser sequence (including a scheduled mega).
        if(!beams.isEmpty())return false;
        int end=frame;
        switch(attack){
            case SINGLE_LASER:
                end=schedule(random.nextInt(8),frame,false);announce("LASER CHARGING",90);break;
            case ALL_LASERS:
                for(int p:LASER_ORDER)end=Math.max(end,schedule(p,frame,false));
                announce("ALL PORTS CHARGING",90);break;
            case MEGA_LASER:
                end=schedule(-1,frame,true);announce("MAIN CANNON CHARGING",168);break;
            case BOMB:
                if(!bombs.isEmpty())return false;
                int p=1+random.nextInt(6);
                bombs.add(new Bomb(getPortX(p),getPortY(p)+12,getPlayerY()+PLAYER_HEIGHT/2.0));
                announce("BOMB INBOUND - SHOOT IT",120);break;
            case DRONES:
                if(!drones.isEmpty())return false;
                int count=2+random.nextInt(4);
                Kind[] types={Kind.NORMAL,Kind.FREEZE,Kind.FAST,Kind.SPLIT,Kind.VULNERABLE};
                int rotation=random.nextInt(types.length);
                for(int i=0;i<count;i++){
                    double x=(i+1)*width/(double)(count+1);
                    drones.add(new Drone(x,getBossBottom()+12,getBossBottom()+48+(i%2)*25,
                        (i%2==0?1:-1)*(.85+random.nextDouble()*.45),types[(i+rotation)%types.length],frame+60+i*15));
                }
                announce("ESCORTS DEPLOYED",120);break;
            case HOMING:
                hostile.add(new Shot(width/2.0,getBossBottom()+6,0,2.5,Kind.HOMING));
                announce("TRACKING PROJECTILE",100);break;
            case SWEEP_RIGHT:case SWEEP_LEFT:
                for(int i=0;i<8;i++)end=Math.max(end,schedule(attack==Attack.SWEEP_RIGHT?i:7-i,frame+i*22,false));
                announce(attack==Attack.SWEEP_RIGHT?"LASER SWEEP  >":"LASER SWEEP  <",end-frame);break;
            case CONVERGE:
                for(int i=0;i<4;i++){
                    end=Math.max(end,schedule(i,frame+i*30,false));
                    end=Math.max(end,schedule(7-i,frame+i*30,false));
                }
                end=schedule(-1,end+12,true);
                announce("CONVERGING LASERS - MAIN CANNON NEXT",end-frame);break;
            case REFLECT_SHIELD:
                if(isReflecting())return false;
                reflectiveUntil=frame+180;
                announce("REFLECTIVE BARRIER - HOLD FIRE",180);break;
            default:throw new IllegalStateException();
        }
        nextSpecial=Math.max(frame+(isPhaseTwo()?125:210),end+(isPhaseTwo()?55:100));
        return true;
    }
    private int schedule(int p,int start,boolean mega){Beam b=new Beam(p,start,mega);beams.add(b);return b.end;}
    private void updateMega(){
        boolean locked=isMegaLocked();
        if(locked&&!megaStarted){
            // Clear lingering hazards so the player has a fair, unobstructed dodge.
            hostile.clear();bombs.clear();reflectiveUntil=0;
            announce("MAIN CANNON CHARGING - MOVE TO THE SIDE",MEGA_LASER_CHARGE+MEGA_LASER_ACTIVE);
        }
        if(!locked&&megaStarted){nextVolley=frame+40;for(Drone d:drones)d.nextShot=frame+60;}
        megaStarted=locked;
    }
    private Attack chooseAttack(){
        int r=random.nextInt(100);
        if(!isPhaseTwo())return r<45?Attack.SINGLE_LASER:r<67?Attack.ALL_LASERS:r<91?Attack.BOMB:Attack.MEGA_LASER;
        if(r<19)return Attack.SINGLE_LASER;
        if(r<32)return Attack.ALL_LASERS;
        if(r<43)return Attack.BOMB;
        if(r<55)return Attack.DRONES;
        if(r<65)return Attack.HOMING;
        if(r<76)return Attack.SWEEP_RIGHT;
        if(r<85)return Attack.SWEEP_LEFT;
        if(r<91)return Attack.CONVERGE;
        if(r<96)return Attack.REFLECT_SHIELD;
        return Attack.MEGA_LASER;
    }
    /** Normal projectiles dominate phase one; special shots become more common later. */
    public Kind randomBullet(){
        int r=random.nextInt(100);
        if(!isPhaseTwo())return r<82?Kind.NORMAL:r<88?Kind.FREEZE:r<93?Kind.FAST:r<97?Kind.SPLIT:Kind.VULNERABLE;
        return r<32?Kind.NORMAL:r<44?Kind.FREEZE:r<61?Kind.FAST:r<81?Kind.SPLIT:r<88?Kind.VULNERABLE:Kind.ACCELERATED;
    }
    private void addAimed(double x,double y,double tx,double ty,double speed,Kind kind){
        double dx=tx-x,dy=Math.max(1,ty-y),len=Math.hypot(dx,dy);
        hostile.add(new Shot(x,y,dx/len*speed,dy/len*speed,kind));
    }
    private void emit(double x,double y,Kind kind){
        if(kind==Kind.SPLIT){hostile.add(new Shot(x,y,-1,4,kind));hostile.add(new Shot(x,y,1,4,kind));}
        else if(kind==Kind.FAST)addAimed(x,y,playerCenter(),getPlayerY()+8,8,kind);
        else hostile.add(new Shot(x,y,0,kind==Kind.ACCELERATED?6:4,kind));
    }
    /** Test one projectile type without waiting for RNG. */
    public boolean fireTestBullet(Kind kind){
        if(ended()||isMegaLocked()||kind==Kind.HOMING||kind==Kind.REFLECTED)return false;
        int p=3;emit(getPortX(p),getPortY(p)+3,kind);return true;
    }
    private void volley(){
        int first=random.nextInt(8),count=1+random.nextInt(isPhaseTwo()?3:2);
        for(int i=0;i<count;i++){
            int p=(first+i*3)%8;emit(getPortX(p),getPortY(p)+2,randomBullet());
        }
    }

    /** One simulation frame. Direction is -1, 0, or +1. */
    public void tick(int direction,boolean fire){
        if(ended())return;
        frame++;
        if(!isFrozen())playerX=Math.max(1,Math.min(width-PLAYER_WIDTH-1,playerX+Integer.signum(direction)*2));
        if(fire&&!isFrozen()&&!isRecovering()&&frame>=nextPlayerShot){
            friendly.add(new PlayerShot(playerCenter(),getPlayerY()-5));nextPlayerShot=frame+45;
        }
        beams.removeIf(b->frame>=b.end);
        updateMega();
        if(automatic&&!isMegaLocked()){
            if(frame>=nextSpecial&&beams.isEmpty()){
                if(!trigger(chooseAttack()))nextSpecial=frame+35;
                updateMega();
            }
            if(!isMegaLocked()&&frame>=nextVolley){volley();nextVolley=frame+(isPhaseTwo()?25+random.nextInt(13):40+random.nextInt(19));}
        }
        updateDrones();
        updatePlayerShots();
        if(ended())return;
        updateHostile();updateBombs();updateBeams();
        blasts.removeIf(b->frame>=b.end);
    }
    private void updateDrones(){
        for(Drone d:drones){
            if(d.y<d.homeY)d.y=Math.min(d.homeY,d.y+1.4);
            else{
                d.x+=d.vx;
                if(d.x<14||d.x>width-14){d.x=Math.max(14,Math.min(width-14,d.x));d.vx=-d.vx;}
                if(!isMegaLocked()&&frame>=d.nextShot){emit(d.x,d.y+9,d.kind);d.nextShot=frame+80+random.nextInt(45);}
            }
        }
    }
    private boolean overlapsPlayer(double x,double y,double w,double h){
        return x+w>=playerX&&x-w<=playerX+PLAYER_WIDTH&&y+h>=getPlayerY()&&y-h<=getPlayerY()+PLAYER_HEIGHT;
    }
    private void damagePlayer(){
        if(ended()||isRecovering())return;
        hits++;protectedUntil=frame+PLAYER_INVULNERABILITY;
        if(!invincible){if(isVulnerable())lives=0;else lives--;}
        if(isLost())announce("GAME OVER - R TO RETRY",Integer.MAX_VALUE-frame);
    }
    private void updateHostile(){
        Iterator<Shot> it=hostile.iterator();
        while(it.hasNext()){
            Shot s=it.next();s.age++;
            if(s.kind==Kind.HOMING){
                // Smoothly home while descending; bounded turn and a finite lifetime.
                double targetV=Math.max(-1.65,Math.min(1.65,(playerCenter()-s.x)*.07));
                s.vx+=Math.max(-.16,Math.min(.16,targetV-s.vx));
                s.vy=Math.min(4.2,s.vy+.014);
            }
            s.x+=s.vx;s.y+=s.vy;
            if(s.x< -20||s.x>width+20||s.y>height+20||s.age>420){it.remove();continue;}
            int radius=s.kind==Kind.HOMING?7:3;
            if(!overlapsPlayer(s.x,s.y,radius,s.kind==Kind.HOMING?7:5))continue;
            it.remove();
            if(isRecovering()||ended())continue;
            if(s.kind==Kind.FREEZE){if(!isFrozen())frozenUntil=frame+180;}
            else if(s.kind==Kind.VULNERABLE){if(!isVulnerable())vulnerableUntil=frame+300;}
            else damagePlayer();
        }
    }
    private void updateBombs(){
        Iterator<Bomb> it=bombs.iterator();
        while(it.hasNext()){
            Bomb b=it.next();b.y+=2.1;
            if(b.y>=b.ground){
                it.remove();blasts.add(new Blast(b.x,b.ground,frame+24,false));
                double nearestX=Math.max(playerX,Math.min(playerX+PLAYER_WIDTH,b.x));
                double nearestY=Math.max(getPlayerY(),Math.min(getPlayerY()+PLAYER_HEIGHT,b.ground));
                if(Math.hypot(nearestX-b.x,nearestY-b.ground)<=BOMB_RADIUS)damagePlayer();
            }
        }
    }
    private void updateBeams(){
        for(Beam b:beams){
            if(frame<b.fire||frame>=b.end)continue;
            double x=b.mega?width/2.0:getPortX(b.port);
            double half=b.mega?width*.17:(isPhaseTwo()?8:6);
            if(playerX+PLAYER_WIDTH>=x-half&&playerX<=x+half)damagePlayer();
        }
    }
    private void updatePlayerShots(){
        Iterator<PlayerShot> it=friendly.iterator();
        while(it.hasNext()){
            PlayerShot s=it.next();s.y-=6;
            boolean consumed=false;
            for(Iterator<Bomb> bi=bombs.iterator();bi.hasNext();){
                Bomb b=bi.next();if(Math.abs(s.x-b.x)<=12&&Math.abs(s.y-b.y)<=17){
                    bi.remove();bombsDisarmed++;blasts.add(new Blast(b.x,b.y,frame+15,true));consumed=true;break;
                }
            }
            if(!consumed)for(Iterator<Drone> di=drones.iterator();di.hasNext();){
                Drone d=di.next();if(Math.abs(s.x-d.x)<=15&&Math.abs(s.y-d.y)<=11){di.remove();consumed=true;break;}
            }
            if(!consumed)for(Iterator<Shot> si=hostile.iterator();si.hasNext();){
                Shot h=si.next();if(h.kind==Kind.VULNERABLE&&Math.abs(s.x-h.x)<=6&&Math.abs(s.y-h.y)<=9){si.remove();consumed=true;break;}
            }
            if(consumed){it.remove();continue;}
            if(Superweapon.containsPixel(width,Superweapon.TOP_Y,s.x,s.y)){
                it.remove();hitBoss(s.x,s.y);if(isWon())return;continue;
            }
            if(s.y<Superweapon.TOP_Y)it.remove();
        }
    }

    /** Draw boss and attacks using the existing text-grid sprite. HUD/player drawn separately. */
    public void draw(Graphics2D target){
        Graphics2D g=(Graphics2D)target.create();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
            if(!isWon()){
                Superweapon.draw(g,width,Superweapon.TOP_Y);
                if(isReflecting()){
                    g.setColor(new Color(122,224,255,64));g.fillRect(0,Superweapon.TOP_Y,width,getBossBottom()-Superweapon.TOP_Y+8);
                    g.setColor(new Color(157,235,255));g.fillRect(0,getBossBottom()+6,width,3);
                    g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));g.drawString("REFLECTING",width/2-38,getBossBottom()+20);
                }else if(frame<phaseFlashUntil&&(frame/6)%2==0){
                    g.setColor(new Color(255,55,35,55));g.fillRect(0,Superweapon.TOP_Y,width,getBossBottom()-Superweapon.TOP_Y);
                }
            }
            for(Beam b:beams)drawBeam(g,b);
            for(Bomb b:bombs)drawBomb(g,b);
            for(Drone d:drones)drawDrone(g,d);
            for(Shot s:hostile)drawShot(g,s);
            for(PlayerShot s:friendly){g.setColor(new Color(90,255,128));g.fillRect((int)s.x-2,(int)s.y-5,4,10);}
            for(Blast b:blasts){int r=b.harmless?13:BOMB_RADIUS;g.setColor(new Color(255,125,25,110));g.fillOval((int)b.x-r,(int)b.y-r,r*2,r*2);g.setColor(AMBER);g.drawOval((int)b.x-r,(int)b.y-r,r*2,r*2);}
        }finally{g.dispose();}
    }
    private void drawBeam(Graphics2D g,Beam b){
        if(frame<b.start||frame>=b.end)return;
        int x=b.mega?width/2:getPortX(b.port),y=b.mega?getBossBottom()-8:getPortY(b.port);
        int half=b.mega?(int)(width*.17):(isPhaseTwo()?8:6);
        if(frame<b.fire){
            double p=(frame-b.start)/(double)(b.fire-b.start);
            int glow=b.mega?12+(int)(p*20):5+(int)(p*5);
            g.setColor(new Color(255,255,255,45+(int)(p*70)));g.fillRect(x-half,y,half*2,height-y);
            g.setColor(Color.WHITE);g.fillOval(x-glow,y-glow,glow*2,glow*2);
            for(int yy=y+12;yy<height;yy+=14){g.fillRect(x-half,yy,2,7);g.fillRect(x+half-2,yy,2,7);}
        }else{
            g.setColor(new Color(255,28,40,100));g.fillRect(x-half-5,y,half*2+10,height-y);
            g.setColor(RED);g.fillRect(x-half,y,half*2,height-y);
            g.setColor(new Color(255,143,116));g.fillRect(x-half/2,y,half,height-y);
            g.setColor(new Color(255,245,225));g.fillRect(x-Math.max(1,half/5),y,Math.max(2,half*2/5),height-y);
        }
    }
    private static Color shotColor(Kind k){switch(k){case FREEZE:return ICE;case VULNERABLE:return VIOLET;case FAST:return AMBER;case SPLIT:return new Color(245,220,150);case ACCELERATED:return new Color(255,92,78);case REFLECTED:return new Color(135,235,255);case HOMING:return new Color(255,84,162);default:return Color.WHITE;}}
    private void drawShot(Graphics2D g,Shot s){
        int x=(int)s.x,y=(int)s.y;g.setColor(shotColor(s.kind));
        if(s.kind==Kind.HOMING){g.fillRect(x-4,y-7,8,14);g.fillRect(x-7,y-2,14,7);g.setColor(Color.WHITE);g.fillRect(x-2,y,4,4);}
        else g.fillRect(x-3,y-5,6,s.kind==Kind.FAST?14:10);
    }
    private void drawBomb(Graphics2D g,Bomb b){
        int x=(int)b.x,y=(int)b.y;
        // Retro falling bomb: cross-shaped fins, metallic case and red warning band.
        g.setColor(new Color(70,76,81));g.fillRect(x-9,y-15,18,5);g.fillRect(x-4,y-20,8,9);
        g.setColor(new Color(148,155,153));g.fillOval(x-9,y-13,18,27);
        g.setColor(new Color(65,70,78));g.fillRect(x-7,y-4,14,7);
        g.setColor((frame/8)%2==0?RED:AMBER);g.fillRect(x-7,y-2,14,3);
        g.setColor(new Color(255,115,45,100));g.drawOval(x-BOMB_RADIUS,(int)b.ground-BOMB_RADIUS,BOMB_RADIUS*2,BOMB_RADIUS*2);
    }
    private static final String[][] DRONE_ART={
        {"...##...","..####..","########","##.##.##",".######.","..#..#.."},
        {".#....#.","###..###",".######.","##.##.##",".######.","...##..."},
        {"...##...","...##...","..####..",".######.","########","#..##..#"},
        {"##....##",".##..##.","########",".######.","###..###","#......#"},
        {"..####..",".##..##.","########","##.##.##",".######.",".#....#."}
    };
    private void drawDrone(Graphics2D g,Drone d){
        int i=d.kind==Kind.FREEZE?1:d.kind==Kind.FAST?2:d.kind==Kind.SPLIT?3:d.kind==Kind.VULNERABLE?4:0;
        String[] art=DRONE_ART[i];g.setColor(shotColor(d.kind));
        for(int y=0;y<art.length;y++)for(int x=0;x<8;x++)if(art[y].charAt(x)=='#')g.fillRect((int)d.x-12+x*3,(int)d.y-9+y*3,3,3);
        g.setColor(RED);g.fillRect((int)d.x-2,(int)d.y-2,4,4);
    }
    public void drawHud(Graphics2D g){
        g.setColor(new Color(13,12,21));g.fillRect(0,0,width,Superweapon.TOP_Y);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));g.setColor(Color.WHITE);
        g.drawString("SUPERWEAPON  /  PHASE "+(isPhaseTwo()?2:1),10,16);
        g.drawString(invincible?"TEST: INVINCIBLE":"LIVES: "+getLives(),width-140,16);
        int x=10,y=24,w=width-20,h=15;
        g.setColor(new Color(65,18,24));g.fillRect(x,y,w,h);
        g.setColor(RED);g.fillRect(x,y,(int)Math.round(w*hull/(double)HULL_HP),h);
        if(shield>0){g.setColor(new Color(240,245,250));g.fillRect(x,y,(int)Math.round(w*shield/(double)SHIELD_HP),h);}
        g.setColor(new Color(150,140,155));g.drawRect(x,y,w,h);
        g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));g.setColor(Color.WHITE);
        g.drawString("SHIELD "+shield+"/"+SHIELD_HP+"   HULL "+hull+"/"+HULL_HP+"   HITS "+hits,10,53);
        g.setColor(isFrozen()?ICE:isVulnerable()?VIOLET:AMBER);
        String line=isWon()?"SUPERWEAPON DESTROYED - R: RESTART":isLost()?"GAME OVER - R: RESTART":isFrozen()?"FROZEN":isVulnerable()?"VULNERABLE":frame<messageUntil?message:"A/D MOVE | SPACE FIRE | T AUTO | ESC EXIT";
        g.drawString(line,10,72);
    }
}
