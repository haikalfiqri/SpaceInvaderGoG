import engine.GameSettings;
import engine.ShardSpace.Mode;
import engine.ShardSpaceBackgrounds;
import engine.ShardSpaceBosses;
import engine.ShardSpaceEnemies;

/**
 * Standalone checks for the Shard Space hooks (GOG-49). Until Ammar and
 * Wafi fill them in, every hook must answer "none", so GameScreen keeps
 * the story boss, enemies and background. When your part is in, change
 * the matching check to expect your boss, formation or image.
 */
public final class ShardSpaceHooksTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        GameSettings settings = new GameSettings(5, 4, 60, 2000);
        for (Mode mode : Mode.values()) {
            check(ShardSpaceBackgrounds.fileFor(mode).equals(mode.name().toLowerCase() + ".png"),
                    mode + " file name");
            // Loaded once, then the same image (or null while there is no art).
            check(ShardSpaceBackgrounds.forMode(mode) == ShardSpaceBackgrounds.forMode(mode),
                    mode + " background cached");
            // GOG-106: every mode has its own 448x520 background.
            check(ShardSpaceBackgrounds.forMode(mode) != null, mode + " background exists");
            check(ShardSpaceBackgrounds.forMode(mode).getWidth() == 448
                    && ShardSpaceBackgrounds.forMode(mode).getHeight() == 520, mode + " background size");
            check(ShardSpaceBosses.create(mode, 448) == null, mode + " boss placeholder");
            check(ShardSpaceEnemies.createFormation(settings, mode) == null,
                    mode + " enemies placeholder");
        }
        System.out.println("ShardSpaceHooksTest: " + checks + " checks passed");
    }
}
