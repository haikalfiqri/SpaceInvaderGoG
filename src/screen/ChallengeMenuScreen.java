package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import engine.ChallengeRewards;
import engine.Cooldown;
import engine.Core;
import engine.DailyChallenge;
import engine.ShardSpace;
import engine.WeeklyChallenge;
import skilltree.ShardType;

/**
 * Challenges menu (GOG-102, GOG-44, GOG-49): pick the daily or the weekly
 * challenge, or open Shard Space. Shows the stages of the highlighted
 * challenge and the time until they change.
 *
 * Returns {@link #PLAY_DAILY} or {@link #PLAY_WEEKLY} to start a run,
 * {@link #OPEN_SHARD_SPACE}, or 1 for the title screen.
 */
public class ChallengeMenuScreen extends Screen {

	/** Code that opens this screen from the title menu. */
	public static final int CODE = 12;
	/** Return code: start the daily challenge. */
	public static final int PLAY_DAILY = 13;
	/** Return code: start the weekly challenge. */
	public static final int PLAY_WEEKLY = 14;
	/** Return code: open the Shard Space menu (GOG-49). */
	public static final int OPEN_SHARD_SPACE = 15;

	/** Milliseconds between two menu moves while a key is held. */
	private static final int MOVE_DELAY = 200;
	/** Grey for entries that are not available yet. */
	private static final Color DISABLED = new Color(110, 110, 110);
	/** Grey for secondary text. */
	private static final Color MUTED = new Color(170, 170, 170);

	/** Menu entries. */
	private enum Option {
		/** Play today's daily challenge. */
		DAILY("Daily challenge", true),
		/** Play this week's weekly challenge (GOG-44). */
		WEEKLY("Weekly challenge", true),
		/** Farm shards in the five rarity modes (GOG-49). */
		SHARD_SPACE("Shard Space", true),
		/** Back to the title screen. */
		BACK("Back", true);

		/** Text shown in the menu. */
		private final String label;
		/** False if it cannot be picked yet. */
		private final boolean enabled;

		Option(final String label, final boolean enabled) {
			this.label = label;
			this.enabled = enabled;
		}
	}

	/** Entry under the cursor. */
	private Option selected;
	/** Limits how fast the cursor moves while a key is held. */
	private final Cooldown moveCooldown;
	/** Today's daily stages. */
	private final List<DailyChallenge.Stage> dailyStages;
	/** This week's weekly stages. */
	private final List<DailyChallenge.Stage> weeklyStages;
	/** Challenge whose details are shown: the last one highlighted. */
	private Option shown;

	/**
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second.
	 */
	public ChallengeMenuScreen(final int width, final int height,
			final int fps) {
		super(width, height, fps);
		this.returnCode = 1;
		this.selected = Option.DAILY;
		this.moveCooldown = Core.getCooldown(MOVE_DELAY);
		this.moveCooldown.reset();
		this.dailyStages = DailyChallenge.stagesForToday();
		this.weeklyStages = WeeklyChallenge.stagesForThisWeek();
		this.shown = Option.DAILY;
	}

