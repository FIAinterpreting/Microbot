package net.runelite.client.plugins.microbot.autowoodcutter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Loop: chop nearest selected tree -> inventory full -> walk to nearest bank, deposit -> walk back.
 * State is re-derived from the game every tick (bank open? inventory full? far from home?).
 */
@Slf4j
@Singleton
public class AutoWoodcutterScript extends Script {

    private static final int RETURN_DISTANCE = 12;
    private static final int RETURN_STOP_DISTANCE = 3;

    /** Where we were chopping; we walk back here after banking. */
    private volatile WorldPoint home;

    public boolean run(AutoWoodcutterConfig config) {
        home = null;
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
        if (home == null) {
            home = Rs2Player.getWorldLocation();
        }

        // 1. Bank open: deposit everything except the axe, close.
        if (Rs2Bank.isOpen()) {
            Microbot.status = "Depositing";
            Rs2Bank.depositAllExcept(item -> {
                String name = item.getName();
                return name != null && name.toLowerCase().endsWith("axe");
            });
            sleepUntil(() -> !Rs2Inventory.isFull(), 3000);
            Rs2Bank.closeBank();
            sleepUntil(() -> !Rs2Bank.isOpen(), 2000);
            return;
        }

        // 2. Inventory full: go to the nearest bank and open it. Retries next tick if it fails.
        if (Rs2Inventory.isFull()) {
            Microbot.status = "Walking to bank";
            Rs2Bank.walkToBankAndUseBank();
            return;
        }

        // 3. Not full and far from the tree area (just banked): walk back.
        WorldPoint here = Rs2Player.getWorldLocation();
        if (here != null && home != null && here.distanceTo(home) > RETURN_DISTANCE) {
            Microbot.status = "Returning to trees";
            Rs2Walker.walkTo(home, RETURN_STOP_DISTANCE);
            return;
        }

        // 4. Already chopping or walking: wait.
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            return;
        }

        // 5. Find and chop the nearest selected tree.
        Rs2TileObjectModel tree = Microbot.getRs2TileObjectCache().query()
                .withName(config.tree().getObjectName())
                .nearestOnClientThread(config.searchRadius());
        if (tree == null) {
            Microbot.status = "No " + config.tree() + " nearby";
            return;
        }

        Microbot.status = "Chopping " + config.tree();
        if (tree.click("Chop down")) {
            home = tree.getWorldLocation();
            sleepUntil(() -> Rs2Player.isAnimating() || Rs2Player.isMoving(), 3000);
        }
    }

    @Override
    public void shutdown() {
        home = null;
        super.shutdown();
    }
}
