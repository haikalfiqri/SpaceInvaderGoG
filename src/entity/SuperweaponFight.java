package entity;

import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
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
    public static final int REFLECT_DURATION = 180;
    public static final int WAVE_WARNING = 60;
    public static final int SLOW_DURATION = 150;
    public static final double SLOW_FACTOR = .5;
    public static final int WAVE_AUTO_COOLDOWN = 1080;
    private static final int WAVE_GAP = 88;
    private static final double WAVE_SPEED = 3.2;
    private static final double WAVE_START_RADIUS = 12.0;
    private static final double WAVE_BAND = 8.0;
    private static final double WAVE_OPEN_HALF_ANGLE = Math.toRadians(11.5);
    public static final int PLAYER_WIDTH = 26, PLAYER_HEIGHT = 16;
    private static final int[] PORT_X = {7,36,63,87,136,160,187,217};
    private static final int[] PORT_Y = {49,62,73,82,82,73,62,49};
    private static final int[] LASER_ORDER = {0,1,2,3,4,5,6,7};
    private static final Color RED = new Color(0xff2838);
    private static final Color ICE = new Color(74,185,232);
    private static final Color VIOLET = new Color(185,138,255);
    private static final Color AMBER = new Color(255,172,65);

    public enum Attack {
        SINGLE_LASER, ALL_LASERS, MEGA_LASER, BOMB, DRONES, HOMING,
        SWEEP_RIGHT, SWEEP_LEFT, CONVERGE, REFLECT_SHIELD, SLOW_WAVE
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
    /** One expanding electromagnetic shockwave, leaving a safe opening sector. */
    public static final class SlowWave {
        public final int launch, gapCenter;
        public final double sourceX, sourceY, openingAngle;
        public double y;
        private boolean resolved;
        SlowWave(int launch,int gapCenter,double radius,double sourceX,double sourceY,double openingAngle){
            this.launch=launch;this.gapCenter=gapCenter;this.y=radius;
            this.sourceX=sourceX;this.sourceY=sourceY;this.openingAngle=openingAngle;
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
    private int slowedUntil, nextReflect=300, nextWave;
    private SlowWave wave;
    private int hits, bombsDisarmed, reflectionCount;
    private int hitFlashUntil;
    // Visual-only timestamps. Never affect attack scheduling, RNG or hit detection.
    private final int[] portFiredAt = {-100,-100,-100,-100,-100,-100,-100,-100};
    private int mainFiredAt=-100, bayOpenedAt=-100;
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
    public boolean isSlowed(){return frame<slowedUntil;}
    public int getSlowFramesRemaining(){return Math.max(0,slowedUntil-frame);}
    public SlowWave getSlowWave(){return wave;}
    public int getWaveGapWidth(){return WAVE_GAP;}
    public boolean isVulnerable(){return frame<vulnerableUntil;}
    public boolean isReflecting(){return frame<reflectiveUntil && !isWon() && !isMegaLocked();}
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
        hitFlashUntil=frame+5;
        if(shield>0){if(--shield==0)beginPhaseTwo();}
        else if(--hull==0){
            hostile.clear();beams.clear();bombs.clear();drones.clear();friendly.clear();
            reflectiveUntil=0;megaStarted=false;wave=null;slowedUntil=0;
            announce("SUPERWEAPON DESTROYED",Integer.MAX_VALUE-frame);
        }
        return true;
    }
    /** True throughout the central laser charge and firing, not earlier in a combo. */
    public boolean isMegaLocked(){
        for(Beam b:beams)if(b.mega && frame>=b.start && frame<b.end)return true;
        return false;
    }
    private boolean phaseTwoOnly(Attack a){return a==Attack.DRONES||a==Attack.HOMING||a==Attack.SWEEP_RIGHT||a==Attack.SWEEP_LEFT||a==Attack.CONVERGE;}

    /** Force one skill in the test arena. Phase two skills need the shield broken. */
    public boolean trigger(Attack attack){
        if(ended()||isMegaLocked()||(!isPhaseTwo()&&phaseTwoOnly(attack)))return false;
        // Do not interrupt an existing laser sequence (including a scheduled mega).
        if(!beams.isEmpty() || (wave!=null && frame<wave.launch))return false;
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
                portFiredAt[p]=frame;
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
                bayOpenedAt=frame;
                announce("ESCORTS DEPLOYED",120);break;
            case HOMING:
                hostile.add(new Shot(width/2.0,getBossBottom()+6,0,2.5,Kind.HOMING));
                mainFiredAt=frame;
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
                reflectiveUntil=frame+REFLECT_DURATION;
                // Independent recurring barrier, available in either health phase.
                // The interval is measured from activation, not expiration.
                nextReflect=frame+(isPhaseTwo()?420+random.nextInt(181):540+random.nextInt(181));
                announce("REFLECTIVE BARRIER - HOLD FIRE",REFLECT_DURATION);return true;
            case SLOW_WAVE:
                if(wave!=null)return false;
                int gap=WAVE_GAP/2+12+random.nextInt(width-WAVE_GAP-23);
                double sourceX=width/2.0,sourceY=getBossBottom()-10;
                double openingAngle=Math.atan2(getPlayerY()+PLAYER_HEIGHT/2.0-sourceY,gap-sourceX);
                wave=new SlowWave(frame+WAVE_WARNING,gap,WAVE_START_RADIUS,sourceX,sourceY,openingAngle);
                nextWave=frame+WAVE_AUTO_COOLDOWN;
                announce("EMP SHOCKWAVE - STAY IN THE OPENING",WAVE_WARNING+120);break;
            default:throw new IllegalStateException();
        }
        nextSpecial=Math.max(frame+(isPhaseTwo()?125:210),end+(isPhaseTwo()?55:100));
        return true;
    }
    private int schedule(int p,int start,boolean mega){Beam b=new Beam(p,start,mega);beams.add(b);return b.end;}
    private void updateMega(){
        boolean locked=isMegaLocked();
        if(locked&&!megaStarted){
            // Existing bullets and bombs keep travelling. Only new attacks are locked.
            reflectiveUntil=0;
            announce("MAIN CANNON CHARGING - MOVE TO THE SIDE",MEGA_LASER_CHARGE+MEGA_LASER_ACTIVE);
        }
        if(!locked&&megaStarted){nextVolley=frame+40;for(Drone d:drones)d.nextShot=frame+60;}
        megaStarted=locked;
    }
    private Attack chooseAttack(){
        int r=random.nextInt(100);
        if(!isPhaseTwo()){
            if(r<43)return Attack.SINGLE_LASER;
            if(r<64)return Attack.ALL_LASERS;
            if(r<87)return Attack.BOMB;
            if(r<92 && frame>=nextWave && wave==null)return Attack.SLOW_WAVE;
            return r<92?Attack.SINGLE_LASER:Attack.MEGA_LASER;
        }
        if(r<19)return Attack.SINGLE_LASER;
        if(r<32)return Attack.ALL_LASERS;
        if(r<43)return Attack.BOMB;
        if(r<55)return Attack.DRONES;
        if(r<65)return Attack.HOMING;
        if(r<76)return Attack.SWEEP_RIGHT;
        if(r<85)return Attack.SWEEP_LEFT;
        if(r<91)return Attack.CONVERGE;
        if(r<96)return frame>=nextWave&&wave==null?Attack.SLOW_WAVE:Attack.SINGLE_LASER;
        return Attack.MEGA_LASER;
    }
    /** Normal projectiles dominate phase one; special shots become more common later. */
    public Kind randomBullet(){
        int r=random.nextInt(100);
        if(!isPhaseTwo()){
            if(shield>SHIELD_HP/2)return r<95?Kind.NORMAL:r<97?Kind.FREEZE:r<98?Kind.FAST:r<99?Kind.SPLIT:Kind.VULNERABLE;
            return r<90?Kind.NORMAL:r<94?Kind.FREEZE:r<97?Kind.FAST:r<99?Kind.SPLIT:Kind.VULNERABLE;
        }
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
        int p=3;emit(getPortX(p),getPortY(p)+3,kind);portFiredAt[p]=frame;return true;
    }
    private void volley(){
        int first=random.nextInt(8),count=isPhaseTwo()?1+random.nextInt(3):2+random.nextInt(2);
        for(int i=0;i<count;i++){
            int p=(first+i*3)%8;emit(getPortX(p),getPortY(p)+2,randomBullet());portFiredAt[p]=frame;
        }
    }

    /** One simulation frame. Direction is -1, 0, or +1. */
    public void tick(int direction,boolean fire){
        if(ended())return;
        frame++;
        if(!isFrozen())playerX=Math.max(1,Math.min(width-PLAYER_WIDTH-1,
                playerX+Integer.signum(direction)*2*(isSlowed()?SLOW_FACTOR:1.0)));
        if(fire&&!isFrozen()&&!isRecovering()&&frame>=nextPlayerShot){
            friendly.add(new PlayerShot(playerCenter(),getPlayerY()-5));nextPlayerShot=frame+45;
        }
        beams.removeIf(b->frame>=b.end);
        updateMega();
        if(automatic&&!isMegaLocked()){
            if(beams.isEmpty() && (wave==null || frame>=wave.launch)){
                if(frame>=nextReflect && !isReflecting())trigger(Attack.REFLECT_SHIELD);
                else if(frame>=nextSpecial){
                    if(!trigger(chooseAttack()))nextSpecial=frame+35;
                    updateMega();
                }
            }
            if(!isMegaLocked()&&frame>=nextVolley){
                volley();
                nextVolley=frame+(isPhaseTwo()?25+random.nextInt(13):
                        shield>SHIELD_HP/2?28+random.nextInt(11):35+random.nextInt(12));
            }
        }
        updateDrones();
        updatePlayerShots();
        if(ended())return;
        updateHostile();updateBombs();updateBeams();updateWave();
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
    /** A wave never removes a life, consumes a shield, or changes bullet cooldowns. */
    private void updateWave(){
        if(wave==null || frame<wave.launch)return;
        wave.y+=WAVE_SPEED;
        if(!wave.resolved){
            double px=playerCenter(), py=getPlayerY()+PLAYER_HEIGHT/2.0;
            double dx=px-wave.sourceX, dy=py-wave.sourceY;
            double distance=Math.hypot(dx,dy);
            if(wave.y+WAVE_BAND>=distance){
                wave.resolved=true;
                double playerAngle=Math.atan2(dy,dx);
                if(!ended() && angleDiff(playerAngle,wave.openingAngle)>WAVE_OPEN_HALF_ANGLE){
                    slowedUntil=Math.max(slowedUntil,frame+SLOW_DURATION);
                    announce("EMP HIT - MOVEMENT SLOWED",SLOW_DURATION);
                }
            }
        }
        double maxDistance=Math.hypot(Math.max(wave.sourceX,width-wave.sourceX)+16,height-wave.sourceY+16);
        if(wave.y-WAVE_BAND>maxDistance)wave=null;
    }
    private int beamTop(Beam b){return b.mega?getBossBottom()-8:getPortY(b.port);}
    private int beamBottom(Beam b){
        int top=beamTop(b);
        double grow=Math.max(0,Math.min(1,(frame-b.fire+1)/(double)(b.mega?10:5)));
        return top+(int)Math.round((height-top)*grow);
    }
    private void updateBeams(){
        for(Beam b:beams){
            if(frame<b.fire||frame>=b.end)continue;
            double x=b.mega?width/2.0:getPortX(b.port);
            double half=b.mega?width*.17:(isPhaseTwo()?8:6);
            if(beamBottom(b)>=getPlayerY() && playerX+PLAYER_WIDTH>=x-half&&playerX<=x+half)damagePlayer();
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
                drawAnimatedBoss(g);
                if(isReflecting()){
                    Superweapon.drawOverlay(g,width,Superweapon.TOP_Y,true,(frame/6)%2==0?0.95f:0.5f);
                }else if(frame<hitFlashUntil || (frame<phaseFlashUntil&&(frame/6)%2==0)){
                    Superweapon.drawOverlay(g,width,Superweapon.TOP_Y,false,0.35f);
                }
            }
            if(wave!=null)drawWave(g,wave);
            for(Beam b:beams)drawBeam(g,b);
            for(Bomb b:bombs)drawBomb(g,b);
            for(Drone d:drones)drawDrone(g,d);
            for(Shot s:hostile)drawShot(g,s);
            for(PlayerShot s:friendly)drawMask(g,PLAYER_BULLET,(int)Math.round(s.x)-3,(int)Math.round(s.y)-5,Color.WHITE,2);
            for(Blast b:blasts)drawBlast(g,b);
        }finally{g.dispose();}
    }
    /** Smooth visual state, derived from the same frame clock as the attacks. */
    private static float recoil(int age,int duration){
        return age<0||age>=duration?0f:1f-age/(float)duration;
    }
    private void drawAnimatedBoss(Graphics2D g){
        float[] charge=new float[8], kick=new float[8];
        for(int i=0;i<8;i++)kick[i]=recoil(frame-portFiredAt[i],12);
        float mainCharge=0f,mainKick=recoil(frame-mainFiredAt,20);
        for(Beam b:beams){
            if(frame<b.start||frame>=b.end)continue;
            float energy=frame<b.fire?.20f+.80f*(frame-b.start)/(float)(b.fire-b.start):1f;
            float r=frame<b.fire?0f:recoil(frame-b.fire,b.mega?25:14);
            if(b.mega){mainCharge=Math.max(mainCharge,energy);mainKick=Math.max(mainKick,r);}
            else {charge[b.port]=Math.max(charge[b.port],energy);kick[b.port]=Math.max(kick[b.port],r);}
        }
        Superweapon.drawAnimated(g,width,Superweapon.TOP_Y,frame,isPhaseTwo(),PORT_X,PORT_Y,
                charge,kick,mainCharge,mainKick,recoil(frame-bayOpenedAt,36));
    }
    /** Circular pixel-ring shockwave with a safe opening sector. */
    private void drawWave(Graphics2D g,SlowWave w){
        if(frame<w.launch){
            drawWaveWarning(g,w);
            return;
        }
        drawWaveRing(g,w,w.y,alpha(RED,64),6,2.6);
        drawWaveRing(g,w,Math.max(0,w.y-6),alpha(RED,150),4,2.15);
        drawWaveRing(g,w,Math.max(0,w.y-12),new Color(255,205,205),2,1.7);
    }
    private void drawWaveWarning(Graphics2D g,SlowWave w){
        int pulse=(frame/6)%2==0?180:95;
        drawWaveRing(g,w,10+((frame/5)%3)*2,alpha(RED,pulse),4,2.1);
        drawWaveRing(g,w,20+((frame/7)%4)*2,alpha(RED,pulse/2),2,1.8);
        double left=w.openingAngle-WAVE_OPEN_HALF_ANGLE,right=w.openingAngle+WAVE_OPEN_HALF_ANGLE;
        drawGuideRay(g,w,left,pulse);
        drawGuideRay(g,w,right,pulse);
        int safeLeft=w.gapCenter-WAVE_GAP/2,safeRight=w.gapCenter+WAVE_GAP/2;
        g.setColor(alpha(Color.WHITE,pulse));
        for(int i=0;i<4;i++){
            int y=getPlayerY()+PLAYER_HEIGHT+6+i*6;
            g.fillRect(safeLeft-8,y,6,2);g.fillRect(safeRight+2,y,6,2);
        }
    }
    private void drawGuideRay(Graphics2D g,SlowWave w,double angle,int alpha){
        for(double r=22;r<height;r+=10){
            int x=(int)Math.round(w.sourceX+Math.cos(angle)*r);
            int y=(int)Math.round(w.sourceY+Math.sin(angle)*r);
            g.setColor(alpha(RED,Math.max(40,alpha-(int)(r*0.35))));
            g.fillRect(x,y,2,4);
        }
    }
    private void drawWaveRing(Graphics2D g,SlowWave w,double radius,Color color,int samples,double thickness){
        if(radius<=0)return;
        double start=w.openingAngle+WAVE_OPEN_HALF_ANGLE;
        double end=w.openingAngle-WAVE_OPEN_HALF_ANGLE+Math.PI*2;
        g.setColor(color);
        for(double ang=start;ang<end;ang+=Math.PI/180.0*samples){
            for(double off=-thickness;off<=thickness;off+=2.0){
                double r=radius+off;
                int x=(int)Math.round(w.sourceX+Math.cos(ang)*r);
                int y=(int)Math.round(w.sourceY+Math.sin(ang)*r);
                g.fillRect(x,y,2,2);
            }
        }
        for(int i=0;i<10;i++){
            double ang=start+(end-start)*(i+.5)/10.0 + Math.sin((frame+i*7)*0.13)*0.05;
            double r=radius+6+(i%2==0?2:-2);
            int x=(int)Math.round(w.sourceX+Math.cos(ang)*r);
            int y=(int)Math.round(w.sourceY+Math.sin(ang)*r);
            g.fillRect(x,y,2,4);
        }
    }
    private static double angleDiff(double a,double b){
        return Math.abs(Math.atan2(Math.sin(a-b),Math.cos(a-b)));
    }
    private static Color alpha(Color c,int a){return new Color(c.getRed(),c.getGreen(),c.getBlue(),a);}
    /** Same layered red glow, white core, charging orb and moving sparks as AI Commander. */
    private void drawBeam(Graphics2D g,Beam b){
        if(frame<b.start||frame>=b.end)return;
        int x=b.mega?width/2:getPortX(b.port),y=beamTop(b);
        int half=b.mega?(int)(width*.17):(isPhaseTwo()?8:6);
        if(frame<b.fire){
            double p=(frame-b.start)/(double)(b.fire-b.start);
            // White warning remains visible for the entire charge; red edge marks danger width.
            g.setColor(alpha(Color.WHITE,20+(int)(p*30)));g.fillRect(x-half,y,half*2,height-y);
            g.setColor(alpha(Color.WHITE,(frame/8)%2==0?215:100));
            for(int yy=y+10;yy<height;yy+=10){
                g.fillRect(x-1,yy,2,6);
                if(b.mega){g.fillRect(x-half,yy,2,6);g.fillRect(x+half-2,yy,2,6);}
            }
            drawOrb(g,x,y,(b.mega?28:12)*(.35+.65*p)*(1+.12*Math.sin(frame*.45)));
            return;
        }
        int h=Math.max(1,beamBottom(b)-y);
        float pulse=(frame/4)%2==0?1f:.85f;
        Graphics2D beam=(Graphics2D)g.create();
        try{
            beam.setComposite(AlphaComposite.SrcOver.derive(pulse));
            beam.setColor(alpha(RED,45));beam.fillRect(x-half-10,y,half*2+20,h);
            beam.setPaint(new LinearGradientPaint(x-half,0,x+half,0,
                    new float[]{0f,.2f,.4f,.6f,.8f,1f},new Color[]{alpha(RED,0),alpha(RED,135),RED,RED,alpha(RED,135),alpha(RED,0)}));
            beam.fillRect(x-half,y,half*2,h);
            beam.setColor(alpha(Color.WHITE,100));int core=Math.max(3,half/4);
            beam.fillRect(x-core-2,y,core*2+4,h);
            beam.setColor(Color.WHITE);beam.fillRect(x-core,y,core*2,h);
            beam.setColor(RED);
            for(int i=0;i<6;i++){
                int sy=y+(i*61+(frame-b.fire)*5)%h;
                beam.fillRect(i%2==0?x-half-5:x+half+3,sy,2,Math.min(10,y+h-sy));
            }
            drawOrb(beam,x,y,b.mega?32:14);
            if(beamBottom(b)>=height-1)drawImpact(beam,x,height-2,b.mega?half:20);
        }finally{beam.dispose();}
    }
    private static void drawOrb(Graphics2D g,int x,int y,double r){
        float radius=(float)Math.max(1,r);
        g.setPaint(new RadialGradientPaint(x,y,radius*1.6f,new float[]{0f,.18f,.42f,.7f,1f},
                new Color[]{Color.WHITE,Color.WHITE,RED,alpha(RED,45),alpha(RED,0)}));
        g.fill(new Ellipse2D.Double(x-radius*1.6,y-radius*1.6,radius*3.2,radius*3.2));
        g.setColor(Color.WHITE);int bar=(int)(radius*.85);
        g.fillRect(x-bar,y-1,bar*2,2);g.fillRect(x-1,y-bar,2,bar*2);
    }
    private static void drawImpact(Graphics2D g,int x,int y,int radius){
        g.setPaint(new RadialGradientPaint(x,y,Math.max(1,radius),new float[]{0f,.35f,1f},
                new Color[]{Color.WHITE,RED,alpha(RED,0)}));
        g.fillOval(x-radius,y-radius,radius*2,radius*2);
        g.setColor(Color.WHITE);
        for(int i=-2;i<=2;i++)g.drawLine(x,y,x+i*radius/3,y-radius+Math.abs(i)*radius/5);
    }
    // Exact original res/graphics bullet masks (3 x 5); white for normal,
    // split, sniper and accelerated shots, like the base game's Bullet class.
    private static final String[] PLAYER_BULLET={"###",".#.",".#.",".#.",".#."};
    private static final String[] ENEMY_BULLET={".#.",".#.",".#.",".#.","###"};
    private static final String[] TRACKING={"..#..",".###.","#####","##.##",".###.","..#.."};
    private static final String[] BOMB_ART={"#...#...#",".#######.","...###...","..#####..",".#######.",".#######.",".#######.",".#######.","..#####..","..#####..","...###...","....#...."};
    private static final String[] EXPLOSION={".#..#...#..#.","..#..#.#..#..","...#.....#...","##.........##","...#.....#...","..#..#.#..#..",".#..#...#..#."};
    private static void drawMask(Graphics2D g,String[] art,int x,int y,Color color,int scale){
        g.setColor(color);
        for(int row=0;row<art.length;row++)for(int col=0;col<art[row].length();col++)
            if(art[row].charAt(col)=='#')g.fillRect(x+col*scale,y+row*scale,scale,scale);
    }
    private static Color shotColor(Kind k){
        switch(k){case FREEZE:return ICE;case VULNERABLE:return VIOLET;case HOMING:return RED;default:return Color.WHITE;}
    }
    private void drawShot(Graphics2D g,Shot s){
        int x=(int)Math.round(s.x),y=(int)Math.round(s.y);
        if(s.kind==Kind.HOMING){
            drawMask(g,TRACKING,x-5,y-6,RED,2);
            g.setColor(Color.WHITE);g.fillRect(x-1,y-2,2,4);
        }else drawMask(g,ENEMY_BULLET,x-3,y-5,shotColor(s.kind),2);
    }
    private void drawBomb(Graphics2D g,Bomb b){
        int x=(int)b.x,y=(int)b.y;
        drawMask(g,BOMB_ART,x-9,y-12,new Color(140,132,148),2);
        g.setColor(new Color(50,40,52));g.fillRect(x-5,y-6,4,12);
        g.setColor((frame/8)%2==0?RED:Color.WHITE);g.fillRect(x-7,y+2,14,2);
        g.setColor(alpha(RED,110));g.drawOval(x-BOMB_RADIUS,(int)b.ground-BOMB_RADIUS,BOMB_RADIUS*2,BOMB_RADIUS*2);
    }
    private void drawBlast(Graphics2D g,Blast b){
        float p=Math.max(0,Math.min(1,(b.end-frame)/(float)(b.harmless?15:24)));
        int scale=b.harmless?2:3+(int)((1-p)*2);
        int x=(int)b.x,y=(int)b.y;
        if(!b.harmless){g.setColor(alpha(RED,(int)(p*90)));g.drawOval(x-BOMB_RADIUS,y-BOMB_RADIUS,BOMB_RADIUS*2,BOMB_RADIUS*2);}
        drawMask(g,EXPLOSION,x-13*scale/2,y-7*scale/2,alpha((frame/3)%2==0?Color.WHITE:RED,(int)(p*255)),scale);
    }
    // Existing Act 2/3/4 enemy frames, recoloured as the Superweapon's escorts.
    private static final String[][][] DRONE_ART={
            {{"#..#.##.#..#",".##########.","##..####..##","##.#....#.##","##.#.##.#.##",".###....###.","..#.####.#..",".#........#."},
                    {".#..#..#..#.",".##########.","##..####..##","##.#....#.##","##.#.##.#.##",".###....###.","..#.####.#..","..#......#.."}},
            {{".#...##...#.","..#.####.#..","..########..",".###.##.###.",".##########.","..########..","..#.####.#..",".#...##...#."},
                    {"#....##....#",".#.######.#.","..########..",".##.####.##.",".##########.","..########..",".#.######.#.","#....##....#"}},
            {{".....##.....",".....##.....","..########..",".##..##..##.","############",".#.######.#.",".#..#..#..#.","#....##....#"},
                    {".....##.....",".....##.....","..########..",".##..##..##.","############",".#.######.#.","#..#....#..#",".....##....."}},
            {{"....####....","..########..",".##.####.##.","############","#.#.#..#.#.#","#.#.#..#.#.#","..#.#..#.#..",".#..#..#..#."},
                    {"....####....","..########..",".##.####.##.","############",".#.#.##.#.#.",".#.#.##.#.#.",".#..#..#..#.","..#..##..#.."}},
            {{"##........##",".##......##.","..###..###..","..########..","...######...","....####....","....#..#....",".....##....."},
                    {".#........#.","..##....##..",".####..####.","..########..","...######...","....####....",".....##.....",".....##....."}}
    };
    private static final Color[] DRONE_COLORS={new Color(0xEEF3F7),ICE,new Color(0xE8C867),new Color(0xEC6A45),VIOLET};
    private void drawDrone(Graphics2D g,Drone d){
        int i=d.kind==Kind.FREEZE?1:d.kind==Kind.FAST?2:d.kind==Kind.SPLIT?3:d.kind==Kind.VULNERABLE?4:0;
        drawMask(g,DRONE_ART[i][(frame/30)%2],(int)d.x-12,(int)d.y-8,DRONE_COLORS[i],2);
    }
    public void drawHud(Graphics2D g){drawHud(g,new Font(Font.MONOSPACED,Font.PLAIN,12));}
    public void drawHud(Graphics2D g,Font font){
        g.setColor(Color.BLACK);g.fillRect(0,0,width,Superweapon.TOP_Y);
        g.setFont(font);g.setColor(Color.WHITE);
        g.drawString("SUPERWEAPON / PHASE "+(isPhaseTwo()?2:1),10,16);
        String life=invincible?"TEST: INVINCIBLE":"LIVES: "+getLives();
        g.drawString(life,width-10-g.getFontMetrics().stringWidth(life),16);
        g.setColor(Color.GREEN);g.fillRect(0,40,width,2);
        g.setColor(isFrozen()?ICE:isSlowed()?AMBER:isVulnerable()?VIOLET:Color.WHITE);
        String line=isWon()?"SUPERWEAPON DESTROYED - R: RESTART":isLost()?"GAME OVER - R: RESTART":isFrozen()?"FROZEN":isSlowed()?"EMP SLOWED - "+(getSlowFramesRemaining()+59)/60+"s":isVulnerable()?"VULNERABLE":frame<messageUntil?message:"A/D MOVE | SPACE FIRE | ESC EXIT";
        g.setFont(font.deriveFont(10f));g.drawString(line,10,31);
        g.setFont(font);g.setColor(Color.WHITE);
        String label="BOSS";g.drawString(label,(width-g.getFontMetrics().stringWidth(label))/2,64);
        int w=width/2,x=(width-w)/2,y=68,h=8;
        g.setColor(Color.RED);g.drawRect(x,y,w,h);
        g.fillRect(x,y,w*hull/HULL_HP,h);
        if(shield>0){g.setColor(Color.WHITE);g.fillRect(x,y,w*shield/SHIELD_HP,h);}
        g.setFont(font.deriveFont(10f));g.setColor(Color.WHITE);
        g.drawString("S "+shield+"/"+SHIELD_HP,8,76);
        String right="HP "+hull+"/"+HULL_HP;
        g.drawString(right,width-8-g.getFontMetrics().stringWidth(right),76);
    }
}