	/**
	 * Starts the action.
	 *
	 * @return {@link #PLAY_DAILY}, or 1 to go back to the title screen.
	 */
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/** Moves the cursor and handles Space and Escape. */
	protected final void update() {
		super.update();
		draw();
		if (!this.inputDelay.checkFinished())
			return;

		if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
			this.returnCode = 1;
			this.isRunning = false;
			return;
		}
		if (this.moveCooldown.checkFinished()) {
			if (inputManager.isKeyDown(KeyEvent.VK_UP)
					|| inputManager.isKeyDown(KeyEvent.VK_W)) {
				move(-1);
				this.moveCooldown.reset();
			} else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
					|| inputManager.isKeyDown(KeyEvent.VK_S)) {
				move(1);
				this.moveCooldown.reset();
			}
		}
		if (inputManager.isKeyDown(KeyEvent.VK_SPACE)
				|| inputManager.isKeyDown(KeyEvent.VK_ENTER)) {
			if (this.selected == Option.DAILY) {
				this.returnCode = PLAY_DAILY;
				this.isRunning = false;
			} else if (this.selected == Option.WEEKLY) {
				this.returnCode = PLAY_WEEKLY;
				this.isRunning = false;
			} else if (this.selected == Option.SHARD_SPACE) {
				this.returnCode = OPEN_SHARD_SPACE;
				this.isRunning = false;
			} else if (this.selected == Option.BACK) {
				this.returnCode = 1;
				this.isRunning = false;
			}
		}
	}

	/**
	 * Moves the cursor, wrapping around at the ends.
	 *
	 * @param direction
	 *            -1 for up, 1 for down.
	 */
	private void move(final int direction) {
		Option[] options = Option.values();
		this.selected = options[Math.floorMod(
				this.selected.ordinal() + direction, options.length)];
		if (this.selected != Option.BACK)
			this.shown = this.selected;
	}

	/** Draws the menu and today's challenge. */
	private void draw() {
		this.drawManager.initDrawing(this);
		this.drawManager.drawScreenTitle(this, "Challenges");

		// Starts higher than before, so the weekly reward fits (GOG-46).
		int y = this.height / 5;
		for (Option option : Option.values()) {
			boolean isSelected = option == this.selected;
			Color color = !option.enabled ? DISABLED
					: isSelected ? Color.GREEN : Color.WHITE;
			String text = isSelected ? "> " + option.label + " <"
					: option.label;
			this.drawManager.drawCenteredRegularString(this, text, y, color);
			y += 30;
		}

		if (this.shown == Option.SHARD_SPACE) {
			drawShardSpace(y + 20);
			this.drawManager.drawKeyHints(this,
					"w/s move, space select, esc back");
			this.drawManager.completeDrawing(this);
			return;
		}
		boolean weekly = this.shown == Option.WEEKLY;
		List<DailyChallenge.Stage> stages = weekly ? this.weeklyStages
				: this.dailyStages;
		y += 20;
		this.drawManager.drawCenteredRegularString(this,
				weekly ? "This week's weekly" : "Today's daily", y,
				Color.GREEN);
		for (int i = 0; i < stages.size(); i++) {
			y += 22;
			this.drawManager.drawCenteredRegularString(this, (i + 1) + ". "
					+ stages.get(i).describe(), y, Color.WHITE);
		}
		y = drawReward(weekly, y + 26);
		y += 28;
		LocalDateTime now = LocalDateTime.now();
		this.drawManager.drawCenteredRegularString(this, weekly
				? "New weekly in " + DailyChallenge.formatCountdown(
						WeeklyChallenge.timeUntilReset(now))
				: "New daily in " + DailyChallenge.formatCountdown(
						DailyChallenge.timeUntilReset(now)), y, MUTED);

		this.drawManager.drawKeyHints(this,
				"w/s move, space select, esc back");
		this.drawManager.completeDrawing(this);
	}

	/**
	 * Draws the Shard Space summary: plays left and open modes.
	 *
	 * @param top
	 *            First line's y.
	 */
	private void drawShardSpace(final int top) {
		ShardSpace progress = ShardSpace.getInstance();
		int y = top;
		this.drawManager.drawCenteredRegularString(this, "Shard Space", y,
				Color.GREEN);
		y += 22;
		this.drawManager.drawCenteredRegularString(this, ShardSpace.WAVES
				+ " waves + boss, one mode per rarity", y, Color.WHITE);
		y += 22;
		int open = 0;
		for (ShardSpace.Mode mode : ShardSpace.Mode.values())
			if (progress.isUnlocked(mode))
				open++;
		this.drawManager.drawCenteredRegularString(this, "Modes open: " + open
				+ " / " + ShardSpace.Mode.values().length, y, Color.WHITE);
		y += 32;
		this.drawManager.drawCenteredRegularString(this, "Plays left today: "
				+ progress.playsLeft(LocalDate.now()) + " / "
				+ ShardSpace.PLAYS_PER_DAY, y, MUTED);
	}

	/**
	 * Draws what clearing the challenge pays (GOG-46), or that this day's
	 * or week's reward was already paid.
	 *
	 * @param weekly
	 *            True for the weekly challenge.
	 * @param top
	 *            First line's y.
	 * @return y of the last line drawn.
	 */
	private int drawReward(final boolean weekly, final int top) {
		ChallengeRewards rewards = ChallengeRewards.getInstance();
		LocalDate today = LocalDate.now();
		if (weekly ? rewards.isWeeklyClaimed(today)
				: rewards.isDailyClaimed(today)) {
			this.drawManager.drawCenteredRegularString(this, weekly
					? "Reward claimed this week" : "Reward claimed today",
					top, MUTED);
			return top;
		}
		int[] reward = weekly ? ChallengeRewards.WEEKLY
				: ChallengeRewards.DAILY;
		List<String> parts = new ArrayList<String>();
		for (ShardType type : ShardType.values())
			if (reward[type.ordinal()] > 0)
				parts.add(reward[type.ordinal()] + " "
						+ type.getDisplayName());
		if (weekly)
			parts.add(Math.round(ChallengeRewards.WEEKLY_MYTHIC_CHANCE * 100)
					+ "% +1 Mythic");
		// Three per line, so the weekly reward fits the screen.
		int y = top;
		for (int i = 0; i < parts.size(); i += 3) {
			this.drawManager.drawCenteredRegularString(this,
					(i == 0 ? "Reward: " : "") + String.join(", ",
							parts.subList(i, Math.min(parts.size(), i + 3))),
					y, Color.GREEN);
			y += 22;
		}
		return y - 22;
	}
}