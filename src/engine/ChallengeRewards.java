package engine;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.Random;

import skilltree.SaveFile;
import skilltree.ShardType;
import skilltree.ShardWallet;

/**
 * Shard rewards of the daily and weekly challenges (GOG-46): paid once per
 * day and once per week (the week starts on Sunday). Replaying a cleared
 * challenge is allowed, it just pays nothing until the next reset.
 *
 * Saved per profile in the "challenge-claims" file: the last day and the
 * last week whose reward was paid. Shard Hunter is applied by
 * {@link ShardReward}.
 */
public final class ChallengeRewards {

	/** Daily reward: 10 Common, 3 Rare, 1 Epic. */
	public static final int[] DAILY = ShardReward.of(10, 3, 1, 0, 0);
	/** Weekly reward: 25 Common, 8 Rare, 3 Epic, 1 Legendary. */
	public static final int[] WEEKLY = ShardReward.of(25, 8, 3, 1, 0);
	/** Chance of 1 extra Mythic shard with the weekly reward. */
	public static final double WEEKLY_MYTHIC_CHANCE = 0.2;
	/** Save file name. */
	private static final String FILE_NAME = "challenge-claims";

	/** Shared instance. */
	private static ChallengeRewards instance;

	/** Where the claims are saved. */
	private final File file;
	/** Last day whose daily reward was paid, or null. */
	private LocalDate dailyClaimed;
	/** Start (Sunday) of the last week whose reward was paid, or null. */
	private LocalDate weeklyClaimed;

	private ChallengeRewards(final File file) {
		this.file = file;
		Properties saved = SaveFile.read(file);
		this.dailyClaimed = date(saved, "daily");
		this.weeklyClaimed = date(saved, "weekly");
	}

	/** @return The player's claims, saved per profile. */
	public static synchronized ChallengeRewards getInstance() {
		if (instance == null)
			instance = new ChallengeRewards(SaveFile.locate(FILE_NAME));
		return instance;
	}

	/**
	 * Claims saved to a given file, for tests.
	 *
	 * @param file
	 *            Save file.
	 * @return New claims reading and writing that file.
	 */
	public static ChallengeRewards forFile(final File file) {
		return new ChallengeRewards(file);
	}

	/**
	 * Connects the rewards to the daily and weekly challenges. Safe to
	 * call more than once.
	 */
	public static void install() {
		DailyChallenge.setRewardHandler(day -> getInstance().claimDaily(day,
				ShardWallet.getInstance(), ShardReward.playerFactor()));
		WeeklyChallenge.setRewardHandler(week -> getInstance().claimWeekly(
				week, ShardWallet.getInstance(), ShardReward.playerFactor(),
				new Random()));
	}

	/**
	 * @param day
	 *            A day.
	 * @return True if that day's daily reward was already paid.
	 */
	public boolean isDailyClaimed(final LocalDate day) {
		return day.equals(this.dailyClaimed);
	}

	/**
	 * @param day
	 *            Any day of the week.
	 * @return True if that week's weekly reward was already paid.
	 */
	public boolean isWeeklyClaimed(final LocalDate day) {
		return WeeklyChallenge.weekStart(day).equals(this.weeklyClaimed);
	}

	/**
	 * Pays the daily reward, once per day.
	 *
	 * @param day
	 *            Day of the cleared challenge.
	 * @param wallet
	 *            Wallet to pay into.
	 * @param factor
	 *            1.0, or 1.1 with Shard Hunter.
	 * @return Text for the results screen, or null if already paid today.
	 */
	public String claimDaily(final LocalDate day, final ShardWallet wallet,
			final double factor) {
		if (isDailyClaimed(day))
			return null;
		LocalDate before = this.dailyClaimed;
		this.dailyClaimed = day;
		return payOrUndo(wallet, DAILY, factor, () -> this.dailyClaimed = before);
	}

	/**
	 * Pays the weekly reward, once per week.
	 *
	 * @param day
	 *            Any day of the cleared week.
	 * @param wallet
	 *            Wallet to pay into.
	 * @param factor
	 *            1.0, or 1.1 with Shard Hunter.
	 * @param random
	 *            Rolls the Mythic bonus chance.
	 * @return Text for the results screen, or null if already paid this
	 *         week.
	 */
	public String claimWeekly(final LocalDate day, final ShardWallet wallet,
			final double factor, final Random random) {
		if (isWeeklyClaimed(day))
			return null;
		int[] reward = WEEKLY.clone();
		if (random.nextDouble() < WEEKLY_MYTHIC_CHANCE)
			reward[ShardType.MYTHIC.ordinal()] = 1;
		LocalDate before = this.weeklyClaimed;
		this.weeklyClaimed = WeeklyChallenge.weekStart(day);
		return payOrUndo(wallet, reward, factor,
				() -> this.weeklyClaimed = before);
	}

	/**
	 * Saves the claim first, then pays, so a crash can't pay twice. If
	 * either fails, the claim is undone and nothing is paid.
	 */
	private String payOrUndo(final ShardWallet wallet, final int[] reward,
			final double factor, final Runnable undo) {
		if (!save()) {
			undo.run();
			return "Couldn't save the reward, try again";
		}
		int[] paid = ShardReward.pay(wallet, reward, factor);
		if (paid == null) {
			undo.run();
			save();
			return "Couldn't save the shards, try again";
		}
		return ShardReward.describe(paid);
	}

	private boolean save() {
		Properties values = new Properties();
		if (this.dailyClaimed != null)
			values.setProperty("daily", this.dailyClaimed.toString());
		if (this.weeklyClaimed != null)
			values.setProperty("weekly", this.weeklyClaimed.toString());
		return SaveFile.write(this.file, values,
				"Daily and weekly rewards claimed (GOG-46)");
	}

	private static LocalDate date(final Properties saved, final String key) {
		try {
			String value = saved.getProperty(key);
			return value == null ? null : LocalDate.parse(value.trim());
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
