package engine;

import entity.EnemyShipFormation;

/**
 * The small Shard Space enemies (GOG-49 hook, enemies by Ammar).
 *
 * GameScreen calls {@link #createFormation} on each wave of a Shard Space
 * run. While it returns null, the story formation of the wave's level is
 * used, so the mode is playable before the enemies are done.
 */
public final class ShardSpaceEnemies {

	/** Not used. Static class. */
	private ShardSpaceEnemies() {
	}

	/**
	 * @param settings
	 *            Settings of the wave (size, speed, fire rate).
	 * @param mode
	 *            Mode being played, 0 = Common to 4 = Mythic.
	 * @return The wave's formation, or null for the story formation.
	 *         GameScreen attaches it, as with the story formations.
	 */
	public static EnemyShipFormation createFormation(
			final GameSettings settings, final ShardSpace.Mode mode) {
		// Ammar (GOG-32): return a formation of the Shard Space enemies.
		return null;
	}
}
