import engine.Core;
import engine.DailyChallenge;
import engine.FormationTactics;
import engine.GameSettings;
import engine.StoryMode;
import entity.Bullet;
import entity.BulletPool;
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.EnemyType;
import entity.Entity;
import screen.Screen;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** GOG-105: standalone regression checks, following the existing tests/ convention. */
public final class StoryDifficultyTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static GameSettings[] settings(boolean challenge) {
        boolean before = DailyChallenge.isRunning();
        DailyChallenge.setRunning(challenge);
        try {
            GameSettings[] result = new GameSettings[41];
            for (int level = 1; level <= 40; level++) {
                result[level] = StoryMode.getSettings(level);
            }
            return result;
        } finally {
            DailyChallenge.setRunning(before);
        }
    }

    private static void checkSettings() {
        GameSettings[] original = settings(true);
        GameSettings[] story = settings(false);
        double lastRate = 0;
        int lastMovement = 0;
        for (int level = 1; level <= 40; level++) {
            GameSettings a = original[level], b = story[level];
            check(a.getFormationWidth() == b.getFormationWidth(), "Width changed: " + level);
            check(a.getFormationHeight() == b.getFormationHeight(), "Height changed: " + level);
            check(a.getTwoHitEnemyCount() == b.getTwoHitEnemyCount(), "HP changed: " + level);
            check(a.isDiagonalMovement() == b.isDiagonalMovement(), "Diagonal layout changed: " + level);
            for (int row = 0; row < a.getFormationHeight(); row++) {
                check(a.getRowType(row) == b.getRowType(row), "Row type changed: " + level + "/" + row);
            }
            check(a.getFormationTactics() == FormationTactics.CLASSIC,
                    "Challenges must retain their original tactics");
            if (level <= 10 || level % 10 == 0) {
                check(a.getBaseSpeed() == b.getBaseSpeed(), "Act 1 / boss movement changed");
                check(a.getShootingFrecuency() == b.getShootingFrecuency(), "Act 1 / boss firing changed");
                check(b.getFormationTactics() == FormationTactics.CLASSIC, "Act 1 / boss tactics changed");
                lastRate = 0;
                continue;
            }
            FormationTactics tactics = b.getFormationTactics();
            double rate = 1000.0 * tactics.getMaxShooters() / b.getShootingFrecuency();
            check(b.getBaseSpeed() < a.getBaseSpeed(), "New movement should be faster: " + level);
            check(rate > 1000.0 / a.getShootingFrecuency(), "New firing pressure should be higher: " + level);
            check(tactics.getMaxShooters() < b.getFormationWidth(), "Reserve a non-firing column");
            check(b.getShootingFrecuency() > 0, "Positive cooldown");
            if (lastRate > 0) {
                check(rate > lastRate, "Nominal volley pressure must rise within each act: " + level);
                check(b.getBaseSpeed() < lastMovement, "Movement must speed up within each act: " + level);
            }
            lastRate = rate;
            lastMovement = b.getBaseSpeed();
        }
        for (int[] pair : new int[][] {{11,21}, {21,31}, {19,29}, {29,39}}) {
            GameSettings earlier = story[pair[0]], later = story[pair[1]];
            double earlierRate = 1000.0 * earlier.getFormationTactics().getMaxShooters()
                    / earlier.getShootingFrecuency();
            double laterRate = 1000.0 * later.getFormationTactics().getMaxShooters()
                    / later.getShootingFrecuency();
            check(laterRate > earlierRate, "Act starts / peaks should rise");
        }
        GameSettings other = new GameSettings(5, 3, 20, 1000);
        check(other.getFormationTactics() == FormationTactics.CLASSIC,
                "Other GameSettings callers retain legacy tactics");
    }

    private static void checkPatterns() {
        Random random = new Random(105);
        for (FormationTactics.VolleyPattern pattern : FormationTactics.VolleyPattern.values()) {
            for (int max = 1; max <= 4; max++) {
                FormationTactics t = new FormationTactics(max, pattern,
                        FormationTactics.MovementRhythm.STEADY);
                for (int available = 0; available <= 7; available++) {
                    Set<Integer> all = new HashSet<>();
                    for (int volley = 0; volley < 40; volley++) {
                        int[] ids = t.chooseShooters(available, volley, random);
                        int expected = available == 0 ? 0 : Math.min(max, Math.max(1, available - 1));
                        check(ids.length == expected, "Incorrect volley count");
                        Set<Integer> unique = new HashSet<>();
                        for (int id : ids) {
                            check(id >= 0 && id < available, "Index outside shooter list");
                            check(unique.add(id), "Enemy selected twice in one volley");
                            all.add(id);
                        }
                    }
                    if (pattern != FormationTactics.VolleyPattern.RANDOM) {
                        check(all.size() == available, "Every surviving column should get a turn");
                    }
                }
            }
        }
        FormationTactics spread = new FormationTactics(3, FormationTactics.VolleyPattern.SPREAD,
                FormationTactics.MovementRhythm.PULSE);
        check(Arrays.equals(spread.chooseShooters(6, 0, random), new int[] {0, 2, 4}), "Spread shape");
        check(Arrays.equals(spread.chooseShooters(6, 1, random), new int[] {1, 3, 5}), "Spread rotation");
        FormationTactics sweep = new FormationTactics(3, FormationTactics.VolleyPattern.SWEEP,
                FormationTactics.MovementRhythm.SURGE);
        check(Arrays.equals(sweep.chooseShooters(6, 1, random), new int[] {1, 2, 3}), "Sweep shape");
        check(spread.movementInterval(15, 0) == 18 && spread.movementInterval(15, 150) == 12,
                "Pulse movement alternates slower and faster steps");
        check(sweep.movementInterval(11, 100) == 6 && sweep.shouldFeint(90)
                && !sweep.shouldFeint(91) && sweep.shouldFeint(270), "Surge and periodic feint");
        check(FormationTactics.CLASSIC.movementInterval(10, 90) == 10
                && !FormationTactics.CLASSIC.shouldFeint(90), "No legacy movement changes");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Set<Bullet> volley(EnemyShipFormation formation) throws Exception {
        setField(formation, "shootingCooldown", Core.getCooldown(0));
        Set<Bullet> result = new HashSet<>();
        formation.shoot(result);
        return result;
    }

    private static void checkFormation() throws Exception {
        GameSettings settings = StoryMode.getSettings(39);
        EnemyShipFormation formation = new EnemyShipFormation(settings);
        formation.attach(new Screen(448, 520, 60));
        List<EnemyShip> ships = new ArrayList<>();
        for (EnemyShip ship : formation) ships.add(ship);
        check(ships.size() == 35, "Level 39 keeps its original 35 enemies");
        Set<Bullet> shots = volley(formation);
        check(shots.size() == 4, "Level 39 starts with four Comet shooters");
        Set<Integer> xPositions = new HashSet<>();
        for (Bullet shot : shots) xPositions.add(shot.getPositionX());
        check(xPositions.size() == 4, "Four separate enemies fire, not four bullets from one");
        BulletPool.recycle(shots);

        int[] xs = new int[ships.size()], ys = new int[ships.size()];
        for (int i = 0; i < ships.size(); i++) {
            xs[i] = ships.get(i).getPositionX() - ships.get(0).getPositionX();
            ys[i] = ships.get(i).getPositionY() - ships.get(0).getPositionY();
        }
        for (int frame = 0; frame < 600; frame++) {
            formation.update();
            for (int i = 0; i < ships.size(); i++) {
                EnemyShip ship = ships.get(i);
                check(ship.getPositionX() - ships.get(0).getPositionX() == xs[i]
                        && ship.getPositionY() - ships.get(0).getPositionY() == ys[i],
                        "Formation movement must preserve rows and columns");
                check(ship.getPositionX() >= 0 && ship.getPositionX() + ship.getWidth() <= 448
                        && ship.getPositionY() >= 40 && ship.getPositionY() + ship.getHeight() <= 520,
                        "Formation must stay inside the play area");
            }
        }

        FormationTactics tactics = new FormationTactics(2, FormationTactics.VolleyPattern.SPREAD,
                FormationTactics.MovementRhythm.STEADY);
        EnemyShipFormation special = new EnemyShipFormation(new GameSettings(5, 3, 20, 800)
                .withRowTypes(EnemyType.POLAR, EnemyType.JELLY, EnemyType.BEETLE)
                .withFormationTactics(tactics));
        shots = volley(special);
        check(shots.size() == 2, "Beetles still fire one ordinary shot each");
        BulletPool.recycle(shots);
        for (EnemyShip ship : special) {
            if (ship.getType() == EnemyType.BEETLE) special.destroy(ship);
        }
        shots = volley(special);
        check(shots.size() == 4, "Two Jellies still each fire their original double shot");
        BulletPool.recycle(shots);
        for (EnemyShip ship : special) {
            if (ship.getType() == EnemyType.JELLY) special.destroy(ship);
        }
        shots = volley(special);
        check(shots.size() == 2, "Two Polar shooters fire");
        for (Bullet shot : shots) check(shot.isFreezing(), "Polar freeze ability unchanged");
        BulletPool.recycle(shots);
        for (EnemyShip ship : special) {
            if (ship.getType() == EnemyType.POLAR) special.destroy(ship);
        }
        check(volley(special).isEmpty(), "No shooting when all enemies destroyed");

        // All columns empty at once: detached Comets remain alive, but cannot shoot.
        EnemyShipFormation diving = new EnemyShipFormation(new GameSettings(3, 1, 10, 800)
                .withRowTypes(EnemyType.COMET).withFormationTactics(tactics));
        diving.attach(new Screen(448, 520, 60));
        Entity target = new Entity(200, 450, 26, 16, Color.WHITE);
        for (EnemyShip ship : diving) check(diving.startDive(ship, target), "Comet ability unchanged");
        diving.update();
        check(!diving.isEmpty() && diving.isCometDiving(), "Detached enemies still count as alive");
        check(volley(diving).isEmpty(), "No shooter-list crash while all enemies dive");
    }

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);
        checkSettings();
        checkPatterns();
        checkFormation();
        System.out.println("StoryDifficultyTest passed (" + checks + " checks).");
    }
}
