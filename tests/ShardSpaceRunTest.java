import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Random;

import engine.ShardSpace;
import engine.ShardSpace.Mode;
import skilltree.ShardType;
import skilltree.ShardWallet;

/**
 * Standalone checks for a Shard Space run (GOG-49): what the menu lets
 * start, that the play is used at the start, and that only a full clear
 * pays and counts.
 */
public final class ShardSpaceRunTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("shard-space-run-test").toFile();
        ShardWallet wallet = ShardWallet.forFile(new File(dir, "shards"));
        ShardSpace space = ShardSpace.forFile(new File(dir, "shard-space"));
        LocalDate day = LocalDate.of(2026, 11, 28);
        Random random = new Random(3);
        int stages = ShardSpace.stagesFor(Mode.COMMON).size();

        // Menu: locked modes and no plays left can't start.
        check(space.startProblem(Mode.COMMON, day) == null, "common can start");
        check("Locked".equals(space.startProblem(Mode.RARE, day)), "rare locked");
        check("Locked".equals(space.startProblem(Mode.MYTHIC, day)), "mythic locked");

        // Failed runs: the play is used, nothing is paid or counted.
        for (int cleared = 0; cleared < stages; cleared++) {
            check(space.startPlay(Mode.COMMON, day), "start " + cleared);
            check(space.finishRun(Mode.COMMON, cleared, stages, wallet, 1.0, random) == null,
                    cleared + "/" + stages + " pays nothing");
        }
        check(space.playsLeft(day) == ShardSpace.PLAYS_PER_DAY - stages, "failed runs used plays");
        check(space.clears(Mode.COMMON) == 0, "failed runs don't count");
        check(wallet.get(ShardType.COMMON) == 0, "failed runs pay no shards");

        // Full clear: paid and counted.
        check(space.startPlay(Mode.COMMON, day), "start full run");
        String reward = space.finishRun(Mode.COMMON, stages, stages, wallet, 1.0, random);
        check(reward != null && reward.endsWith("Common shards"), "full clear text: " + reward);
        int paid = wallet.get(ShardType.COMMON);
        check(paid >= 10 && paid <= 14, "full clear paid " + paid);
        check(space.clears(Mode.COMMON) == 1, "full clear counted");

        // All 5 plays used: the menu says so, and starting is refused.
        check(space.playsLeft(day) == 0, "no plays left");
        check("No plays left today".equals(space.startProblem(Mode.COMMON, day)), "no plays text");
        check(!space.startPlay(Mode.COMMON, day), "6th start refused");
        check(space.startProblem(Mode.COMMON, day.plusDays(1)) == null, "new day can start");

        System.out.println("ShardSpaceRunTest: " + checks + " checks passed");
    }
}
