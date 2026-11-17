package net.runelite.client.plugins.microbot.autowoodcutter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("autowoodcutter")
public interface AutoWoodcutterConfig extends Config {

    @ConfigItem(
            keyName = "tree",
            name = "Tree",
            description = "Tree to chop. Start the script near trees of this type.",
            position = 0
    )
    default TreeType tree() {
        return TreeType.TREE;
    }

    @ConfigItem(
            keyName = "searchRadius",
            name = "Search radius",
            description = "How far from you to look for the selected tree (tiles)",
            position = 1
    )
    default int searchRadius() {
        return 20;
    }
}
