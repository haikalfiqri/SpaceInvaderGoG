import engine.EndlessCheckpoint;

/** Standalone checks for endless diamond checkpoints (GOG-109). */
public final class EndlessCheckpointTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
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

        System.out.println("EndlessCheckpointTest: " + checks + " checks passed");
    }
}
