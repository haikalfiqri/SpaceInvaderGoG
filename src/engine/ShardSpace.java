package engine;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

import skilltree.SaveFile;
import skilltree.ShardType;
import skilltree.ShardWallet;

/**
 * Shard Space (GOG-48): the mode that farms shards. There is one mode per
 * shard rarity. A run is {@link #WAVES} enemy waves and then the Shard
 * Space boss.
 *
 * <ul>
 * <li>Common is open from the start. Each next mode opens after
 * {@link #CLEARS_TO_UNLOCK} clears of the one before, so Mythic takes at
 * least 4 days.</li>
 * <li>{@link #PLAYS_PER_DAY} plays per day for all modes together, on
 * their own counter (not stamina). A play is used when the run starts,
 * so quitting does not give it back. There is no way to buy more.</li>
 * <li>Every clear pays the mode's reward (random inside its range), with
 * Shard Hunter applied by {@link ShardReward}.</li>
 * </ul>
 *
 * Saved per profile in the "shard-space" file. The boss, small enemies
 * and backgrounds are filled in by GOG-49's hooks; until then the story
 * ones of the mode's act are used.
 */
public final class ShardSpace {

	/** Enemy waves before the boss. */
	public static final int WAVES = 3;
	/** Clears of a mode needed to open the next one. */
	public static final int CLEARS_TO_UNLOCK = 5;
	/** Plays per day, all modes together. */
	public static final int PLAYS_PER_DAY = 5;
	/** Save file name. */
	private static final String FILE_NAME = "shard-space";

	/** Mode being played, or null outside Shard Space. */
	private static Mode active;
	/** Shared instance. */
	private static ShardSpace instance;

	/** One Shard Space mode per rarity, easiest first. */
	public enum Mode {
		/** Act 1 waves, 10-14 Common. */
		COMMON(ShardType.COMMON, 1, new int[] { 3, 6, 9 },
				ShardReward.of(10, 0, 0, 0, 0),
				ShardReward.of(14, 0, 0, 0, 0)),
		/** Act 2 waves, 4 Common and 3-4 Rare. */
		RARE(ShardType.RARE, 2, new int[] { 3, 6, 9 },
				ShardReward.of(4, 3, 0, 0, 0),
				ShardReward.of(4, 4, 0, 0, 0)),
		/** Act 3 waves, 6 Common, 2 Rare and 1-2 Epic. */
		EPIC(ShardType.EPIC, 3, new int[] { 3, 6, 9 },
				ShardReward.of(6, 2, 1, 0, 0),
				ShardReward.of(6, 2, 2, 0, 0)),
		/** Act 4 waves, 8 Common, 3 Rare, 2 Epic and 1 Legendary. */
		LEGENDARY(ShardType.LEGENDARY, 4, new int[] { 3, 6, 9 },
				ShardReward.of(8, 3, 2, 1, 0),
				ShardReward.of(8, 3, 2, 1, 0)),
		/** The hardest act 4 waves, one of every rarity and more. */
		MYTHIC(ShardType.MYTHIC, 4, new int[] { 7, 8, 9 },
				ShardReward.of(10, 4, 2, 1, 1),
				ShardReward.of(10, 4, 2, 1, 1));

		/** Rarity the mode is named after. */
		private final ShardType rarity;
		/** Story act the waves and placeholder boss come from. */
		private final int act;
		/** Levels inside the act used for the waves. */
		private final int[] waveLevels;
		/** Least shards per rarity for a clear. */
		private final int[] minReward;
		/** Most shards per rarity for a clear. */
		private final int[] maxReward;

		Mode(final ShardType rarity, final int act, final int[] waveLevels,
				final int[] minReward, final int[] maxReward) {
			this.rarity = rarity;
			this.act = act;
			this.waveLevels = waveLevels;
			this.minReward = minReward;
			this.maxReward = maxReward;
		}

		/** @return Rarity the mode is named after. */
		public ShardType getRarity() {
			return this.rarity;
		}

		/** @return Story act the waves come from, 1 to 4. */
		public int getAct() {
			return this.act;
		}

		/** @return Name for menus, e.g. "Epic Shard Space". */
		public String getTitle() {
			return this.rarity.getDisplayName() + " Shard Space";
		}

