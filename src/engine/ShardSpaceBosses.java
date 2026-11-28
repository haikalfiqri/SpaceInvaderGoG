package engine;

import entity.Boss;

/**
 * The Shard Space boss (GOG-49 hook, boss by Ammar): one boss, stronger
 * in each rarity mode.
 *
 * GameScreen calls {@link #create} on the boss stage of a Shard Space run.
 * While it returns null, the story boss of the mode's act is used, so the
 * mode is playable before the boss is done.
 */
public final class ShardSpaceBosses {

	/** Not used. Static class. */
	private ShardSpaceBosses() {
	}

	/**
	 * @param mode
	 *            Mode being played; use mode.ordinal() (0 = Common, 4 =
	 *            Mythic) to scale health, bullets and speed.
	 * @param screenWidth
	 *            Screen width, to center the boss.
	 * @return The boss for this mode, or null for the story boss.
	 */
	public static Boss create(final ShardSpace.Mode mode,
			final int screenWidth) {
		// Ammar (GOG-32): return new ShardSpaceBoss(mode, ...) here.
		return null;
	}
}
