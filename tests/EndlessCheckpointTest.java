import java.io.File;
import java.nio.file.Files;

import engine.DiamondManager;
import engine.EndlessCheckpoint;
import engine.GameState;

/**
 * Standalone checks for endless diamond checkpoints (GOG-109). Uses a
 * temporary save folder, so the player's real save is never touched.
 */
public final class EndlessCheckpointTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    /** A run state going into {@code level} with some pending diamonds. */
    private static GameState state(int level, int pending) {
        return new GameState(level, 500, 2, 40, 30, pending, 5, 7);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("endless-checkpoint-test").toFile();
        System.setProperty("game.saveDirectory", dir.getAbsolutePath());

        int every = EndlessCheckpoint.WAVES_PER_CHECKPOINT;
        check(every == 5, "checkpoint interval changed: " + every);

        // Waves 5, 10, 15, ... are checkpoints; nothing else is.
        int count = 0;
        for (int wave = -1; wave <= 100; wave++) {
            boolean expected = wave > 0 && wave % 5 == 0;
            check(EndlessCheckpoint.isCheckpoint(wave) == expected, "wave " + wave);
            if (EndlessCheckpoint.isCheckpoint(wave)) count++;
        }
        check(count == 20, "expected 20 checkpoints in 100 waves, got " + count);

        // Next checkpoint after each wave.
        check(EndlessCheckpoint.nextCheckpoint(0) == 5, "before the first wave");
        check(EndlessCheckpoint.nextCheckpoint(-3) == 5, "negative wave");
        check(EndlessCheckpoint.nextCheckpoint(4) == 5, "after wave 4");
        check(EndlessCheckpoint.nextCheckpoint(5) == 10, "after a checkpoint, the next one");
        check(EndlessCheckpoint.nextCheckpoint(9) == 10, "after wave 9");
        for (int wave = 0; wave <= 100; wave++) {
            int next = EndlessCheckpoint.nextCheckpoint(wave);
            check(next > wave && EndlessCheckpoint.isCheckpoint(next) && next - wave <= every,
                    "next checkpoint after wave " + wave + " is " + next);
        }

        // Banking: only at a checkpoint, and only if something is pending.
        DiamondManager wallet = DiamondManager.getInstance();
        int start = wallet.getDiamonds();
        GameState wave4 = state(5, 10);
        check(EndlessCheckpoint.bankIfCheckpoint(wave4, 4) == wave4, "banked on a normal wave");
        check(wallet.getDiamonds() == start, "wallet changed on a normal wave");

        GameState nothing = state(6, 0);
        check(EndlessCheckpoint.bankIfCheckpoint(nothing, 5) == nothing, "nothing pending should change nothing");

        // Wave 5: 1+2+3+4+5 = 15 pending diamonds get banked.
        GameState wave5 = state(6, 15);
        GameState banked = EndlessCheckpoint.bankIfCheckpoint(wave5, 5);
        check(wallet.getDiamonds() == start + 15, "wave 5 should bank 15, wallet " + wallet.getDiamonds());
        check(banked.getPendingDiamonds() == 0, "pending not cleared after banking");
        check(banked.getLevel() == 6 && banked.getScore() == 500 && banked.getLivesRemaining() == 2
                && banked.getBulletsShot() == 40 && banked.getShipsDestroyed() == 30
                && banked.getLevelsCompletedRun() == 5 && banked.getCoinsCollected() == 7,
                "banking changed something other than pending diamonds");

        // Wave 10 banks only what was earned since wave 5 (6+7+8+9+10 = 40).
        EndlessCheckpoint.bankIfCheckpoint(state(11, 40), 10);
        check(wallet.getDiamonds() == start + 55, "after wave 10 the wallet should hold 55 more");

        // Saved to disk, not just in memory.
        File[] saved = dir.listFiles();
        check(saved != null && saved.length > 0, "nothing saved to the save folder");

        System.out.println("EndlessCheckpointTest: " + checks + " checks passed");
    }
}
