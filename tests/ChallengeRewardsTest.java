import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Random;

import engine.ChallengeRewards;
import engine.DailyChallenge;
import engine.WeeklyChallenge;
import skilltree.ShardType;
import skilltree.ShardWallet;

/** Standalone checks for the daily and weekly shard rewards (GOG-46). */
public final class ChallengeRewardsTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("challenge-rewards-test").toFile();
        File save = new File(dir, "challenge-claims");
        ShardWallet wallet = ShardWallet.forFile(new File(dir, "shards"));
        ChallengeRewards rewards = ChallengeRewards.forFile(save);
        LocalDate friday = LocalDate.of(2026, 11, 27); // week starts Sun 22 Nov

        // Daily: 10 Common, 3 Rare, 1 Epic, once per day.
        check(!rewards.isDailyClaimed(friday), "not claimed yet");
        check("+10 Common, +3 Rare, +1 Epic shards".equals(rewards.claimDaily(friday, wallet, 1.0)),
                "daily text");
        check(wallet.get(ShardType.COMMON) == 10 && wallet.get(ShardType.RARE) == 3
                && wallet.get(ShardType.EPIC) == 1, "daily paid");
        check(rewards.isDailyClaimed(friday), "claimed");
        check(rewards.claimDaily(friday, wallet, 1.0) == null, "second clear pays nothing");
        check(wallet.get(ShardType.COMMON) == 10, "not paid twice");
        check(rewards.claimDaily(friday.plusDays(1), wallet, 1.0) != null, "next day pays again");

        // Weekly: once per Sunday-to-Saturday week.
        Random noMythic = new Random() { public double nextDouble() { return 0.99; } };
        Random mythic = new Random() { public double nextDouble() { return 0.0; } };
        ShardWallet w2 = ShardWallet.forFile(new File(dir, "shards2"));
        check("+25 Common, +8 Rare, +3 Epic, +1 Legendary shards"
                .equals(rewards.claimWeekly(friday, w2, 1.0, noMythic)), "weekly text");
        check(w2.get(ShardType.MYTHIC) == 0, "no mythic roll");
        check(rewards.isWeeklyClaimed(LocalDate.of(2026, 11, 22)), "Sunday same week");
        check(rewards.isWeeklyClaimed(LocalDate.of(2026, 11, 28)), "Saturday same week");
        check(rewards.claimWeekly(LocalDate.of(2026, 11, 28), w2, 1.0, mythic) == null,
                "same week pays nothing");
        check(!rewards.isWeeklyClaimed(LocalDate.of(2026, 11, 29)), "next Sunday is new");
        check(rewards.claimWeekly(LocalDate.of(2026, 11, 29), w2, 1.0, mythic).contains("+1 Mythic"),
                "mythic bonus");
        check(w2.get(ShardType.MYTHIC) == 1 && w2.get(ShardType.COMMON) == 50, "two weeks paid");

        // Mythic chance is about 20%.
        int hits = 0;
        Random seeded = new Random(7);
        for (int i = 0; i < 400; i++) {
            ChallengeRewards fresh = ChallengeRewards.forFile(new File(dir, "c" + i));
            ShardWallet w = ShardWallet.forFile(new File(dir, "w" + i));
            fresh.claimWeekly(friday, w, 1.0, seeded);
            hits += w.get(ShardType.MYTHIC);
        }
        check(hits > 50 && hits < 110, "mythic chance off: " + hits + "/400");

        // Shard Hunter: +10%, rounded.
        ShardWallet w3 = ShardWallet.forFile(new File(dir, "shards3"));
        ChallengeRewards.forFile(new File(dir, "h")).claimDaily(friday, w3, 1.1);
        check(w3.get(ShardType.COMMON) == 11 && w3.get(ShardType.RARE) == 3, "hunter daily");

        // Claims survive a restart.
        ChallengeRewards reloaded = ChallengeRewards.forFile(save);
        check(reloaded.isDailyClaimed(friday.plusDays(1)), "daily saved");
        check(reloaded.isWeeklyClaimed(LocalDate.of(2026, 12, 1)), "weekly saved");

        // A full wallet: no pay, and the claim is given back to try again.
        ShardWallet full = ShardWallet.forFile(new File(dir, "full"));
        full.add(ShardType.COMMON, Integer.MAX_VALUE);
        ChallengeRewards c = ChallengeRewards.forFile(new File(dir, "c-full"));
        check(c.claimDaily(friday, full, 1.0).contains("Couldn't"), "full wallet text");
        check(!c.isDailyClaimed(friday), "claim undone");

        // Hooked into the challenges' payReward.
        File saveDir = Files.createTempDirectory("challenge-rewards-hook").toFile();
        System.setProperty("game.saveDirectory", saveDir.getPath());
        DailyChallenge.setRewardHandler(null);
        WeeklyChallenge.setRewardHandler(null);
        ChallengeRewards.install();
        ChallengeRewards.install(); // twice is fine
        // playerFactor() needs the selected ship; with no shop save it is the Standard ship.
        String first = DailyChallenge.payReward(friday);
        check(first.startsWith("+10 Common"), "hooked daily: " + first);
        check(DailyChallenge.payReward(friday).contains("already claimed"), "hooked repeat");
        check(WeeklyChallenge.payReward(friday).startsWith("+25 Common"), "hooked weekly");
        check(WeeklyChallenge.payReward(friday).contains("already claimed"), "hooked weekly repeat");
        DailyChallenge.setRewardHandler(null);
        WeeklyChallenge.setRewardHandler(null);

        System.out.println("ChallengeRewardsTest: " + checks + " checks passed");
    }
}
