package screen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import engine.Core;
import engine.Frame;
import engine.StoryBackgrounds;
import entity.Ship;
import entity.Superweapon;

/**
 * GOG-105: isolated visual test for the post-Level-40 superweapon.
 * Run this class instead of engine.Core. No profiles, rewards, or fights.
 */
public final class SuperweaponPreview extends Screen {
    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;
    private static final int FPS = 60;
    private static final int SHIP_ROW_FROM_BOTTOM = 30;

    private Ship ship;
    private BufferedImage background;

    public SuperweaponPreview(final int width, final int height, final int fps) {
        super(width, height, fps);
    }

    public static void main(final String[] args) {
        Frame frame = new Frame(WIDTH, HEIGHT, Core.SCALE);
        frame.setTitle("GoG - Superweapon visual preview");
        try {
            Core.getDrawManager().setFrame(frame);
            frame.setScreen(new SuperweaponPreview(frame.getWidth(),
                    frame.getHeight(), FPS));
        } finally {
            frame.dispose();
        }
    }

    @Override
    public void initialize() {
        ship = new Ship(0, this.height - SHIP_ROW_FROM_BOTTOM);
        ship.setPositionX((this.width - ship.getWidth()) / 2);
        // Use the real Level 40 background, without starting its fight.
        background = StoryBackgrounds.forLevel(40);
    }

    @Override
    protected void update() {
        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            this.isRunning = false;
            return;
        }

        boolean left = inputManager.isKeyDown(KeyEvent.VK_LEFT)
                || inputManager.isKeyDown(KeyEvent.VK_A);
        boolean right = inputManager.isKeyDown(KeyEvent.VK_RIGHT)
                || inputManager.isKeyDown(KeyEvent.VK_D);
        if (left && !right) ship.moveLeft();
        if (right && !left) ship.moveRight();
        ship.setPositionX(Math.max(1, Math.min(this.width - ship.getWidth() - 1,
                ship.getPositionX())));
        ship.update();
        draw();
    }

    private void draw() {
        drawManager.initDrawing(this);
        if (background != null) drawManager.drawBackground(this, background);

        // The only new artwork is the text-based ship. It never moves.
        Superweapon.draw(drawManager.getWorldGraphics(), this.width, Superweapon.TOP_Y);
        drawManager.drawEntity(ship, ship.getPositionX(), ship.getPositionY());

        // Temporary HUD, separate from normal gameplay and boss health.
        Graphics2D g = (Graphics2D) drawManager.getWorldGraphics().create();
        try {
            g.setColor(new Color(13, 12, 21));
            g.fillRect(0, 0, this.width, Superweapon.TOP_Y);
            g.setColor(new Color(110, 48, 55));
            g.drawLine(0, Superweapon.TOP_Y - 1, this.width - 1,
                    Superweapon.TOP_Y - 1);
        } finally {
            g.dispose();
        }
        drawManager.drawRegularString("SUPERWEAPON / ART PREVIEW", 12, 21, Color.WHITE);
        drawManager.drawRegularString("A/D or arrows: move | ESC: exit", 12, 43,
                new Color(182, 179, 196));
        drawManager.drawRegularString("NO COMBAT - VISUAL TEST ONLY", 12, 65,
                new Color(255, 128, 95));
        drawManager.completeDrawing(this);
    }
}
