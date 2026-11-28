package entity;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * GOG-105 experimental post-Level-40 superweapon artwork.
 * Only its leading edge is visible. No movement or combat logic yet.
 * Each character in ART represents one pixel, like AICommander.
 */
public final class Superweapon {
    public static final int ART_WIDTH = 224;
    public static final int ART_HEIGHT = 90;
    public static final int PIXEL = 2;
    public static final int TOP_Y = 84; // Below the existing boss HUD.
    private static final BufferedImage IMAGE = createImage();

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

    private static int palette(final char c) {
        switch (c) {
            case '.': return 0;
            case 'k': return 0xFF0F0C16;
            case 'd': return 0xFF201D2B;
            case 'a': return 0xFF302B3B;
            case 'g': return 0xFF454050;
            case 'l': return 0xFF655E6F;
            case 's': return 0xFF8F8696;
            case 'p': return 0xFF411439;
            case 'm': return 0xFF741841;
            case 'o': return 0xFF71141F;
            case 'r': return 0xFFBE2128;
            case 'f': return 0xFFF74323;
            case 'y': return 0xFFFF9831;
            case 'w': return 0xFFFFE49B;
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
