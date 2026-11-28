package engine;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * Backgrounds of the Shard Space modes (GOG-49 hook, art by Wafi).
 *
 * Each mode looks for res/backgrounds/shard_space/&lt;rarity&gt;.png, e.g.
 * common.png or mythic.png, 448 x 520 like the story backgrounds. A mode
 * without its image uses the story background of its act, so the game
 * works before the art is added.
 */
public final class ShardSpaceBackgrounds {

	/** Folder of the images, inside res. */
	static final String FOLDER = "backgrounds/shard_space/";
	/** Images already loaded (null = missing, not tried again). */
	private static final Map<ShardSpace.Mode, BufferedImage> CACHE =
			new EnumMap<ShardSpace.Mode, BufferedImage>(ShardSpace.Mode.class);
	/** Logger. */
	private static final Logger LOGGER = Logger.getLogger(
			ShardSpaceBackgrounds.class.getName());

	/** Not used. Static class. */
	private ShardSpaceBackgrounds() {
	}

	/**
	 * @param mode
	 *            Shard Space mode.
	 * @return File name of the mode's image, e.g. "epic.png".
	 */
	public static String fileFor(final ShardSpace.Mode mode) {
		return mode.name().toLowerCase() + ".png";
	}

	/**
	 * @param mode
	 *            Shard Space mode.
	 * @return The mode's background, or null if it has none yet (then the
	 *         story background of its act is used).
	 */
	public static synchronized BufferedImage forMode(
			final ShardSpace.Mode mode) {
		if (!CACHE.containsKey(mode))
			CACHE.put(mode, load(fileFor(mode)));
		return CACHE.get(mode);
	}

	private static BufferedImage load(final String file) {
		try (InputStream in = ShardSpaceBackgrounds.class.getClassLoader()
				.getResourceAsStream(FOLDER + file)) {
			if (in != null)
				return ImageIO.read(in);
			File onDisk = new File("res", FOLDER + file);
			return onDisk.isFile() ? ImageIO.read(onDisk) : null;
		} catch (IOException | RuntimeException e) {
			LOGGER.warning("Could not load Shard Space background " + file
					+ ": " + e);
			return null;
		}
	}
}
