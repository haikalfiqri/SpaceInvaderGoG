package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;

import engine.Cooldown;
import engine.Core;
import engine.DailyChallenge;
import engine.ShardSpace;
import engine.ShardSpace.Mode;
import skilltree.ShardType;

/**
 * Shard Space menu (GOG-49): pick one of the five rarity modes. Shows
 * which modes are open, the clears, the reward and today's plays.
 *
 * Returns {@link #PLAY} with {@link #getChosenMode()} set to start a run,
 * or {@link ChallengeMenuScreen#CODE} to go back.
 */
public class ShardSpaceScreen extends Screen {

	/** Return code: start the chosen mode. */
	public static final int PLAY = 16;

	/** Milliseconds between two menu moves while a key is held. */
	private static final int MOVE_DELAY = 200;
	/** Grey for locked modes. */
	private static final Color DISABLED = new Color(110, 110, 110);
	/** Grey for secondary text. */
	private static final Color MUTED = new Color(170, 170, 170);

	/** The player's Shard Space progress. */
	private final ShardSpace progress;
	/** Row under the cursor: a mode's ordinal, or the Back row after them. */
	private int selected;
	/** Limits how fast the cursor moves while a key is held. */
	private final Cooldown moveCooldown;
	/** Mode picked, or null. */
	private Mode chosen;
	/** Message after Space on a mode that can't start, or null. */
	private String warning;

	/**
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second.
	 * @param progress
	 *            The player's Shard Space progress.
	 */
	public ShardSpaceScreen(final int width, final int height, final int fps,
			final ShardSpace progress) {
		super(width, height, fps);
		this.progress = progress;
		this.returnCode = ChallengeMenuScreen.CODE;
		this.moveCooldown = Core.getCooldown(MOVE_DELAY);
		this.moveCooldown.reset();
		// Start on the hardest open mode.
		for (Mode mode : Mode.values())
			if (progress.isUnlocked(mode))
				this.selected = mode.ordinal();
	}

	/**
	 * Starts the action.
	 *
	 * @return {@link #PLAY}, or {@link ChallengeMenuScreen#CODE} for back.
	 */
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/** @return Mode picked, or null if the player went back. */
	public final Mode getChosenMode() {
		return this.chosen;
	}

	/** Moves the cursor and handles Space and Escape. */
	protected final void update() {
		super.update();
		draw();
		if (!this.inputDelay.checkFinished())
			return;

		if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
			this.isRunning = false;
			return;
		}
		int rows = Mode.values().length + 1;
		if (this.moveCooldown.checkFinished()) {
			int move = inputManager.isKeyDown(KeyEvent.VK_UP)
					|| inputManager.isKeyDown(KeyEvent.VK_W) ? -1
					: inputManager.isKeyDown(KeyEvent.VK_DOWN)
							|| inputManager.isKeyDown(KeyEvent.VK_S) ? 1 : 0;
			if (move != 0) {
				this.selected = Math.floorMod(this.selected + move, rows);
				this.warning = null;
				this.moveCooldown.reset();
			}
		}
		if (inputManager.isKeyDown(KeyEvent.VK_SPACE)
				|| inputManager.isKeyDown(KeyEvent.VK_ENTER)) {
			if (this.selected == Mode.values().length) {
				this.isRunning = false;
				return;
			}
			Mode mode = Mode.values()[this.selected];
			this.warning = this.progress.startProblem(mode, LocalDate.now());
			if (this.warning == null) {
				this.chosen = mode;
				this.returnCode = PLAY;
				this.isRunning = false;
			}
		}
	}

	/** Draws the modes and the details of the highlighted one. */
	private void draw() {
		this.drawManager.initDrawing(this);
		this.drawManager.drawScreenTitle(this, "Shard Space");

		LocalDate today = LocalDate.now();
		int y = this.height / 5;
		this.drawManager.drawCenteredRegularString(this, "Plays left today: "
				+ this.progress.playsLeft(today) + " / "
				+ ShardSpace.PLAYS_PER_DAY, y, Color.WHITE);
		y += 36;
		for (Mode mode : Mode.values()) {
			boolean open = this.progress.isUnlocked(mode);
			boolean isSelected = mode.ordinal() == this.selected;
			String text = open ? mode.getTitle() : mode.getTitle() + " (locked)";
			this.drawManager.drawCenteredRegularString(this,
					isSelected ? "> " + text + " <" : text, y,
					!open ? DISABLED
							: isSelected ? mode.getRarity().getColor()
									: Color.WHITE);
			y += 28;
		}
		boolean back = this.selected == Mode.values().length;
		this.drawManager.drawCenteredRegularString(this,
				back ? "> Back <" : "Back", y, back ? Color.GREEN : Color.WHITE);

		y += 44;
		if (!back)
			y = drawDetails(Mode.values()[this.selected], y);
		if (this.progress.playsLeft(today) <= 0)
			this.drawManager.drawCenteredRegularString(this, "New plays in "
					+ DailyChallenge.formatCountdown(DailyChallenge
							.timeUntilReset(LocalDateTime.now())), y, MUTED);
		if (this.warning != null)
			this.drawManager.drawCenteredRegularString(this, this.warning,
					y + 26, Color.RED);

		this.drawManager.drawKeyHints(this,
				"w/s move, space select, esc back");
		this.drawManager.completeDrawing(this);
	}

	/**
	 * Draws the run, reward and unlock state of a mode.
	 *
	 * @return The next free y.
	 */
	private int drawDetails(final Mode mode, final int top) {
		int y = top;
		// Shards come only from a full clear; coins drop as usual.
		this.drawManager.drawCenteredRegularString(this, "Clear "
				+ ShardSpace.WAVES + " waves + boss for:", y, Color.WHITE);
		y += 24;
		// Three rarities per line, so Mythic's five fit the screen.
		String[] parts = rewardText(mode).split(", ");
		for (int i = 0; i < parts.length; i += 3) {
			String line = String.join(", ", java.util.Arrays.copyOfRange(
					parts, i, Math.min(parts.length, i + 3)));
			this.drawManager.drawCenteredRegularString(this,
					(i == 0 ? "Reward: " : "") + line, y, Color.GREEN);
			y += 24;
		}
		Mode previous = mode.previous();
		String state = this.progress.isUnlocked(mode)
				? "Cleared " + this.progress.clears(mode) + " times"
				: "Clear " + previous.getTitle() + " "
						+ this.progress.clearsToUnlock(mode) + " more times";
		this.drawManager.drawCenteredRegularString(this, state, y, MUTED);
		return y + 30;
	}

	/**
	 * @return Reward range, e.g. "10-14 Common" or "6 Common, 2 Rare".
	 */
	private static String rewardText(final Mode mode) {
		int[] min = mode.getMinReward();
		int[] max = mode.getMaxReward();
		StringBuilder text = new StringBuilder();
		for (ShardType type : ShardType.values()) {
			int i = type.ordinal();
			if (max[i] <= 0)
				continue;
			if (text.length() > 0)
				text.append(", ");
			text.append(min[i]);
			if (max[i] != min[i])
				text.append('-').append(max[i]);
			text.append(' ').append(type.getDisplayName());
		}
		return text.toString();
	}
}
