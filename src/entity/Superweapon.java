package entity;

import java.awt.Graphics2D;
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

    private Superweapon() { }

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