		/** @return The mode before this one, or null for Common. */
		public Mode previous() {
			return ordinal() == 0 ? null : values()[ordinal() - 1];
		}

		/** @return Least shards per rarity for a clear (a copy). */
		public int[] getMinReward() {
			return this.minReward.clone();
		}

		/** @return Most shards per rarity for a clear (a copy). */
		public int[] getMaxReward() {
			return this.maxReward.clone();
		}
	}

	/** Clears per mode. */
	private final Map<Mode, Integer> clears =
			new EnumMap<Mode, Integer>(Mode.class);
	/** Day the plays counter belongs to, or null if never played. */
	private LocalDate playsDay;
	/** Plays used on playsDay. */
	private int playsUsed;
	/** Where the progress is saved. */
	private final File file;
	/** Game logger. */
	private final Logger logger = Logger.getLogger(ShardSpace.class
			.getName());

	private ShardSpace(final File file) {
		this.file = file;
		Properties saved = SaveFile.read(file);
		for (Mode mode : Mode.values())
			this.clears.put(mode, SaveFile.count(saved, clearsKey(mode)));
		this.playsUsed = Math.min(PLAYS_PER_DAY,
				SaveFile.count(saved, "plays.used"));
		try {
			String day = saved.getProperty("plays.day");
			this.playsDay = day == null ? null : LocalDate.parse(day.trim());
		} catch (DateTimeParseException e) {
			this.playsDay = null;
		}
	}

	/** @return The player's Shard Space progress, saved per profile. */
	public static synchronized ShardSpace getInstance() {
		if (instance == null)
			instance = new ShardSpace(SaveFile.locate(FILE_NAME));
		return instance;
	}

	/**
	 * Progress saved to a given file, for tests.
	 *
	 * @param file
	 *            Save file.
	 * @return New progress reading and writing that file.
	 */
	public static ShardSpace forFile(final File file) {
		return new ShardSpace(file);
	}

	/**
	 * @param mode
	 *            A mode.
	 * @return The stages of a run: {@link #WAVES} waves of the mode's act,
	 *         then a boss stage (played as the act's boss level, so the
	 *         story boss is the placeholder until GOG-49's hook has one).
	 */
	public static List<DailyChallenge.Stage> stagesFor(final Mode mode) {
		List<DailyChallenge.Stage> stages =
				new ArrayList<DailyChallenge.Stage>();
		for (int levelInAct : mode.waveLevels)
			stages.add(new DailyChallenge.Stage(DailyChallenge.levelOf(
					mode.act, levelInAct), mode.act, false));
		stages.add(new DailyChallenge.Stage(DailyChallenge.levelOf(mode.act,
				StoryMode.LEVELS_PER_CHAPTER), mode.act, true));
		return stages;
	}

	/**
	 * Picks a clear's reward inside the mode's range.
	 *
	 * @param mode
	 *            The mode cleared.
	 * @param random
	 *            Random numbers (a test can pass a seeded one).
	 * @return Shards per rarity, before Shard Hunter.
	 */
	public static int[] rollReward(final Mode mode, final Random random) {
		int[] reward = new int[mode.minReward.length];
		for (int i = 0; i < reward.length; i++)
			reward[i] = mode.minReward[i] + random.nextInt(
					mode.maxReward[i] - mode.minReward[i] + 1);
		return reward;
	}

	/** @return True while a Shard Space run is played. */
	public static boolean isRunning() {
		return active != null;
	}

	/** @return Mode being played, or null outside Shard Space. */
	public static Mode activeMode() {
		return active;
	}

	/**
	 * Marks a Shard Space run as started or finished. Core calls this
	 * around the run, so GameScreen can use the Shard Space boss, enemies
	 * and background.
	 *
	 * @param mode
	 *            Mode started, or null when the run ends.
	 */
	public static void setActiveMode(final Mode mode) {
		active = mode;
	}

	/**
	 * @param mode
	 *            A mode.
	 * @return How many times it was cleared.
	 */
	public int clears(final Mode mode) {
		return this.clears.get(mode);
	}

	/**
	 * @param mode
	 *            A mode.
	 * @return True if the player can pick it.
	 */
	public boolean isUnlocked(final Mode mode) {
		return clearsToUnlock(mode) == 0;
	}

