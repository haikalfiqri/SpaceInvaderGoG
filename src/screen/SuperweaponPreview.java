package screen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import engine.Core;
import engine.Frame;
import engine.StoryBackgrounds;
import entity.Ship;
import entity.SuperweaponFight;
import entity.SuperweaponFight.Attack;
import entity.SuperweaponFight.Kind;

/** GOG-105: playable sandbox. Run main(), not Core. Does not touch profiles. */
public final class SuperweaponPreview extends Screen {
    private static final int WIDTH=448, HEIGHT=520, FPS=60;
    private static final Kind[] TEST_BULLETS={Kind.NORMAL,Kind.FREEZE,Kind.FAST,
            Kind.SPLIT,Kind.VULNERABLE,Kind.ACCELERATED};
    private final boolean invincible=!"false".equalsIgnoreCase(
            System.getProperty("superweapon.invincible","true"));
    private final Set<Integer> held=new HashSet<>();
    private Ship ship;
    private SuperweaponFight fight;
    private BufferedImage background;
    private int bulletIndex;

    public SuperweaponPreview(int width,int height,int fps){super(width,height,fps);}
    public static void main(String[] args){
        Frame frame=new Frame(WIDTH,HEIGHT,Core.SCALE);
        frame.setTitle("GoG - Superweapon combat sandbox");
        try{
            Core.getDrawManager().setFrame(frame);
            frame.setScreen(new SuperweaponPreview(frame.getWidth(),frame.getHeight(),FPS));
        }finally{frame.dispose();}
    }
    @Override public void initialize(){
        background=StoryBackgrounds.forLevel(40);
        reset();
        System.out.println("Superweapon preview: A/D move, Space shoot, Esc exit, R restart");
        System.out.println("F2 skip shield | T toggle auto | Q cycle projectile | 1-9/0 attacks");
        System.out.println("1 single, 2 all, 3 mega, 4 bomb, 5 drones, 6 homing,");
        System.out.println("7 left->right, 8 right->left, 9 convergence, 0 reflection, E EMP wave");
    }
    private void reset(){
        fight=new SuperweaponFight(width,height,invincible,System.nanoTime());
        ship=new Ship(fight.getPlayerX(),fight.getPlayerY());bulletIndex=0;
    }
    private boolean pressed(int key){
        if(inputManager.isKeyDown(key))return held.add(key);
        held.remove(key);return false;
    }
    @Override protected void update(){
        if(inputManager.isKeyDown(KeyEvent.VK_ESCAPE)){isRunning=false;return;}
        if(pressed(KeyEvent.VK_R))reset();
        if(pressed(KeyEvent.VK_F2))fight.skipShield();
        if(pressed(KeyEvent.VK_T)){
            fight.setAutomatic(!fight.isAutomatic());
            System.out.println("Automatic attacks: "+fight.isAutomatic());
        }
        if(pressed(KeyEvent.VK_Q)){
            Kind k=TEST_BULLETS[bulletIndex%TEST_BULLETS.length];
            if(fight.fireTestBullet(k)){bulletIndex++;System.out.println("Bullet: "+k);}
        }
        int[] keys={KeyEvent.VK_1,KeyEvent.VK_2,KeyEvent.VK_3,KeyEvent.VK_4,
                KeyEvent.VK_5,KeyEvent.VK_6,KeyEvent.VK_7,KeyEvent.VK_8,KeyEvent.VK_9,KeyEvent.VK_0};
        Attack[] attacks=Attack.values();
        for(int i=0;i<keys.length;i++)if(pressed(keys[i])){
            boolean ok=fight.trigger(attacks[i]);
            System.out.println(attacks[i]+(ok?" triggered":" unavailable (phase/cooldown/active attack)"));
        }
        if(pressed(KeyEvent.VK_E)){
            boolean ok=fight.trigger(Attack.SLOW_WAVE);
            System.out.println("EMP wave"+(ok?" triggered":" unavailable (active attack)"));
        }
        boolean left=inputManager.isKeyDown(KeyEvent.VK_LEFT)||inputManager.isKeyDown(KeyEvent.VK_A);
        boolean right=inputManager.isKeyDown(KeyEvent.VK_RIGHT)||inputManager.isKeyDown(KeyEvent.VK_D);
        fight.tick((right?1:0)-(left?1:0),inputManager.isKeyDown(KeyEvent.VK_SPACE));
        ship.setPositionX(fight.getPlayerX());ship.setPositionY(fight.getPlayerY());
        ship.setSkinColor(fight.isFrozen()?new Color(74,185,232):fight.isSlowed()?new Color(255,172,65):Color.GREEN);
        ship.update();
        draw();
    }
    private void draw(){
        drawManager.initDrawing(this);
        if(background!=null)drawManager.drawBackground(this,background);
        Graphics2D g=drawManager.getWorldGraphics();
        fight.draw(g);
        if(!fight.isLost()&&(!fight.isRecovering()||(fight.getFrame()/6)%2==0))
            drawManager.drawEntity(ship,ship.getPositionX(),ship.getPositionY());
        Graphics2D hud=(Graphics2D)g.create();
        try{
            fight.drawHud(hud,drawManager.getPixelFont(12f,false));
            hud.setColor(new Color(155,145,164));
            hud.setFont(new java.awt.Font(java.awt.Font.MONOSPACED,java.awt.Font.PLAIN,10));
            hud.drawString(fight.isAutomatic()?"AUTO ON":"AUTO OFF",width-65,54);
        }finally{hud.dispose();}
        drawManager.completeDrawing(this);
    }
}
