import entity.Superweapon;
import entity.SuperweaponFight;
import entity.SuperweaponFight.Attack;
import entity.SuperweaponFight.Kind;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

/** Standalone deterministic test: no game window, profiles or resource files. */
public final class SuperweaponFightTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static SuperweaponFight fight(boolean invincible){
        SuperweaponFight f=new SuperweaponFight(448,520,invincible,105L);f.setAutomatic(false);return f;
    }
    private static void advance(SuperweaponFight f,int frames,int direction,boolean fire){for(int i=0;i<frames;i++)f.tick(direction,fire);}
    private static SuperweaponFight phase2(){SuperweaponFight f=fight(true);f.skipShield();return f;}
    public static void main(String[] args){
        SuperweaponFight f=fight(true);
        check(f.getShield()==22&&f.getHull()==36,"initial health");
        check(!f.trigger(Attack.DRONES),"phase one excludes drones");
        for(int i=0;i<22;i++)check(f.hitBoss(224,200),"shield hit");
        check(f.isPhaseTwo()&&f.getHull()==36,"shield break preserves hull");
        check(f.trigger(Attack.REFLECT_SHIELD),"phase two reflection");
        check(!f.hitBoss(224,200)&&f.getHull()==36&&f.getReflectionCount()==1,"reflect without damage");
        check(f.getHostileShots().get(0).kind==Kind.REFLECTED,"reflected shot");
        advance(f,180,0,false);check(!f.isReflecting(),"reflection expires");
        for(int i=0;i<36;i++)check(f.hitBoss(224,200),"hull damage");
        check(f.isWon()&&f.getHostileShots().isEmpty(),"victory clears projectiles");

        f=fight(true);check(f.trigger(Attack.ALL_LASERS),"all lasers");
        check(f.getBeams().size()==8,"eight ports");
        for(SuperweaponFight.Beam b:f.getBeams())check(b.fire==66&&b.end==90,"laser timing");
        advance(f,65,0,false);check(f.getHits()==0,"warning no damage");
        advance(f,25,0,false);check(f.getBeams().isEmpty(),"laser expires");
        f=phase2();check(f.trigger(Attack.DRONES),"drone deploy");
        check(f.getDrones().size()>=2&&f.getDrones().size()<=5,"drone count");
        check(!f.trigger(Attack.DRONES),"no overlapping waves");
        check(f.trigger(Attack.BOMB),"bomb deployed");
        check(f.fireTestBullet(Kind.NORMAL),"normal projectile");
        check(f.trigger(Attack.MEGA_LASER),"mega triggered");
        check(f.isMegaLocked(),"mega locks attacks during charge");
        SuperweaponFight.Shot existing=f.getHostileShots().get(0);
        SuperweaponFight.Bomb falling=f.getBombs().get(0);
        double initialY=existing.y, bombY=falling.y;
        advance(f,1,0,false);
        check(f.getHostileShots().contains(existing)&&existing.y>initialY,"existing bullet continues travelling");
        check(f.getBombs().contains(falling)&&falling.y>bombY,"existing bomb keeps falling");
        f.setAutomatic(true);
        for(int i=0;i<166;i++){
            f.tick(0,false);check(f.isMegaLocked(),"lock persists");
            check(f.getHostileShots().stream().allMatch(s->s==existing),"no new bullets from boss or escorts");
            check(f.getBombs().stream().allMatch(b->b==falling),"no new bombs");
        }
        check(!f.trigger(Attack.HOMING),"manual trigger locked");
        f.tick(0,false);check(!f.isMegaLocked(),"lock ends");
        check(f.getLives()==3&&f.getHits()>0,"test player immune, hits counted");
        f=fight(false);check(f.trigger(Attack.MEGA_LASER),"normal mode mega");
        advance(f,119,0,false);check(f.getLives()==3,"mega warning safe");
        advance(f,10,0,false);check(f.getLives()==2,"mega deals one life once beam reaches player");
        int x=f.getPlayerX();advance(f,20,1,false);check(f.getPlayerX()>x&&f.getLives()==2,"movement during invincibility");
        f=phase2();check(f.trigger(Attack.REFLECT_SHIELD),"reflective shield before mega");
        check(f.trigger(Attack.MEGA_LASER),"mega during reflection");
        check(f.hitBoss(224,200)&&f.getHostileShots().isEmpty(),"no reflected shot during lock even before first tick");
        f=phase2();check(f.trigger(Attack.CONVERGE),"special combo");
        check(f.getBeams().size()==9,"eight side lasers and mega");
        for(int i=0;i<4;i++){SuperweaponFight.Beam a=f.getBeams().get(i*2),b=f.getBeams().get(i*2+1);check(a.port==i&&b.port==7-i&&a.start==b.start,"converging pairs");}
        SuperweaponFight.Beam mega=f.getBeams().get(8);check(mega.mega&&mega.start>f.getBeams().get(7).end,"central cannon after sides finish");
        f=phase2();check(f.trigger(Attack.SWEEP_RIGHT),"right sweep");for(int i=0;i<8;i++)check(f.getBeams().get(i).port==i,"right order");
        f=phase2();check(f.trigger(Attack.SWEEP_LEFT),"left sweep");for(int i=0;i<8;i++)check(f.getBeams().get(i).port==7-i,"left order");
        f=phase2();check(f.trigger(Attack.HOMING),"tracking shot");advance(f,30,1,false);check(f.getHostileShots().get(0).vx>0,"homes towards moving ship");
        f=fight(true);check(f.fireTestBullet(Kind.SPLIT)&&f.getHostileShots().size()==2,"split projectile");
        f=fight(true);check(f.trigger(Attack.BOMB),"bomb");
        double bx=f.getBombs().get(0).x;
        int direction=bx>f.getPlayerX()+13?1:-1;
        advance(f,(int)Math.round(Math.abs(bx-f.getPlayerX()-13)/2),direction,false);
        advance(f,120,0,true);check(f.getBombsDisarmed()==1,"bomb can be shot down");
        f=fight(false);check(f.trigger(Attack.BOMB),"bomb impact");
        bx=f.getBombs().get(0).x;direction=bx>f.getPlayerX()+13?1:-1;
        advance(f,(int)Math.round(Math.abs(bx-f.getPlayerX()-13)/2),direction,false);
        advance(f,180,0,false);check(f.getLives()==2,"bomb damages nearby ship once");
        f=fight(false);check(f.trigger(Attack.BOMB),"bomb distant");bx=f.getBombs().get(0).x;
        advance(f,250,bx<224?1:-1,false);check(f.getLives()==3,"far outside bomb blast safe");
        f=fight(true);advance(f,150,0,true);check(f.getShield()<22,"real player projectiles damage sprite");
        check(Superweapon.containsPixel(448,84,0,84),"full width left hull");
        check(Superweapon.containsPixel(448,84,447,84),"full width right hull");
        check(!Superweapon.containsPixel(448,84,0,263),"transparent corner");
        for(int seed=0;seed<40;seed++){
            f=new SuperweaponFight(448,520,true,seed);f.skipShield();
            for(int i=0;i<2500;i++)f.tick((i/70)%2==0?1:-1,i%2==0);
            check(f.getLives()==3&&f.getDrones().size()<=5,"randomized run "+seed);
            BufferedImage image=new BufferedImage(448,520,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();f.draw(g);f.drawHud(g);g.dispose();
        }
        System.out.println("SuperweaponFightTest passed: "+checks+" checks");
    }
}
