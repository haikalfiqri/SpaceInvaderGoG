package engine;

import java.util.logging.Logger;

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

	/** Logs what each checkpoint banked. */
	private static final Logger LOGGER = Logger.getLogger(
			EndlessCheckpoint.class.getSimpleName());

	/** Told when a checkpoint banks diamonds, e.g. to show a screen. */
	public interface BankedListener {
		/**
		 * @param clearedWave
		 *            Checkpoint wave that was just cleared.
		 * @param banked
		 *            Diamonds that were banked.
		 */
		void onBanked(int clearedWave, int banked);
	}

	/** Who to tell when diamonds are banked, or null for nobody. */
	private static BankedListener bankedListener;

	/** Banks diamonds after each cleared endless wave. */
	private static final EndlessMode.LevelClearedListener BANKER =
			EndlessCheckpoint::bankAndTell;

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

	/**
	 * Turns checkpoints on for endless runs by hooking into
	 * {@link EndlessMode}. Safe to call before every run: the hook is only
	 * added once.
	 *
	 * @param onBanked
	 *            Told each time diamonds are banked, or null for nobody.
	 */
	public static void install(final BankedListener onBanked) {
		bankedListener = onBanked;
		EndlessMode.addLevelClearedListener(BANKER);
	}

	/**
	 * Banks like {@link #bankIfCheckpoint} and tells the listener how many
	 * diamonds were banked.
	 *
	 * @param state
	 *            State going into the next wave.
	 * @param clearedWave
	 *            Number of the wave just cleared (1-based).
	 * @return State to start the next wave with.
	 */
	private static GameState bankAndTell(final GameState state,
			final int clearedWave) {
		GameState after = bankIfCheckpoint(state, clearedWave);
		int banked = state.getPendingDiamonds() - after.getPendingDiamonds();
		if (banked > 0 && bankedListener != null)
			bankedListener.onBanked(clearedWave, banked);
		return after;
	}

	/**
	 * Banks the run's pending diamonds if the wave just cleared is a
	 * checkpoint. If the wallet could not be saved, the diamonds stay
	 * pending instead of disappearing.
	 *
	 * @param state
	 *            State going into the next wave.
	 * @param clearedWave
	 *            Number of the wave just cleared (1-based).
	 * @return The state with no pending diamonds if they were banked,
	 *         otherwise {@code state} unchanged.
	 */
	public static GameState bankIfCheckpoint(final GameState state,
			final int clearedWave) {
		int pending = state.getPendingDiamonds();
		if (!isCheckpoint(clearedWave) || pending <= 0)
			return state;

		DiamondManager wallet = DiamondManager.getInstance();
		int before = wallet.getDiamonds();
		wallet.addDiamonds(pending);
		if (wallet.getDiamonds() == before) {
			LOGGER.warning("Endless checkpoint at wave " + clearedWave
					+ ": could not bank " + pending + " diamonds.");
			return state;
		}
		LOGGER.info("Endless checkpoint at wave " + clearedWave
				+ ": banked " + pending + " diamonds.");

		return new GameState(state.getLevel(), state.getScore(),
				state.getLivesRemaining(), state.getBulletsShot(),
				state.getShipsDestroyed(), 0,
				state.getLevelsCompletedRun(), state.getCoinsCollected());
	}
}
