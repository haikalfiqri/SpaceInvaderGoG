import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import engine.DailyChallenge;
import engine.ShardReward;
import engine.ShardSpace;
import engine.ShardSpace.Mode;
import skilltree.ShardType;
import skilltree.ShardWallet;

/** Standalone checks for the Shard Space rules and rewards (GOG-48). */
public final class ShardSpaceTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("shard-space-test").toFile();
        File save = new File(dir, "shard-space");
        ShardWallet wallet = ShardWallet.forFile(new File(dir, "shards"));
        ShardSpace space = ShardSpace.forFile(save);
        LocalDate day = LocalDate.of(2026, 11, 27);
        Random random = new Random(42);

        // Stages: 3 waves of the mode's act, then the act's boss level.
        for (Mode mode : Mode.values()) {
            List<DailyChallenge.Stage> stages = ShardSpace.stagesFor(mode);
            check(stages.size() == ShardSpace.WAVES + 1, mode + " stage count");
            for (int i = 0; i < ShardSpace.WAVES; i++)
                check(!stages.get(i).isBoss() && stages.get(i).getAct() == mode.getAct(),
                        mode + " wave " + i);
            check(stages.get(3).isBoss() && stages.get(3).getLevel() == mode.getAct() * 10,
                    mode + " boss level");
        }
        check(Mode.MYTHIC.getTitle().equals("Mythic Shard Space"), "title");

        // Unlocks: only Common at first; 5 clears open the next mode.
        check(space.isUnlocked(Mode.COMMON), "common open");
        check(!space.isUnlocked(Mode.RARE) && space.clearsToUnlock(Mode.RARE) == 5, "rare locked");
        check(!space.startPlay(Mode.RARE, day), "locked mode can't start");
        check(space.playsLeft(day) == 5, "locked start used no play");

        // Plays: 5 a day for all modes, used at the start.
        for (int i = 0; i < 5; i++) {
            check(space.startPlay(Mode.COMMON, day), "play " + i);
            space.recordClear(Mode.COMMON, wallet, 1.0, random);
        }
        check(space.playsLeft(day) == 0, "no plays left");
        check(!space.startPlay(Mode.COMMON, day), "6th play refused");
        check(space.clears(Mode.COMMON) == 5, "5 clears counted");
        check(space.isUnlocked(Mode.RARE), "rare open after 5 clears");
        check(!space.isUnlocked(Mode.EPIC), "epic still locked");
        check(space.playsLeft(day.plusDays(1)) == 5, "new day, new plays");

        // Saved: a fresh load sees the same clears and plays.
        ShardSpace reloaded = ShardSpace.forFile(save);
        check(reloaded.clears(Mode.COMMON) == 5, "clears saved");
        check(reloaded.playsLeft(day) == 0, "plays saved");
        check(reloaded.startPlay(Mode.RARE, day.plusDays(1)), "rare plays next day");
        check(reloaded.playsLeft(day.plusDays(1)) == 4, "counter reset on new day");

        // Mythic needs 20 clears before it, so at least 4 days of plays.
        int days = 0;
        ShardSpace fresh = ShardSpace.forFile(new File(dir, "fresh"));
        LocalDate d = day;
        while (!fresh.isUnlocked(Mode.MYTHIC)) {
            days++;
            for (int i = 0; i < ShardSpace.PLAYS_PER_DAY; i++) {
                Mode best = Mode.COMMON;
                for (Mode m : Mode.values()) if (fresh.isUnlocked(m)) best = m;
                check(fresh.startPlay(best, d), "play on day " + days);
                fresh.recordClear(best, wallet, 1.0, random);
            }
            d = d.plusDays(1);
        }
        check(days == 4, "mythic takes 4 days, took " + days);

        // Rewards stay inside each mode's range.
        for (Mode mode : Mode.values()) {
            int[] min = mode.getMinReward(), max = mode.getMaxReward();
            for (int i = 0; i < 200; i++) {
                int[] r = ShardSpace.rollReward(mode, random);
                for (int k = 0; k < r.length; k++)
                    check(r[k] >= min[k] && r[k] <= max[k], mode + " reward out of range");
            }
        }
        check(Arrays.equals(Mode.LEGENDARY.getMinReward(), ShardReward.of(8, 3, 2, 1, 0)),
                "legendary table");

        // Shard Hunter: +10%, rounded per rarity.
        check(Arrays.equals(ShardReward.scale(ShardReward.of(10, 3, 1, 0, 0), 1.1),
                ShardReward.of(11, 3, 1, 0, 0)), "scale rounding");
        ShardWallet hunter = ShardWallet.forFile(new File(dir, "hunter"));
        String text = ShardSpace.forFile(new File(dir, "h")).recordClear(Mode.COMMON, hunter, 1.1,
                new Random(1));
        check(hunter.get(ShardType.COMMON) >= 11 && hunter.get(ShardType.COMMON) <= 15,
                "hunter paid " + hunter.get(ShardType.COMMON));
        check(text.equals("+" + hunter.get(ShardType.COMMON) + " Common shards"), "text " + text);
        check(ShardReward.describe(new int[5]).equals("No shards"), "empty text");
        check(ShardReward.describe(ShardReward.of(4, 3, 0, 0, 1))
                .equals("+4 Common, +3 Rare, +1 Mythic shards"), "multi text");

        // A broken save file reads as a fresh start, not a crash.
        File bad = new File(dir, "bad");
        Files.write(bad.toPath(), "clears.common=abc\nplays.day=yesterday\nplays.used=99\n".getBytes("UTF-8"));
        ShardSpace broken = ShardSpace.forFile(bad);
        check(broken.clears(Mode.COMMON) == 0 && broken.playsLeft(day) == 5, "bad save");

        // Running flag.
        check(!ShardSpace.isRunning(), "not running");
        ShardSpace.setActiveMode(Mode.EPIC);
        check(ShardSpace.isRunning() && ShardSpace.activeMode() == Mode.EPIC, "running");
        ShardSpace.setActiveMode(null);
        check(!ShardSpace.isRunning(), "stopped");

        System.out.println("ShardSpaceTest: " + checks + " checks passed");
    }
}
