package net.runelite.client.plugins.microbot.autowoodcutter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("autowoodcutter")
public interface AutoWoodcutterConfig extends Config {

    @ConfigItem(
            keyName = "tree",
            name = "Tree",
            description = "Tree to chop. Start the script standing near trees of this type.",
            position = 0
    )
    default TreeType tree() {
        return TreeType.TREE;
    }

    @ConfigItem(
            keyName = "strayDistance",
            name = "Stray distance",
            description = "Only chop trees within this many tiles of where you started. You walk back here after banking.",
            position = 1
    )
    @Range(min = 3, max = 40)
    default int strayDistance() {
        return 20;
    }
}
