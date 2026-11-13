package screen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * An animated pixel-art planet for the main menu background. The frames sit
 * in one sprite sheet (res/planets/), read left to right, top to bottom, and
 * play in a loop. The planet is optional: if its sheet cannot be loaded, it
 * is skipped and the menu just has no planet.
 */
public class Planet {

	private static final Logger LOGGER = Logger.getLogger(Planet.class
			.getName());

	/** Sprite sheet with every frame. */
	private final BufferedImage sheet;
	/** Size of one frame, in pixels. */
	private final int frameWidth, frameHeight;
	/** Frames per row of the sheet, and number of frames. */
	private final int columns, frameCount;
	/** Milliseconds each frame stays on screen. */
	private final long frameMillis;
	/** Where the planet's centre sits on the screen. */
	private final int centerX, centerY;
	/** Offset into the loop, so planets do not start in step. */
	private final long startOffsetMillis;

	private Planet(final BufferedImage sheet, final int frameWidth,
			final int frameHeight, final int columns, final int frameCount,
			final long frameMillis, final int centerX, final int centerY) {
		this.sheet = sheet;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		this.columns = columns;
		this.frameCount = frameCount;
		this.frameMillis = frameMillis;
		this.centerX = centerX;
		this.centerY = centerY;
		this.startOffsetMillis = (long) (Math.random() * frameCount
				* frameMillis);
	}

	/**
	 * Loads a planet sheet.
	 *
	 * @param name
	 *            Sheet path relative to res/, e.g. "planets/planet1.png".
	 * @param frameWidth
	 *            Width of one frame.
	 * @param frameHeight
	 *            Height of one frame.
	 * @param columns
	 *            Frames per row of the sheet.
	 * @param frameCount
	 *            Number of frames.
	 * @param frameMillis
	 *            Time each frame is shown.
	 * @param centerX
	 *            Screen position of the planet's centre.
	 * @param centerY
	 *            Screen position of the planet's centre.
	 * @return The planet, or null if the sheet could not be loaded.
	 */
	public static Planet load(final String name, final int frameWidth,
			final int frameHeight, final int columns, final int frameCount,
			final long frameMillis, final int centerX, final int centerY) {
		try {
			InputStream in = Planet.class.getClassLoader()
					.getResourceAsStream(name);
			if (in == null) {
				File file = new File("res", name);
				if (!file.isFile())
					throw new IOException("Planet sheet not found: " + name);
				BufferedImage sheet = ImageIO.read(file);
				return new Planet(sheet, frameWidth, frameHeight, columns,
						frameCount, frameMillis, centerX, centerY);
			}
			try (InputStream stream = in) {
				BufferedImage sheet = ImageIO.read(stream);
				return new Planet(sheet, frameWidth, frameHeight, columns,
						frameCount, frameMillis, centerX, centerY);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.warning("Could not load planet " + name + ": " + e);
			return null;
		}
	}

	/** @return The frame to show now. */
	public final BufferedImage getFrame() {
		long step = (System.currentTimeMillis() + startOffsetMillis)
				/ frameMillis;
		int index = (int) (step % frameCount);
		int column = index % columns;
		int row = index / columns;
		return sheet.getSubimage(column * frameWidth, row * frameHeight,
				frameWidth, frameHeight);
	}

	/** @return Screen x of the frame's left edge. */
	public final int getX() {
		return centerX - frameWidth / 2;
	}

	/** @return Screen y of the frame's top edge. */
	public final int getY() {
		return centerY - frameHeight / 2;
	}
}