	/**
	 * @param mode
	 *            A mode.
	 * @return Clears of the previous mode still needed to open it, 0 if
	 *         it is open.
	 */
	public int clearsToUnlock(final Mode mode) {
		Mode previous = mode.previous();
		return previous == null ? 0
				: Math.max(0, CLEARS_TO_UNLOCK - clears(previous));
	}

	/**
	 * @param day
	 *            Today.
	 * @return Plays left today, 0 to {@link #PLAYS_PER_DAY}.
	 */
	public int playsLeft(final LocalDate day) {
		return day.equals(this.playsDay) ? PLAYS_PER_DAY - this.playsUsed
				: PLAYS_PER_DAY;
	}

	/**
	 * Why a mode can't start now, for the Shard Space menu (GOG-49).
	 *
	 * @param mode
	 *            Mode the player picked.
	 * @param day
	 *            Today.
	 * @return "Locked", "No plays left today", or null if it can start.
	 */
	public String startProblem(final Mode mode, final LocalDate day) {
		if (!isUnlocked(mode))
			return "Locked";
		if (playsLeft(day) <= 0)
			return "No plays left today";
		return null;
	}
	/**
	 * Uses one play to start a run.
	 *
	 * @param mode
	 *            Mode to play.
	 * @param day
	 *            Today.
	 * @return False (and nothing changes) if the mode is locked, no play
	 *         is left today, or the progress could not be saved.
	 */
	public boolean startPlay(final Mode mode, final LocalDate day) {
		if (!isUnlocked(mode) || playsLeft(day) <= 0)
			return false;
		LocalDate oldDay = this.playsDay;
		int oldUsed = this.playsUsed;
		this.playsUsed = day.equals(this.playsDay) ? this.playsUsed + 1 : 1;
		this.playsDay = day;
		if (!save()) {
			this.playsDay = oldDay;
			this.playsUsed = oldUsed;
			return false;
		}
		return true;
	}

	/**
	 * Counts a clear and pays its reward.
	 *
	 * @param mode
	 *            Mode cleared.
	 * @param wallet
	 *            Wallet to pay into.
	 * @param factor
	 *            1.0, or 1.1 with Shard Hunter.
	 * @param random
	 *            Random numbers for the reward.
	 * @return Text for the results screen, e.g. "+11 Common shards".
	 */
	public String recordClear(final Mode mode, final ShardWallet wallet,
			final double factor, final Random random) {
		this.clears.put(mode, (int) Math.min(Integer.MAX_VALUE,
				clears(mode) + 1L));
		if (!save())
			this.logger.warning("Couldn't save Shard Space clears.");
		int[] paid = ShardReward.pay(wallet, rollReward(mode, random),
				factor);
		return paid == null ? "Couldn't save the shards"
				: ShardReward.describe(paid);
	}

	/**
	 * Ends a run (GOG-49): only a full clear (every wave and the boss)
	 * counts and pays. A failed or quit run pays nothing, and its play
	 * stays used.
	 *
	 * @param mode
	 *            Mode played.
	 * @param stagesCleared
	 *            Stages the player cleared.
	 * @param stageCount
	 *            Stages in the run.
	 * @param wallet
	 *            Wallet to pay into.
	 * @param factor
	 *            1.0, or 1.1 with Shard Hunter.
	 * @param random
	 *            Random numbers for the reward.
	 * @return Reward text for the results screen, or null if the run was
	 *         not fully cleared.
	 */
	public String finishRun(final Mode mode, final int stagesCleared,
			final int stageCount, final ShardWallet wallet,
			final double factor, final Random random) {
		if (stagesCleared < stageCount)
			return null;
		return recordClear(mode, wallet, factor, random);
	}

	private boolean save() {
		Properties values = new Properties();
		for (Mode mode : Mode.values())
			values.setProperty(clearsKey(mode),
					Integer.toString(clears(mode)));
		if (this.playsDay != null) {
			values.setProperty("plays.day", this.playsDay.toString());
			values.setProperty("plays.used", Integer.toString(this.playsUsed));
		}
		return SaveFile.write(this.file, values,
				"Shard Space clears and plays (GOG-48)");
	}

	private static String clearsKey(final Mode mode) {
		return "clears." + mode.name().toLowerCase();
	}
}
