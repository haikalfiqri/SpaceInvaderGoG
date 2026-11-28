package engine;

import java.util.Objects;
import java.util.Random;

/** GOG-105: formation-level tactics; individual enemy abilities are unchanged. */
public final class FormationTactics {
    public enum VolleyPattern {
        RANDOM, SPREAD, SWEEP, MIXED
    }

    public enum MovementRhythm {
        STEADY, PULSE, SURGE
    }

    /** Default for every existing caller, including Act 1, bosses and other modes. */
    public static final FormationTactics CLASSIC = new FormationTactics(
            1, VolleyPattern.RANDOM, MovementRhythm.STEADY);

    private final int maxShooters;
    private final VolleyPattern volleyPattern;
    private final MovementRhythm movementRhythm;

    public FormationTactics(int maxShooters, VolleyPattern volleyPattern,
                            MovementRhythm movementRhythm) {
        if (maxShooters < 1) {
            throw new IllegalArgumentException("Need at least one shooter");
        }
        this.maxShooters = maxShooters;
        this.volleyPattern = Objects.requireNonNull(volleyPattern);
        this.movementRhythm = Objects.requireNonNull(movementRhythm);
    }

    public int getMaxShooters() { return maxShooters; }
    public VolleyPattern getVolleyPattern() { return volleyPattern; }
    public MovementRhythm getMovementRhythm() { return movementRhythm; }

    /**
     * Choose distinct, living front-column shooters, ordered from left to right
     * by the caller. Keep at least one available column out of each volley
     * whenever possible; a lone remaining shooter can still fire.
     *
     * RANDOM: independent sample without replacement.
     * SPREAD: evenly spaced, rotating firing positions.
     * SWEEP: a group of adjacent firing positions travels across the formation.
     * MIXED: alternates between the spread and the sweep.
     */
    public int[] chooseShooters(int available, long volley, Random random) {
        if (available <= 0) {
            return new int[0];
        }
        int count = Math.min(maxShooters, Math.max(1, available - 1));
        int[] selected = new int[count];
        VolleyPattern pattern = volleyPattern;
        long step = volley;
        if (pattern == VolleyPattern.MIXED) {
            pattern = Math.floorMod(volley, 2) == 0
                    ? VolleyPattern.SPREAD : VolleyPattern.SWEEP;
            step = Math.floorDiv(volley, 2);
        }
        if (pattern == VolleyPattern.RANDOM) {
            int[] pool = new int[available];
            for (int i = 0; i < available; i++) {
                pool[i] = i;
            }
            for (int i = 0; i < count; i++) {
                int j = i + random.nextInt(available - i);
                int temp = pool[i];
                pool[i] = pool[j];
                pool[j] = temp;
                selected[i] = pool[i];
            }
        } else {
            int start = Math.floorMod(step, available);
            for (int i = 0; i < count; i++) {
                int offset = pattern == VolleyPattern.SPREAD
                        ? i * available / count : i;
                selected[i] = (start + offset) % available;
            }
        }
        return selected;
    }

    /**
     * Adjust the original frames-between-moves value. Rows move together,
     * so neither the formation's layout nor any enemy hitbox is changed.
     * Tick advances only while the formation is being updated (not paused/frozen).
     */
    public int movementInterval(int originalInterval, long tick) {
        if (movementRhythm == MovementRhythm.STEADY) {
            return originalInterval;
        }
        int phase = Math.floorMod(tick, 180);
        int adjustment;
        if (movementRhythm == MovementRhythm.PULSE) {
            adjustment = phase < 60 ? 3 : phase < 120 ? 0 : -3;
        } else {
            adjustment = phase < 60 ? 2 : phase < 90 ? 0 : -5;
        }
        // Limit bursts to at most one movement step every six game updates.
        return Math.max(6, originalInterval + adjustment);
    }

    /** One direction feint per surge cycle; boundaries are checked by the formation. */
    public boolean shouldFeint(long tick) {
        return movementRhythm == MovementRhythm.SURGE
                && Math.floorMod(tick, 180) == 90;
    }
}
