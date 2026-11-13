package screen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Twinkling pixel-art stars over a dark nebula, behind the main menu.
 * The stars stay in place; each one swells from a dot into a small cross or
 * a big sparkle and shrinks back, out of step with its neighbours.
 */
public class Starfield {

	/** Number of stars on screen. */
	private static final int STAR_COUNT = 55;
	/** Longest arm, in pixels, of a dot, a small star and a big sparkle. */
	private static final int[] MAX_ARM = { 0, 2, 5 };
	/** Share of the stars that are dots, small stars and big sparkles. */
	private static final int[] KIND_WEIGHT = { 55, 30, 15 };
	/** Frames of the update loop in which a star keeps one pixel size. */
	private static final int TICKS_PER_STEP = 4;
	/** Shortest and longest twinkle cycle, in updates. */
	private static final int MIN_PERIOD = 90;
	private static final int MAX_PERIOD = 200;
	/** Nebula is painted this many times smaller, then enlarged blocky. */
	private static final int NEBULA_PIXEL = 6;
	/** Size of one star pixel on screen, so the sparkles are easy to see. */
	private static final int STAR_PIXEL = 2;
	private static final Color BRIGHT = new Color(255, 255, 255);
	private static final Color DIM = new Color(150, 150, 170);

	/** Horizontal position of each star. */
	private final int[] x;
	/** Vertical position of each star. */
	private final int[] y;
	/** 0 dot, 1 small star, 2 big sparkle. */
	private final int[] kind;
	/** Offset of each star in its own twinkle cycle. */
	private final int[] phase;
	/** Length of each star's twinkle cycle. */
	private final int[] period;
	/** Dark purple clouds, painted once. */
	private final BufferedImage nebula;
	/** Updates since the screen opened. */
	private int tick;
	/** Planets drifting in the background; empty if they failed to load. */
	private final java.util.List<Planet> planets =
			new java.util.ArrayList<Planet>();

	/**
	 * Scatters the stars over the screen and paints the nebula.
	 *
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 */
	public Starfield(final int width, final int height) {
		this.x = new int[STAR_COUNT];
		this.y = new int[STAR_COUNT];
		this.kind = new int[STAR_COUNT];
		this.phase = new int[STAR_COUNT];
		this.period = new int[STAR_COUNT];

		Random random = new Random();
		int totalWeight = 0;
		for (int w : KIND_WEIGHT)
			totalWeight += w;
		for (int i = 0; i < STAR_COUNT; i++) {
			this.x[i] = random.nextInt(width);
			this.y[i] = random.nextInt(height);
			int roll = random.nextInt(totalWeight);
			int k = 0;
			while (roll >= KIND_WEIGHT[k]) {
				roll -= KIND_WEIGHT[k];
				k++;
			}
			this.kind[i] = k;
			this.period[i] = MIN_PERIOD
					+ random.nextInt(MAX_PERIOD - MIN_PERIOD);
			this.phase[i] = random.nextInt(this.period[i]);
		}
		this.nebula = paintNebula(width, height, random);

		// Two planets in uneven spots: one tucked in the top-right corner,
		// one half off the bottom-left edge, so the sky looks natural. The
		// numbers are each sheet's frame size, columns, frame count and the
		// time one frame is shown (the clips run at about 14 frames/s).
		Planet earth = Planet.load("planets/planet1.png", 305, 149, 22, 258,
				70, width * 89 / 100, height * 10 / 100);
		if (earth != null)
			this.planets.add(earth);
		Planet gas = Planet.load("planets/planet2.png", 258, 149, 14, 86, 70,
				width * 7 / 100, height * 85 / 100);
		if (gas != null)
			this.planets.add(gas);
	}

	/** Paints soft dark purple and maroon clouds on a small canvas. */
	private static BufferedImage paintNebula(final int width,
			final int height, final Random random) {
		int w = Math.max(1, width / NEBULA_PIXEL);
		int h = Math.max(1, height / NEBULA_PIXEL);
		BufferedImage image = new BufferedImage(w, h,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(3, 2, 8));
		g.fillRect(0, 0, w, h);
		Color[] clouds = { new Color(14, 8, 34), new Color(9, 6, 22),
				new Color(22, 8, 20), new Color(8, 8, 10) };
		// Each cloud is a short random walk of overlapping blobs, which gives
		// ragged edges instead of one clean ellipse.
		for (int cloud = 0; cloud < 7; cloud++) {
			g.setColor(clouds[random.nextInt(clouds.length)]);
			int cx = random.nextInt(w);
			int cy = random.nextInt(h);
			for (int blob = 0; blob < 14; blob++) {
				int r = w / 10 + random.nextInt(w / 8);
				g.fillOval(cx - r, cy - r * 2 / 3, r * 2, r * 4 / 3);
				cx += random.nextInt(w / 6) - w / 12;
				cy += random.nextInt(h / 10) - h / 20;
			}
		}
		g.dispose();
		return image;
	}

	/**
	 * Advances the twinkle by one frame. Stars do not move.
	 */
	public final void update() {
		this.tick++;
	}

	/** @return Number of stars. */
	public final int getCount() {
		return STAR_COUNT;
	}

	/** @return Planets to draw between the nebula and the stars. */
	public final java.util.List<Planet> getPlanets() {
		return this.planets;
	}

	/** @return Dark nebula to draw behind the stars. */
	public final BufferedImage getNebula() {
		return this.nebula;
	}

	/** @return Size of one star pixel on screen. */
	public final int getStarPixel() {
		return STAR_PIXEL;
	}

	/** @return Size of one nebula pixel on screen. */
	public final int getNebulaPixel() {
		return NEBULA_PIXEL;
	}

	/**
	 * @param i
	 *            Star index.
	 * @return Horizontal position of the star.
	 */
	public final int getX(final int i) {
		return this.x[i];
	}

	/**
	 * @param i
	 *            Star index.
	 * @return Vertical position of the star.
	 */
	public final int getY(final int i) {
		return this.y[i];
	}

	/**
	 * Current arm length of a star, in pixels. 0 means just the centre dot.
	 * It rises and falls in whole pixels, so the sparkle grows in steps like
	 * the pixel art it copies.
	 *
	 * @param i
	 *            Star index.
	 * @return Arm length, from 0 to the star's maximum.
	 */
	public final int getArm(final int i) {
		int step = (this.tick / TICKS_PER_STEP) * TICKS_PER_STEP;
		double t = ((step + this.phase[i]) % this.period[i])
				/ (double) this.period[i];
		double wave = (1 - Math.cos(2 * Math.PI * t)) / 2;
		return (int) Math.round(wave * MAX_ARM[this.kind[i]]);
	}

	/**
	 * @param i
	 *            Star index.
	 * @return Colour of the star; a star at rest is dimmer.
	 */
	public final Color getColor(final int i) {
		return getArm(i) == 0 && this.kind[i] != 0 ? DIM : BRIGHT;
	}
}
