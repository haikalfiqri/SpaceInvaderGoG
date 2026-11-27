package engine;

import skilltree.ShardType;
import skilltree.ShardWallet;
import skilltree.SkillEffects;
import skilltree.SkillSalvage;

/**
 * Shard rewards of the GOG-32 modes: Shard Space (GOG-48), the daily and
 * the weekly challenge (GOG-46). An amount is an int[] with one number per
 * rarity, in {@link ShardType} order (Common first, Mythic last).
 *
 * Every reward goes through {@link #pay}, so the Shard Hunter node
 * (+10%, GOG-89) is applied the same way everywhere.
 */
public final class ShardReward {

	/** Not used. Static class. */
	private ShardReward() {
	}

	/**
	 * @param common
	 *            Common shards.
	 * @param rare
	 *            Rare shards.
	 * @param epic
	 *            Epic shards.
	 * @param legendary
	 *            Legendary shards.
	 * @param mythic
	 *            Mythic shards.
	 * @return The amounts as an array in {@link ShardType} order.
	 */
	public static int[] of(final int common, final int rare, final int epic,
			final int legendary, final int mythic) {
		return new int[] { common, rare, epic, legendary, mythic };
	}

	/**
	 * @return The reward factor of the ship the player flies: 1.1 with
	 *         Shard Hunter, else 1.0.
	 */
	public static double playerFactor() {
		return SkillSalvage.shardRewardFactor(
				SkillEffects.forShip(Core.getSelectedShipType()));
	}

	/**
	 * Applies a reward factor, rounding each rarity to the nearest whole
	 * shard (10 x 1.1 = 11, 3 x 1.1 = 3).
	 *
	 * @param amounts
	 *            Shards per rarity.
	 * @param factor
	 *            1.0 for none, 1.1 for Shard Hunter.
	 * @return New array with the scaled amounts.
	 */
	public static int[] scale(final int[] amounts, final double factor) {
		int[] scaled = new int[amounts.length];
		for (int i = 0; i < amounts.length; i++)
			scaled[i] = (int) Math.round(amounts[i] * factor);
		return scaled;
	}

	/**
	 * Scales a reward and adds it to the wallet in one save.
	 *
	 * @param wallet
	 *            Wallet to pay into.
	 * @param amounts
	 *            Shards per rarity before the factor.
	 * @param factor
	 *            1.0 for none, 1.1 for Shard Hunter.
	 * @return What was paid, or null if the wallet could not be saved
	 *         (then nothing was paid).
	 */
	public static int[] pay(final ShardWallet wallet, final int[] amounts,
			final double factor) {
		int[] paid = scale(amounts, factor);
		return wallet.apply(paid) ? paid : null;
	}

	/**
	 * @param amounts
	 *            Shards per rarity.
	 * @return Text for menus and results, e.g. "+11 Common, +3 Rare shards",
	 *         or "No shards" if every amount is 0.
	 */
	public static String describe(final int[] amounts) {
		StringBuilder text = new StringBuilder();
		for (ShardType type : ShardType.values()) {
			int amount = amounts[type.ordinal()];
			if (amount <= 0)
				continue;
			if (text.length() > 0)
				text.append(", ");
			text.append('+').append(amount).append(' ')
					.append(type.getDisplayName());
		}
		return text.length() == 0 ? "No shards"
				: text.append(" shards").toString();
	}
}
