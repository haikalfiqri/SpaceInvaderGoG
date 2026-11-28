package entity;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * GOG-105 experimental post-Level-40 superweapon artwork.
 * Only its leading edge is visible. Combat lives in SuperweaponFight.
 * Each character in ART represents one pixel, like AICommander.
 */
public final class Superweapon {
    public static final int ART_WIDTH = 224;
    public static final int ART_HEIGHT = 90;
    public static final int PIXEL = 2;
    public static final int TOP_Y = 84; // Below the existing boss HUD.
    private static final BufferedImage IMAGE = createImage();
    private static final BufferedImage SHIELD_EDGE = createOverlay(true);
    private static final BufferedImage FLASH = createOverlay(false);

    private static final int FRAME_COUNT = 32;
    private static final int FRAME_STEP = 3;
    private static final BufferedImage[][] IDLE_FRAMES = createIdleFrames();

    private Superweapon() { }

    /** Animated artwork only: the silhouette, weapon positions and hitbox never move. */
    public static void drawAnimated(Graphics2D target, int width, int topY, int frame,
                                    boolean overdrive, int[] portX, int[] portY,
                                    float[] charge, float[] recoil, float mainCharge,
                                    float mainRecoil, float bayPulse) {
        if (width <= 0) return;
        Graphics2D g = (Graphics2D) target.create();
        try {
            int h = Math.max(1, Math.round(width * ART_HEIGHT / (float) ART_WIDTH));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.translate(0, topY);
            g.scale(width / (double) ART_WIDTH, h / (double) ART_HEIGHT);
            // Faint slipstream beside the hull, drifting upward. Never a bullet.
            drawSlipstream(g, frame, overdrive);
            int index = Math.floorMod(frame / FRAME_STEP, FRAME_COUNT);
            g.drawImage(IDLE_FRAMES[overdrive ? 1 : 0][index], 0, 0, null);
            // Gun shutters and inner rails shift inside the existing sprite.
            for (int i=0; i<portX.length; i++) {
                drawIdleGun(g, portX[i], portY[i], i, frame, overdrive);
                drawMechanism(g, portX[i], portY[i], charge[i], recoil[i], frame, false);
            }
            drawReactor(g,frame,overdrive,mainCharge);
            drawMechanism(g, ART_WIDTH/2, 85, mainCharge, mainRecoil, frame, true);
            if (bayPulse > 0) {
                int a = (int)(150 * clamp(bayPulse));
                for (int x : new int[]{45, 178}) {
                    for (int j=0;j<3;j++) {
                        int yy=24+j*3;
                        clippedRect(g,x-5,yy,10,1,new Color(255,40,56,a));
                    }
                }
            }
        } finally { g.dispose(); }
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f,v)); }

    /** Cached character-grid animation frames. Only lit pixels change colour. */
    private static BufferedImage[][] createIdleFrames() {
        BufferedImage[][] frames = new BufferedImage[2][FRAME_COUNT];
        for (int phase=0; phase<2; phase++) for(int i=0;i<FRAME_COUNT;i++) {
            BufferedImage img = new BufferedImage(ART_WIDTH,ART_HEIGHT,BufferedImage.TYPE_INT_ARGB);
            double angle = i * Math.PI * 2 / FRAME_COUNT;
            for(int y=0;y<ART_HEIGHT;y++) for(int x=0;x<ART_WIDTH;x++) {
                char c=ArtData.ART[y].charAt(x);
                int argb=IMAGE.getRGB(x,y);
                if(c=='.') continue;
                if ("morfyw".indexOf(c)>=0) {
                    // A travelling electrical pulse down the channels and around the reactor.
                    double distance=Math.hypot(x-111.5,(y-60)*1.3);
                    double theta=Math.atan2(y-60,x-111.5);
                    double wave=(distance<25 ? Math.sin(angle*2-theta*3) : Math.sin(angle*2-y*.24+Math.abs(x-111.5)*.06));
                    double factor=(phase==0?.80:.93)+(phase==0?.18:.23)*wave;
                    argb=light(argb,factor);
                } else if ((c=='l'||c=='s') && (y<36 || x<36 || x>187)) {
                    // Dim travelling highlights make the panels read as moving machinery.
                    double wave=Math.sin(angle-y*.13+x*.035);
                    if(wave>.91) argb=light(argb,1.04+(wave-.91)*.45);
                }
                img.setRGB(x,y,argb);
            }
            frames[phase][i]=img;
        }
        return frames;
    }
    private static int light(int rgb,double factor) {
        int r=(int)Math.min(255,((rgb>>16)&255)*factor);
        int g=(int)Math.min(255,((rgb>>8)&255)*factor);
        int b=(int)Math.min(255,(rgb&255)*factor);
        return 0xff000000|(r<<16)|(g<<8)|b;
    }
    private static void drawSlipstream(Graphics2D g,int frame,boolean overdrive) {
        // In space these are stylised motion streaks, not atmospheric wind.
        for(int i=0;i<16;i++) {
            int x=(i<8?2+i*12:ART_WIDTH-3-(i-8)*12);
            int edge=ART_HEIGHT-1;
            while(edge>=0 && (IMAGE.getRGB(x,edge)>>>24)==0) edge--;
            int travel=Math.floorMod(60-frame*(overdrive?2:1)+i*17,30);
            int y=edge+4+travel;
            int length=3+i%4;
            int a=(int)(45*Math.sin(Math.PI*travel/30.0));
            if(a>0) {
                g.setColor(new Color(132,120,147,a));
                g.fillRect(x,y,1,length);
            }
        }
    }
    private static void clippedRect(Graphics2D g,int x,int y,int w,int h,Color c) {
        g.setColor(c);
        for(int yy=Math.max(0,y);yy<Math.min(ART_HEIGHT,y+h);yy++)
            for(int xx=Math.max(0,x);xx<Math.min(ART_WIDTH,x+w);xx++)
                if((IMAGE.getRGB(xx,yy)>>>24)!=0)g.fillRect(xx,yy,1,1);
    }
    private static void drawIdleGun(Graphics2D g,int x,int y,int index,int frame,boolean overdrive) {
        // A little reciprocating metal piston inside each large gun housing.
        // The innermost guns have no rectangular housing, so only their lights pulse.
        if(index==3||index==4)return;
        int cycle=Math.floorMod(frame/(overdrive?3:5)+index*3,8);
        int slide=cycle<4?cycle:7-cycle;
        clippedRect(g,x-4,y-12,9,4,new Color(50,40,52));
        clippedRect(g,x-3,y-12+slide,7,1,new Color(140,132,148));
        clippedRect(g,x-2,y-12+slide,5,1,new Color(176,167,182));
    }
    private static void drawReactor(Graphics2D g,int frame,boolean overdrive,float charge) {
        // Segmented rotating reactor lights, with a brighter, faster overdrive ring.
        double theta=frame*(overdrive?.075:.04);
        for(int i=0;i<3;i++) {
            double t=theta+i*Math.PI*2/3;
            int x=112+(int)Math.round(Math.cos(t)*19);
            int y=60+(int)Math.round(Math.sin(t)*12);
            clippedRect(g,x-1,y,3,1,new Color(255,40,56,170));
            clippedRect(g,x,y,1,1,new Color(255,214,214,overdrive||charge>.5f?220:150));
        }
    }
    private static void drawMechanism(Graphics2D g,int x,int y,float charge,float recoil,int frame,boolean main) {
        charge=clamp(charge);recoil=clamp(recoil);
        if(charge<=0 && recoil<=0)return;
        int radius=main?6:3;
        int stroke=main?10:6;
        int retract=Math.round(recoil*(main?4:3));
        // Recess the barrel's bright strip, then let it return to its rest position.
        if(retract>0) {
            clippedRect(g,x-radius,y-stroke,radius*2+1,stroke+1,new Color(18,10,16,210));
            clippedRect(g,x-1,y-5-retract,3,5,new Color(255,40,56));
            clippedRect(g,x,y-5-retract,1,4,new Color(255,214,214));
            clippedRect(g,x-radius,y-retract,radius*2+1,1,new Color(140,132,148));
        }
        if(charge>0) {
            int a=(int)(75+charge*150);
            int progress=(int)(charge*(stroke-1));
            for(int j=0;j<=progress;j+=2)
                clippedRect(g,x-radius,y-j,radius*2+1,1,new Color(255,40,56,a));
            int pulse=1+(frame/4)%3;
            clippedRect(g,x-1,y-pulse,3,pulse+1,new Color(255,214,214,a));
            if(main) {
                // The central reactor powers up before the large cannon fires.
                for(int i=0;i<12;i++) {
                    double t=i*Math.PI/6+frame*.045;
                    int px=112+(int)Math.round(Math.cos(t)*19);
                    int py=60+(int)Math.round(Math.sin(t)*12);
                    clippedRect(g,px,py,2,2,new Color(255,214,214,(int)(180*charge)));
                }
            }
        }
        if(recoil>0) {
            // Short muzzle flash; deliberately separate from the actual bullet or laser.
            int a=(int)(220*recoil);
            int spread=(main?6:3)+(int)((1-recoil)*2);
            g.setColor(new Color(255,40,56,a/2));g.fillRect(x-spread,y,spread*2+1,3);
            g.setColor(new Color(255,214,214,a));g.fillRect(x-1,y-1,3,3);
            g.fillRect(x-spread/2,y+1,spread+1,1);
            if(recoil>.45f){g.fillRect(x-spread,y+3,1,2);g.fillRect(x+spread,y+3,1,2);}
        }
    }


    /** Draw across the entire logical playfield, without moving. */
    public static void draw(final Graphics2D target, final int screenWidth,
                            final int topY) {
        if (screenWidth <= 0) return;
        Graphics2D g = (Graphics2D) target.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int height = Math.max(1, Math.round((float) screenWidth
                    * ART_HEIGHT / ART_WIDTH));
            g.drawImage(IMAGE, 0, topY, screenWidth, height, null);
        } finally {
            g.dispose();
        }
    }

    /** True only over a visible pixel, using the same scaling as draw(). */
    public static boolean containsPixel(final int screenWidth, final int topY,
                                        final double x, final double y) {
        if (screenWidth <= 0 || x < 0 || x >= screenWidth || y < topY) return false;
        int height = Math.max(1, Math.round((float) screenWidth * ART_HEIGHT / ART_WIDTH));
        if (y >= topY + height) return false;
        int px = (int) (x * ART_WIDTH / screenWidth);
        int py = (int) ((y - topY) * ART_HEIGHT / height);
        return (IMAGE.getRGB(px, py) >>> 24) != 0;
    }

    /** A silhouette-following barrier / damage flash, not a rectangular overlay. */
    public static void drawOverlay(Graphics2D target, int width, int topY,
                                   boolean shield, float opacity) {
        Graphics2D g = (Graphics2D) target.create();
        try {
            g.setComposite(java.awt.AlphaComposite.SrcOver.derive(opacity));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(shield ? SHIELD_EDGE : FLASH, 0, topY, width,
                    Math.round(width * ART_HEIGHT / (float) ART_WIDTH), null);
        } finally { g.dispose(); }
    }

    private static BufferedImage createOverlay(boolean edge) {
        BufferedImage result = new BufferedImage(ART_WIDTH, ART_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        for (int y=0; y<ART_HEIGHT; y++) for (int x=0; x<ART_WIDTH; x++) {
            if ((IMAGE.getRGB(x,y) >>> 24)==0) continue;
            boolean border = y==ART_HEIGHT-1 || (IMAGE.getRGB(x,y+1) >>> 24)==0
                    || (x>0 && (IMAGE.getRGB(x-1,y) >>> 24)==0)
                    || (x<ART_WIDTH-1 && (IMAGE.getRGB(x+1,y) >>> 24)==0);
            if (!edge || border) result.setRGB(x,y,edge ? 0xFFB9E8FF : 0xFFFFD6D6);
        }
        return result;
    }

    private static int palette(final char c) {
        switch (c) {
            case '.': return 0;
            case 'k': return 0xFF120A10;
            case 'd': return 0xFF322834;
            case 'a': return 0xFF423744;
            case 'g': return 0xFF544C5A;
            case 'l': return 0xFF8C8494;
            case 's': return 0xFFB0A7B6;
            case 'p': return 0xFF322834;
            case 'm': return 0xFF960E1E;
            case 'o': return 0xFF960E1E;
            case 'r': return 0xFFFF2838;
            case 'f': return 0xFFFF2838;
            case 'y': return 0xFFFF8A80;
            case 'w': return 0xFFFFD6D6;
            default: throw new IllegalArgumentException("Unknown art pixel: " + c);
        }
    }

    private static BufferedImage createImage() {
        // The nested holder avoids accessing ART before it is initialized.
        String[] art = ArtData.ART;
        if (art.length != ART_HEIGHT) throw new IllegalStateException("Art height");
        BufferedImage image = new BufferedImage(ART_WIDTH, ART_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < ART_HEIGHT; y++) {
            if (art[y].length() != ART_WIDTH)
                throw new IllegalStateException("Art width at row " + y);
            for (int x = 0; x < ART_WIDTH; x++)
                image.setRGB(x, y, palette(art[y].charAt(x)));
        }
        return image;
    }

    private static final class ArtData {
        private static final String[] ART = {
                "aaaakkddddddddddddddddkkllgggggglllkkdddddddddddddkssssaaaaaaaaaaaaaaaaaasssssskkaaaaaakkdddddkkllgggggggddryrddddryrddgggggggllkkdddddkkaaaaaakkssssssaaaaaaaaaaaaaaaaaasssskdddddddddddddkklllggggggllkkddddddddddddddddkkaaaa",
                "aaaakkdlllllllllllllldkkllgggggglllkkllllllllllllldsssssaaaaaaaaaaaaaaaaasssssskkaaaaaakkdllllkkllgggggggddryrddddryrddgggggggllkklllldkkaaaaaakkssssssaaaaaaaaaaaaaaaaasssssdlllllllllllllkklllggggggllkkdlllllllllllllldkkaaaa",
                "aaaakkdlllllllllllllldkkllgggggglllkkllllllllllllldkggggaaaaaaaaaaaaaaaaaggggsskkaaaaaakkdllllkkllgggggggddryrddddryrddgggggggllkklllldkkaaaaaakkssggggaaaaaaaaaaaaaaaaaggggkdlllllllllllllkklllggggggllkkdlllllllllllllldkkaaaa",
                "aaaakkdlllllllllllllldkkllgggggglllkkllllllllllllldkkggggaaaaaaaaaaaaaaaaggggsskkaaaaaakkdllllkkllgggggggddryrddddryrddgggggggllkklllldkkaaaaaakkssggggaaaaaaaaaaaaaaaaggggkkdlllllllllllllkklllggggggllkkdlllllllllllllldkkaaaa",
                "aaaakkddddddddddddddddkkllgggggglllkkddddddddddddddkkggggaaaaaaaaaaaaaaaaggggsskkaaaaaakkdddddkkllgggggggddryrddddryrddgggggggllkkdddddkkaaaaaakkssggggaaaaaaaaaaaaaaaaggggkkddddddddddddddkklllggggggllkkddddddddddddddddkkaaaa",
                "aaaakkddssddssddssddddkkllgggggglllkkssddssddssddddkkkggggaaaaaaaaaaaaaaaggggsskkaaaaaakkddssdkkllgggggggddryrddddryrddgggggggllkkdssddkkaaaaaakkssggggaaaaaaaaaaaaaaaggggkkkddddssddssddsskklllggggggllkkddddssddssddssddkkaaaa",
                "aaaakkddssddssddssddddkkllgggggglllkkssddssddssddddkkaggggaaaaaaaaaaaaaaagggggsskaaaaaakkddssdkkllgggggggddryrddddryrddgggggggllkkdssddkkaaaaaakssgggggaaaaaaaaaaaaaaaggggakkddddssddssddsskklllggggggllkkddddssddssddssddkkaaaa",
                "aaaakkddggddggddggddddkkllgggggglllkkkkkkkkkkkkkkkkkkkkggggaaaaaaaaaaaaaaaggggsskaaaaaakkddggdkkllgggggggddryrddddryrddgggggggllkkdggddkkaaaaaakssggggaaaaaaaaaaaaaaaggggkkkkkkkkkkkkkkkkkkkklllggggggllkkddddggddggddggddkkaaaa",
                "kkkkkkkkkkkkkkkkkgddddkkllgggggglllkkkkkkkkkkkkkkkkkkkkggggaaaaaaaaaaaaaaaggggsskaaaaaakkddggdkkllgggggggddryrddddryrddgggggggllkkdggddkkaaaaaakssggggaaaaaaaaaaaaaaaggggkkkkkkkkkkkkkkkkkkkklllggggggllkkddddgkkkkkkkkkkkkkkkkk",
                "llllllllllllllllkkkdddkkllggggggllllllllllllllllllllllllggggaaaaaaaaaaaaaaggggsskaaaaaakkddggdkkllgggggggddryrddddryrddgggggggllkkdggddkkaaaaaakssggggaaaaaaaaaaaaaaggggllllllllllllllllllllllllggggggllkkdddkkkllllllllllllllll",
                "llllllllllllllllllkkddkklllgggggllllllllllllllllllllllllgggggaaaaaaaaaaaaaggggsskaaaaaakkddggdkkllgggggggddryrddddryrddgggggggllkkdggddkkaaaaaakssggggaaaaaaaaaaaaagggggllllllllllllllllllllllllggggglllkkddkkllllllllllllllllll",
                "ggggggggggggggggglskkkkkklllgggggggggggggggggggggggggglllkgggggaaaaaaaaaaaggggsskkaaaaakkddggdkklllggggggddryrddddryrddgggggglllkkdggddkkaaaaakkssggggaaaaaaaaaaagggggklllgggggggggggggggggggggggggglllkkkkkkslggggggggggggggggg",
                "ggggggggggggggggggkklkkkkkllggggggggggggggggggggggggggglllkgggggaaaaaaaaaaggggssskkkaaakkddggdkkklllgggggddryrddddryrddggggglllkkkdggddkkaaakkksssggggaaaaaaaaaagggggklllgggggggggggggggggggggggggggllkkkkklkkgggggggggggggggggg",
                "gggggggggggggggggggglllkkkkklggggggggggggggggggggggggggggllkkgggggaaaaaaaagggggsssskkaakkddddddkkklllggggddryrddddryrddgggglllkkkddddddkkaakkssssgggggaaaaaaaagggggkkllgggggggggggggggggggggggggggglkkkkklllgggggggggggggggggggg",
                "gggggggggggggggggggggglllkkkklgggggggggggggggggggggggggggglllkgggggaaaaaaaaggggggssskkakkdooooookkklllgggddryrddddryrddggglllkkkoooooodkkakksssggggggaaaaaaaagggggklllgggggggggggggggggggggggggggglkkkklllgggggggggggggggggggggg",
                "ggggggggggggggggggggggggllkkkklllllllllllllllgggggggggggggglllkkgskggaaaaaaaagggggssskkkkddddddddkkklllggddryrddddryrddgglllkkkddddddddkkkksssgggggaaaaaaaaggksgkklllgggggggggggggglllllllllllllllkkkkllgggggggggggggggggggggggg",
                "ggggggggggggggggggggggggggllkkkkkkkkkkkkkkkkklggggggggggggggglllkkkgggaaaaaaaagggggssskkkkkkkkkkkkkkklllgddryrddddryrddglllkkkkkkkkkkkkkkksssgggggaaaaaaaagggkkklllggggggggggggggglkkkkkkkkkkkkkkkkkllgggggggggggggggggggggggggg",
                "dggggggggggggggggggggggggggllkkkkkkkkkkkkkkkkkllgggggggggggggglllkkgggggaaaaaaagggggsssskkkkkkkkkkkkkkllgddryrddddryrddgllkkkkkkkkkkkkkkssssgggggaaaaaaagggggkklllggggggggggggggllkkkkkkkkkkkkkkkkkllggggggggggggggggggggggggggd",
                "ddddggggggggggggggggggggggggglkkkkkkkkkkkkkkkkkklggggggggggggggllllkkggggaaaaaaaggggggssskkkaaaaaaakkkllgddryrddddryrddgllkkkaaaaaaakkksssggggggaaaaaaaggggkkllllgggggggggggggglkkkkkkkkkkkkkkkkkklgggggggggggggggggggggggggdddd",
                "kkkddddgggggggggggggggggggggggggggggggggllllkkkkkllgggggggggggggglllkkggggaaaaaaaagggggssskkkaaaaaakkkllgddryrddddryrddgllkkkaaaaaakkksssgggggaaaaaaaaggggkklllggggggggggggggllkkkkkllllgggggggggggggggggggggggggggggggggddddkkk",
                "aakkkkdddggggggggggggggggggggggskggggggggglllkkkkkkllggggggggggggglllkkkggggaaaaaaagggggssskkkkaaaakkkllgddryrddddryrddgllkkkaaaakkkksssgggggaaaaaaaggggkkklllgggggggggggggllkkkkkklllgggggggggksggggggggggggggggggggggdddkkkkaa",
                "aaaakkkkddddgggggggggggggggggggkkggggggggggllllkkkkkklgggggggggggglllkkkkkgggaaaaaaagggggsssskkkaaakkkllgddryrddddryrddgllkkkaaakkkssssgggggaaaaaaagggkkkkklllgggggggggggglkkkkkkllllggggggggggkkgggggggggggggggggggddddkkkkaaaa",
                "aaaaaaakkkkdddddddddddddddddddggggggggggggskglllkkkkkkllgggggggggglllkkaakkggggaaaskaggggggssskkaaakkkllgddryrddddryrddgllkkkaaakksssggggggaksaaaggggkkaakklllggggggggggllkkkkkklllgksggggggggggggdddddddddddddddddddkkkkaaaaaaa",
                "akkkkkkkkkkkkkkkkkkddddddddddddgggggggggggkkgglllkkkkkkklggggggggglllkkaaakkkgggaakkaaagggggskkkaaakkkllgddryrddddryrddgllkkkaaakkksgggggaaakkaagggkkkaaakklllggggggggglkkkkkkklllggkkgggggggggggddddddddddddkkkkkkkkkkkkkkkkkka",
                "akoffffffffffffffokkkkkkkkkkkdddgggggggggggggggglllkkkkkkllggggggglllkkaaaaakkggggaaaaaagggggkkkaaakkkllgddryrddddryrddgllkkkaaakkkgggggaaaaaaggggkkaaaaakklllgggggggllkkkkkklllggggggggggggggggdddkkkkkkkkkkkoffffffffffffffoka",
                "akoorrrrrrrrrrooookkkkkkkkkkkkddddggggggggggggggglllkkkkkkklgggggglllkkaaaaaakkkgggaaaaagggggkkkaaakkkllgddryrddddryrddgllkkkaaakkkgggggaaaaagggkkkaaaaaakklllgggggglkkkkkkklllgggggggggggggggddddkkkkkkkkkkkkoooorrrrrrrrrrooka",
                "akooooooooooooooookaaaaaaaakkkkkdddgggggggggggggggllllkkkkkkllgggglllkkaaaakkkgggggaaaaagggggkkkaaakkkllgddryrddddryrddgllkkkaaakkkgggggaaaaagggggkkkaaaakklllggggllkkkkkkllllgggggggggggggggdddkkkkkaaaaaaaakooooooooooooooooka",
                "akkkkkkkkkkkkkkkkkkaaaaaaaaaakkkkdddgggggggggggggggglllkkkkkkklllllllkkaskkkggggggaaaaaagggggkkkaaakkkllgddryrddddryrddgllkkkaaakkkgggggaaaaaaggggggkkksakklllllllkkkkkkklllggggggggggggggggdddkkkkaaaaaaaaaakkkkkkkkkkkkkkkkkka",
                "akkkkkkkkkkkkkkkkkkaaaaaaaaaaakkkkdddggggggggggggggggllllkkkkkkkkkkkkkkkkkkgggggggaaaaaagggggkkkaaakkkllgddryrddddryrddgllkkkaaakkkgggggaaaaaagggggggkkkkkkkkkkkkkkkkkkllllggggggggggggggggdddkkkkaaaaaaaaaaakkkkkkkkkkkkkkkkkka",
                "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkakkkkdddggggggggggggggggglllkkkkkkkkkkkkkkkgggggggggaaaaaggggggkkkaaakkkllgddryrddddryrddgllkkkaaakkkggggggaaaaagggggggggkkkkkkkkkkkkkkklllgggggggggggggggggdddkkkkakkkkkkkkkkkkkkkkkkkkkkkkkkkkkk",
                "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkaakkkddddgggggggggggggggglkkkakkkkkkkkkkgggggggggggaaaaaggggggkkaaaakkkllgddryrddddryrddgllkkkaaaakkggggggaaaaagggggggggggkkkkkkkkkkakkklggggggggggggggggddddkkkaakkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk",
                "kkkkkkkkkkkkkkksssssssssssssskkkkkkkkkkkkkgggggggggggggggkkkaaaaaakkkggggggggggggaaaaaaggggggkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkggggggaaaaaaggggggggggggkkkaaaaaakkkgggggggggggggggkkkkkkkkkkkkksssssssssssssskkkkkkkkkkkkkkk",
                "kkgggggggggggkkkgggggggggggggkkkffffffffokggggggggggggggkskkaaaaaakkkggggggggggggaaaaaagggggkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkgggggaaaaaaggggggggggggkkkaaaaaakkskggggggggggggggkoffffffffkkkgggggggggggggkkkgggggggggggkk",
                "kkglllllllllgkkkdddddddddddggkkkkrrrrooookddddggggggggggkkkkaaaaaakkkggggggggggggaaaaaaggggkksssssssssssssssssssssssssssssssssssssskkggggaaaaaaggggggggggggkkkaaaaaakkkkggggggggggddddkoooorrrrkkkkggdddddddddddkkkglllllllllgkk",
                "kkglllllllllgkkkdyoddyoddyoggkkkkooooooookkkkddddddgggggkkkaaaaaaakkkgggggggggggaaaagggggkkksssssssssssssssssssssssssssssssssssssssskkkgggggaaaagggggggggggkkkaaaaaaakkkgggggddddddkkkkooooooookkkkggoyddoyddoydkkkglllllllllgkk",
                "kkgggggggggggkkkdfoddfoddfoggkkkkkkkkkkkkkkkkkkkkkddddddkkkaaaaaaakkkgggggggggggagggggggkkssssggggggggggggggggggggggggggggggggggggsssskkgggggggagggggggggggkkkaaaaaaakkkddddddkkkkkkkkkkkkkkkkkkkkkggofddofddofdkkkgggggggggggkk",
                "kkggdddddddggkkkdfoddfoddfoggkkkkkkkkkkkkkaakkkkkkkkkkkdkkkaaaaaaakkkggggggggggggggggggkksssggggggggggggggggggggggggggggggggggggggggssskkggggggggggggggggggkkkaaaaaaakkkdkkkkkkkkkkkaakkkkkkkkkkkkkggofddofddofdkkkggdddddddggkk",
                "kkggdddddddggkkkdfoddfoddfoggkkkkaaaaaaaaaaaaaaaakkkkkkkkkkaaaaaaakkkgggggggggggggggggkksssggggddddddddddddddddddddddddddddddddddggggssskkgggggggggggggggggkkkaaaaaaakkkkkkkkkkaaaaaaaaaaaaaaaakkkkggofddofddofdkkkggdddddddggkk",
                "kkggdddddddggkkkdskddooddooggkkkkaaaaaaaaaaaaaaaaaaaakkkkkkaaaaaaakkkgggggggggggggggkkksssggggddddddddddddddddddddddddddddddddddddggggssskkkgggggggggggggggkkkaaaaaaakkkkkkaaaaaaaaaaaaaaaaaaaakkkkggooddooddksdkkkggdddddddggkk",
                "kkggdddddddggkkkdkkddooddooggkkkkaaaaaaaaaaaaaaakkkkkkkkkkkkkkkkkkkkkkkggggggggggggkkssssggggddddddddddddddddddddddddddddddddddddddggggsssskkggggggggggggkkkkkkkkkkkkkkkkkkkkkkkaaaaaaaaaaaaaaakkkkggooddooddkkdkkkggdddddddggkk",
                "kkgggggggggggkkddddddddddddggkkkkaaaaaaaaaaaaaaakoffffffffffffffokkkkkkkkkggggggggkksssggggddddddddddddddddddddddddddddddddddddddddddggggssskkggggggggkkkkkkkkkoffffffffffffffokaaaaaaaaaaaaaaakkkkggddddddddddddkkgggggggggggkk",
                ".kkkaaaaaakkkkgggggggggggggggkkkkaaaaaaaaaaaaaaakoorrrrrrrrrrooookaakkkkkkkkgggggkksssggggddddddddddddddddddkkkkkkkkddddddddddddddddddggggssskkgggggkkkkkkkkaakoooorrrrrrrrrrookaaaaaaaaaaaaaaakkkkgggggggggggggggkkkkaaaaaakkk.",
                ".kkkaooooakkkgggggggggggggggkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkoooookaaaaakkkkkkkgkkksssggggdddddddddddddddddddkrrrrrrkdddddddddddddddddddggggssskkkgkkkkkkkaaaaakoooookkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkgggggggggggggggkkkaooooakkk.",
                "..kkaoffoakkkrrrrrrrrrrrrrkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkaaaaaaakkkkkkkssssggggddddddddddddddddddkkkrryyrrkkkddddddddddddddddddggggsssskkkkkkkaaaaaaakkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkrrrrrrrrrrrrrkkkaoffoakk..",
                "..kkaoyfoakkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkssssssssssssssskkkkkkkaaaaaaaaaakkksssggggdddddddddddddddkkkkkookrryyrrkookkkkkdddddddddddddddggggssskkkaaaaaaaaaakkkkkkkssssssssssssssskkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkaofyoakk..",
                "..kkaoyfoakkkkkkkkkkkkkkkkkkkkkgggggggggggkkkggggggggggggggkkkaaaaaaaaaaaaaaaakksggggddddddddddddddkkkoooorrkrryyrrkrrooookkkddddddddddddddggggskkaaaaaaaaaaaaaaaakkkggggggggggggggkkkgggggggggggkkkkkkkkkkkkkkkkkkkkkaofyoakk..",
                "...kaoyfoakkkkkkkkkkkkkkkkkkkkkglllllllllgkkkddddddddddddggkkkkaaaaaaaaaaaaaakkkggggddddddddddddkkkoooorrrrrkrryyrrkrrrrrooookkkddddddddddddggggkkkaaaaaaaaaaaaaakkkkggddddddddddddkkkglllllllllgkkkkkkkkkkkkkkkkkkkkkaofyoak...",
                "...kaoyfoak..........kkkkkkkkkkglllllllllgkkkddyoddyoddyoggkkkkaaaaaaaaaaaaaakkgggggdddddddddddkkooorrrrrrrfkkkkkkkkfrrrrrrroookkdddddddddddgggggkkaaaaaaaaaaaaaakkkkggoyddoyddoyddkkkglllllllllgkkkkkkkkkk..........kaofyoak...",
                "...kkoyfokk.................kkkgggggggggggkkkddfoddfoddfoggkkkkaaaaaaaaaaaaaakkgggggdddddddddkkooorrrrrffffffffffffffffffrrrrroookkdddddddddgggggkkaaaaaaaaaaaaaakkkkggofddofddofddkkkgggggggggggkkk.................kkofyokk...",
                "...kkkffkkk.................kkkggdddddddggkkkddfoddfoddfoggkkkkaaaaaaaaaaaaaakkgggkkkkddddddkkoorrrrfffffffkkkkkkkkkkfffffffrrrrookkddddddkkkkgggkkaaaaaaaaaaaaaakkkkggofddofddofddkkkggdddddddggkkk.................kkkffkkk...",
                "............................kkkggdddddddggkkkddskddfoddfoggkkkkaaaaaaaaaaaaakkkgggkffkdddddkkoorrrffffffkkkggggggggggkkkffffffrrrookkdddddkffkgggkkkaaaaaaaaaaaaakkkkggofddofddksddkkkggdddddddggkkk............................",
                "............................kkkggdddddddggkkkddkkddooddooggkkkkaaaaakkkkkkkkkkkgggkffkddddkkoorrrffffkkkggggggggggggggggkkkffffrrrookkddddkffkgggkkkkkkkkkkkaaaaakkkkggooddooddkkddkkkggdddddddggkkk............................",
                "............................kkkggdddddddggkkkddooddooddooggkkkkaaaaakkkkkkkkkkkgggkyfkdddkkoorrrffffkkggggggooooooooggggggkkffffrrrookkdddkyfkgggkkkkkkkkkkkaaaaakkkkggooddooddooddkkkggdddddddggkkk............................",
                ".............................kkgggggggggggkkdddddddddddkkkkkkkkkkkkkkksssssskkggggkyfkddkkllllllkfkkkggggoooffffffffoooggggkkkfkllllllkkddkyfkggggkksssssskkkkkkkkkkkkkkkdddddddddddkkgggggggggggkk.............................",
                "..............................kkkaaaaaakkkkggggggggggggkkkkkkkkkkkkkkkggggggkkggggkyfkdkkkllllllkkkkgggooofffyyyyyyfffooogggkkkkllllllkkkdkyfkggggkkggggggkkkkkkkkkkkkkkkggggggggggggkkkkaaaaaakkk..............................",
                "..............................kkkaooooakkkgggggggggggggkkkkkkkkkkkkkkkkgdddkkkggggkyfkdkkkkkkkkkkkkgggoooffyyyyyyyyyyffooogggkkkkkkkkkkkkdkyfkggggkkkdddgkkkkkkkkkkkkkkkkgggggggggggggkkkaooooakkk..............................",
                "...............................kkaoffoakkkkrrrrrrrrrrrrkkkgggggggggggkkkdyokkkggggkyfkdkkkkkkkkkkkgggoooffyyyywwwwyyyyffooogggkkkkkkkkkkkdkyfkggggkkkoydkkkgggggggggggkkkrrrrrrrrrrrrkkkkaoffoakk...............................",
                "...............................kkaoyfoakkkkkkkkkkkkkkkkkkkglllllllllgkkkdfokkkgggdkyfkkkoorrfffkkkgggooffyyywwwwwwwwyyyffoogggkkkfffrrookkkyfkdgggkkkofdkkkglllllllllgkkkkkkkkkkkkkkkkkkkaofyoakk...............................",
                "...............................kkaoyfoakkkkkkkkkkkkkkkkkkkglllllllllgkkkdfokkggggdkffkkkoorrfffkkgggooffyyywwwwwwwwwwyyyffoogggkkfffrrookkkffkdggggkkofdkkkglllllllllgkkkkkkkkkkkkkkkkkkkaofyoakk...............................",
                "................................kaoyfoak.kkkkkkkkkkkkkkkkkgggggggggggkkkdfkkkggggdkffkkkoorrfffkkgggooffyyywwwwwwwwwwyyyffoogggkkfffrrookkkffkdggggkkkfdkkkgggggggggggkkkkkkkkkkkkkkkkk.kaofyoak................................",
                "................................kaoyfoak...............kkkggdddddddggkkkdokkkppgggkkkkkkoorrfffkkgggooffyyywwwwwwwwwwyyyffoogggkkfffrrookkkkkkgggppkkkodkkkggdddddddggkkk...............kaofyoak................................",
                "................................kkoyfokk...............kkkggdddddddggkkkdokkkkppggddddkkoorrfffkkgggooffyyywwwwwwwwwwyyyffoogggkkfffrrookkddddggppkkkkodkkkggdddddddggkkk...............kkofyokk................................",
                "................................kkkffkkk...............kkkggdddddddggkkkdddkkkppgggdddkkoorrfffkkkgggooffyyywwwwwwwwyyyffoogggkkkfffrrookkdddgggppkkkdddkkkggdddddddggkkk...............kkkffkkk................................",
                ".......................................................kkkggdddddddggkkkgggkkkkppgggdddkkkggggggkkgggoooffyyyywwwwyyyyffooogggkkggggggkkkdddgggppkkkkgggkkkggdddddddggkkk.......................................................",
                "........................................................kkgggggggggggkkgggggkkkppgggdddkkkggggggkkkgggoooffyyyyyyyyyyffooogggkkkggggggkkkdddgggppkkkgggggkkgggggggggggkk........................................................",
                ".........................................................kkkaaaaaakkkkkkkrrrkkkkppgggddkkkkkkkkkkkkkgggooofffyyyyyyfffooogggkkkkkkkkkkkkkddgggppkkkkrrrkkkkkkkaaaaaakkk.........................................................",
                ".........................................................kkkaooooakkkmkkkkkkkkkkppgggdddkkkkkkkkkfkkkggggoooffffffffoooggggkkkfkkkkkkkkkdddgggppkkkkkkkkkkmkkkaooooakkk.........................................................",
                "..........................................................kkaoffoakkkkkkkkkkkkkkkppgggdddkkoorrrffffkkggggggooooooooggggggkkffffrrrookkdddgggppkkkkkkkkkkkkkkkaoffoakk..........................................................",
                "..........................................................kkaoyfoakk...kkkkkkkkkkkppgggdddkkoorrrffffkkkggggggggggggggggkkkffffrrrookkdddgggppkkkkkkkkkkk...kkaofyoakk..........................................................",
                "..........................................................kkaoyfoakk...........kkkppgggddddkkoorrrffffffkkkggggggggggkkkffffffrrrookkddddgggppkkk...........kkaofyoakk..........................................................",
                "...........................................................kaoyfoak............kkkkppgggddddkkoorrrrfffffffkkkkkkkkkkfffffffrrrrookkddddgggppkkkk............kaofyoak...........................................................",
                "...........................................................kaoyfoak............kkkkppgggggdddkkooorrrrrffffffffffffffffffrrrrroookkdddgggggppkkkk............kaofyoak...........................................................",
                "...........................................................kkoyfokk............kkkkkppggggggdddkkooorrrrrrrffffffffffrrrrrrroookkdddggggggppkkkkk............kkofyokk...........................................................",
                "...........................................................kkkffkkk.............kkkkppggggggggddkkkoooorrrrrrrrrrrrrrrrrrooookkkddggggggggppkkkk.............kkkffkkk...........................................................",
                ".................................................................................kkkkppgggggggggdddkkkoooorrrrrrrrrrrrooookkkdddgggggggggppkkkk.................................................................................",
                ".................................................................................kkkkpppggggggggggdddkkkkkookkkkkkkkookkkkkdddggggggggggpppkkkk.................................................................................",
                "..................................................................................kkkkkkpppgggggggggddddddkkkooooookkkddddddgggggggggpppkkkkkk..................................................................................",
                "..................................................................................kkakkkkkpppgggggggggggggggkooffookgggggggggggggggpppkkkkkakk..................................................................................",
                "..................................................................................kkaoyfkkkkkpppggggggggggggkooffookggggggggggggpppkkkkkfyoakk..................................................................................",
                "...................................................................................kaoyfoakkkkkpppggggggggggkooyfookggggggggggpppkkkkkaofyoak...................................................................................",
                "...................................................................................kaoyfoak.kkkkkkppppggggggkooyfookggggggppppkkkkkk.kaofyoak...................................................................................",
                "...................................................................................kkoyfokk...kkkkkkpppgggggkooyfookgggggpppkkkkkk...kkofyokk...................................................................................",
                "...................................................................................kkkffkkk......kkkkpppggggkooyfookggggpppkkkk......kkkffkkk...................................................................................",
                "...................................................................................................kkkpppgggkooyfookgggpppkkk...................................................................................................",
                "....................................................................................................kkkppgggkkkffkkkgggppkkk....................................................................................................",
                ".....................................................................................................kkpppgggggffgggggpppkk.....................................................................................................",
                "......................................................................................................kkpppggggggggggpppkk......................................................................................................",
                ".......................................................................................................kkppppppppppppppkk.......................................................................................................",
                "........................................................................................................kkppppppppppppkk........................................................................................................",
                ".........................................................................................................kkkkkkkkkkkkkk.........................................................................................................",
        };
    }
}
