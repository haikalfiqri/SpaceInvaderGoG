package engine;

import entity.EnemyType;
import static engine.FormationTactics.MovementRhythm.*;
import static engine.FormationTactics.VolleyPattern.*;

/**
 * GOG-105: tuning for the 27 non-boss levels in Acts 2-4.
 * The targets are playtest starting points, not measured difficulty scores.
 * Original row types, row counts, widths, HP and diagonal levels are retained.
 */
public final class StoryDifficulty {
    private StoryDifficulty() { }

    private static final class Stage {
        final int movement;
        final int interval;
        final FormationTactics tactics;

        Stage(int movement, int interval, int shooters,
              FormationTactics.VolleyPattern pattern,
              FormationTactics.MovementRhythm rhythm) {
            this.movement = movement;
            this.interval = interval;
            this.tactics = new FormationTactics(shooters, pattern, rhythm);
        }
    }

    // Movement: smaller = faster. Interval: milliseconds between volleys.
    // Shooters: maximum simultaneous front-column enemies (not projectiles).
    private static final Stage[][] ACTS = {
        {   // Act 2 (11-19): learn coordinated fire, then build pressure.
            new Stage(22,  800, 1, RANDOM, STEADY), // 11
            new Stage(20,  680, 1, RANDOM, STEADY), // 12
            new Stage(18, 1100, 2, SPREAD, STEADY), // 13
            new Stage(16,  950, 2, SPREAD, STEADY), // 14
            new Stage(13,  820, 2, SWEEP,  PULSE),  // 15
            new Stage(10,  700, 2, SPREAD, PULSE),  // 16
            new Stage( 7,  590, 2, MIXED,  PULSE),  // 17
            new Stage( 5,  800, 3, SWEEP,  PULSE),  // 18
            new Stage( 3,  660, 3, MIXED,  PULSE)   // 19
        },
        {   // Act 3 (21-29): coordinated waves and direction changes.
            new Stage(16, 850, 2, SPREAD, PULSE), // 21
            new Stage(14, 760, 2, SWEEP,  PULSE), // 22
            new Stage(12, 680, 2, MIXED,  PULSE), // 23
            new Stage(10, 600, 2, SWEEP,  SURGE), // 24
            new Stage( 8, 810, 3, SPREAD, PULSE), // 25
            new Stage( 6, 720, 3, MIXED,  SURGE), // 26
            new Stage( 4, 640, 3, SWEEP,  SURGE), // 27
            new Stage( 3, 560, 3, MIXED,  SURGE), // 28
            new Stage( 2, 500, 3, MIXED,  SURGE)  // 29
        },
        {   // Act 4 (31-39): the final stretch, with four-shooter volleys.
            new Stage(12, 680, 2, MIXED,  PULSE), // 31
            new Stage(10, 620, 2, SWEEP,  PULSE), // 32
            new Stage( 8, 560, 2, MIXED,  SURGE), // 33
            new Stage( 6, 750, 3, SPREAD, SURGE), // 34
            new Stage( 5, 660, 3, SWEEP,  SURGE), // 35
            new Stage( 4, 580, 3, MIXED,  SURGE), // 36
            new Stage( 3, 500, 3, MIXED,  SURGE), // 37
            new Stage( 2, 620, 4, SWEEP,  SURGE), // 38
            new Stage( 1, 550, 4, MIXED,  SURGE)  // 39
        }
    };

    /** Return the original object for Act 1 and bosses; copy later stages. */
    public static GameSettings rebalance(int level, GameSettings original) {
        if (level < 11 || level > 39 || level % 10 == 0) {
            return original;
        }
        Stage stage = ACTS[(level - 11) / 10][(level - 1) % 10];
        GameSettings result = new GameSettings(original.getFormationWidth(),
                original.getFormationHeight(), stage.movement, stage.interval,
                original.isDiagonalMovement(), original.getTwoHitEnemyCount());
        EnemyType[] rows = new EnemyType[original.getFormationHeight()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = original.getRowType(i);
        }
        return result.withRowTypes(rows).withFormationTactics(stage.tactics);
    }
}
