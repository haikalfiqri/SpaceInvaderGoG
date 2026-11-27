package engine;

/**
 * Diamond checkpoints for endless mode (GOG-109).
 *
 * An endless run only ends when the player dies, and dying forfeits the
 * run's pending diamonds, so without checkpoints endless never pays out.
 * Every {@link #WAVES_PER_CHECKPOINT} waves cleared is a checkpoint: the
 * diamonds pending at that point are banked for good, and dying only loses
 * what was earned since the last checkpoint.
 */
public final class EndlessCheckpoint {

	/** Waves to clear between checkpoints. */
	public static final int WAVES_PER_CHECKPOINT = 5;

	/** Not used. Static class. */
	private EndlessCheckpoint() {
	}

	/**
	 * @param clearedWave
	 *            Number of the wave just cleared (1-based).
	 * @return True if clearing this wave reaches a checkpoint.
	 */
	public static boolean isCheckpoint(final int clearedWave) {
		return clearedWave > 0 && clearedWave % WAVES_PER_CHECKPOINT == 0;
	}

	/**
	 * @param clearedWave
	 *            Number of the last wave cleared (0 before the first).
	 * @return The wave whose clear is the next checkpoint.
	 */
	public static int nextCheckpoint(final int clearedWave) {
		int cleared = Math.max(0, clearedWave);
		return (cleared / WAVES_PER_CHECKPOINT + 1) * WAVES_PER_CHECKPOINT;
	}
}
