package net.runelite.client.plugins.microbot.autowoodcutter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Chop the nearest selected tree near the start point; when the inventory is full, bank everything
 * except the axe at the nearest bank and walk back. State is re-derived from the game every tick.
 */
@Slf4j
@Singleton
public class AutoWoodcutterScript extends Script {

    private static final int RETURN_STOP_DISTANCE = 3;
    /** Ticks (600 ms each) with no tree found or no successful click before the script gives up. */
    private static final int MAX_FAILED_TICKS = 40;

    /** Where the script started. Trees are searched around this point and we walk back to it. */
    private volatile WorldPoint anchor;
    private volatile boolean preflightDone;
    private int failedTicks;

    public boolean run(AutoWoodcutterConfig config) {
        anchor = null;
        preflightDone = false;
        failedTicks = 0;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;
                tick(config);
            } catch (Exception ex) {
                log.error("Auto Woodcutter error", ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void tick(AutoWoodcutterConfig config) {
        if (anchor == null) {
            anchor = Rs2Player.getWorldLocation();
        }

        // Once at start: need the level for the tree and an axe (worn or carried).
        if (!preflightDone) {
            if (!preflight(config)) return;
            preflightDone = true;
        }

        // 1. Bank open: deposit everything except the axe, close.
        if (Rs2Bank.isOpen()) {
            Microbot.status = "Depositing";
            Rs2Bank.depositAllExcept(item -> isAxe(item.getName()));
            sleepUntil(() -> !Rs2Inventory.isFull(), 3000);
            Rs2Bank.closeBank();
            sleepUntil(() -> !Rs2Bank.isOpen(), 2000);
            failedTicks = 0;
            return;
        }

        // 2. Inventory full: walk to the nearest bank and open it. Retried next tick if it fails.
        if (Rs2Inventory.isFull()) {
            Microbot.status = "Walking to bank";
            Rs2Bank.walkToBankAndUseBank();
            return;
        }

        // 3. Not full and outside the allowed area (just banked): walk back to the start point.
        WorldPoint here = Rs2Player.getWorldLocation();
        if (here != null && anchor != null && here.distanceTo(anchor) > config.strayDistance()) {
            Microbot.status = "Returning to trees";
            Rs2Walker.walkTo(anchor, RETURN_STOP_DISTANCE);
            return;
        }

        // 4. Already chopping or walking: wait.
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            return;
        }

        // 5. Find and chop the nearest selected tree around the start point.
        Rs2TileObjectModel tree = Microbot.getRs2TileObjectCache().query()
                .withName(config.tree().getObjectName())
                .nearestOnClientThread(anchor, config.strayDistance());
        if (tree == null) {
            Microbot.status = "No " + config.tree() + " nearby";
            recordFailure();
            return;
        }

        Microbot.status = "Chopping " + config.tree();
        if (tree.click("Chop down")) {
            failedTicks = 0;
            sleepUntil(() -> Rs2Player.isAnimating() || Rs2Player.isMoving(), 3000);
        } else {
            recordFailure();
        }
    }

    private boolean preflight(AutoWoodcutterConfig config) {
        TreeType tree = config.tree();
        int level = Rs2Player.getRealSkillLevel(Skill.WOODCUTTING);
        if (level < tree.getLevelRequired()) {
            stopWith("Need Woodcutting " + tree.getLevelRequired() + " for " + tree + " (you have " + level + ")");
            return false;
        }
        boolean hasAxe = Rs2Inventory.contains(item -> isAxe(item.getName()))
                || Rs2Equipment.isWearing(item -> isAxe(item.getName()));
        if (!hasAxe) {
            stopWith("No axe in inventory or equipped");
            return false;
        }
        return true;
    }

    private void recordFailure() {
        if (++failedTicks >= MAX_FAILED_TICKS) {
            stopWith("Stopped: no progress for " + MAX_FAILED_TICKS + " ticks");
        }
    }

    private void stopWith(String reason) {
        Microbot.status = reason;
        log.warn("Auto Woodcutter: {}", reason);
        shutdown();
    }

    private static boolean isAxe(String name) {
        return name != null && name.toLowerCase().endsWith("axe");
    }

    @Override
    public void shutdown() {
        anchor = null;
        preflightDone = false;
        super.shutdown();
    }
}
