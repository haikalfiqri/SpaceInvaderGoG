package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;

import engine.Core;
import engine.DiamondManager;
import engine.EndlessCheckpoint;

/**
 * Shown between endless waves when a checkpoint banks diamonds (GOG-109):
 * how many were banked, the new balance and the next checkpoint wave.
 * Opened directly by Core, so it needs no MenuItem entry.
 */
public class CheckpointScreen extends Screen {

	/** Milliseconds before the screen can be closed. */
	private static final int CLOSE_DELAY = 600;
	/** Colour of the secondary lines. */
	private static final Color MUTED = new Color(170, 170, 170);

	/** Checkpoint wave that was just cleared. */
	private final int clearedWave;
	/** Diamonds that were banked. */
	private final int banked;
	/** True once Space and Enter are released since the screen opened, so
	 * the Space held for shooting cannot close it by accident. */
	private boolean keysReleased;

	/**
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second.
	 * @param clearedWave
	 *            Checkpoint wave that was just cleared.
	 * @param banked
	 *            Diamonds that were banked.
	 */
	public CheckpointScreen(final int width, final int height, final int fps,
			final int clearedWave, final int banked) {
		super(width, height, fps);
		this.clearedWave = clearedWave;
		this.banked = banked;
		this.keysReleased = false;
		this.inputDelay = Core.getCooldown(CLOSE_DELAY);
		this.inputDelay.reset();
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code (unused: Core opens this screen directly).
	 */
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/** Updates the screen and closes it on Space or Enter. */
	protected final void update() {
		super.update();
		draw();

		boolean confirm = inputManager.isKeyDown(KeyEvent.VK_SPACE)
				|| inputManager.isKeyDown(KeyEvent.VK_ENTER);
		if (!this.keysReleased) {
			this.keysReleased = !confirm;
			return;
		}
		if (this.inputDelay.checkFinished() && confirm)
			this.isRunning = false;
	}

	/** Draws what was banked and the next checkpoint. */
	private void draw() {
		this.drawManager.initDrawing(this);

		this.drawManager.drawScreenTitle(this, "Checkpoint!");
		this.drawManager.drawCenteredRegularString(this, "Wave "
				+ this.clearedWave + " cleared", this.height / 4, MUTED);

		int y = this.height / 3 + 10;
		this.drawManager.drawDiamondBalance(this, this.banked, y);
		this.drawManager.drawCenteredRegularString(this, "diamonds banked",
				y + 25, Color.WHITE);
		this.drawManager.drawCenteredRegularString(this,
				"Safe even if you die", y + 55, Color.GREEN);
		this.drawManager.drawCenteredRegularString(this, "You have "
				+ DiamondManager.getInstance().getDiamonds() + " diamonds",
				y + 85, Color.WHITE);
		this.drawManager.drawCenteredRegularString(this, "Next checkpoint: wave "
				+ EndlessCheckpoint.nextCheckpoint(this.clearedWave),
				y + 115, MUTED);

		this.drawManager.drawKeyHints(this, "space continue");
		this.drawManager.completeDrawing(this);
	}
}
