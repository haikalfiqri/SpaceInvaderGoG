
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

import engine.CurrencyManager;
import engine.DiamondManager;
import engine.LevelRewards;

/**
 * Tests Story Mode first-clear rewards (GOG-99).
 * Uses a temporary save folder to protect actual player saves.
 */
public final class LevelRewardsTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition)
            throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory(
                "level-rewards-test").toFile();

        System.setProperty(
                "game.saveDirectory", dir.getAbsolutePath());

        // Test the rewards for each eligible level.
        int paying = 0;

        for (int level = -1; level <= 42; level++) {
            LevelRewards.Reward r = LevelRewards.rewardFor(level);

            if (r != null)
                paying++;

            if (level == 5 || level == 15
                    || level == 25 || level == 35) {
                check(r != null
                                && r.getCoins() == 10
                                && r.getDiamonds() == 0,
                        "Milestone reward wrong at " + level);

            } else if (level == 10) {
                check(r != null
                                && r.getCoins() == 20
                                && r.getDiamonds() == 5,
                        "Level 10 reward wrong");

            } else if (level == 20) {
                check(r != null
                                && r.getCoins() == 20
                                && r.getDiamonds() == 7,
                        "Level 20 reward wrong");

            } else if (level == 30) {
                check(r != null
                                && r.getCoins() == 20
                                && r.getDiamonds() == 11,
                        "Level 30 reward wrong");

            } else if (level == 40) {
                check(r != null
                                && r.getCoins() == 50
                                && r.getDiamonds() == 17,
                        "Level 40 reward wrong");

            } else {
                check(r == null,
                        "Level " + level + " should pay nothing");
            }
        }

        check(paying == 8,
                "Expected 8 paying levels, got " + paying);

        // Verify that first-clear rewards are paid once.
        int coinsBefore =
                CurrencyManager.getInstance().getCoins();
        int diamondsBefore =
                DiamondManager.getInstance().getDiamonds();

        check(LevelRewards.claim(3) == null,
                "Normal level received a bonus");

        LevelRewards.Reward paid = LevelRewards.claim(10);

        check(paid != null,
                "Level 10 did not pay");

        check(CurrencyManager.getInstance().getCoins()
                        == coinsBefore + 20,
                "Coin reward incorrect");

        check(DiamondManager.getInstance().getDiamonds()
                        == diamondsBefore + 5,
                "Diamond reward incorrect");

        check(LevelRewards.claim(10) == null,
                "Level 10 paid twice");

        check(CurrencyManager.getInstance().getCoins()
                        == coinsBefore + 20,
                "Second claim changed coins");

        // Verify persistence after reloading.
        Method reload = LevelRewards.class.getDeclaredMethod(
                "reloadForTests");
        reload.setAccessible(true);
        reload.invoke(null);

        check(LevelRewards.isClaimed(10),
                "Claim not saved to disk");

        check(!LevelRewards.isClaimed(5),
                "Unclaimed level marked claimed");

        check(LevelRewards.claim(10) == null,
                "Paid again after restart");

        check(new File(dir, "level-rewards").isFile(),
                "Save file missing");

        System.out.println("LevelRewardsTest: "
                + checks + " checks passed");
    }
}
