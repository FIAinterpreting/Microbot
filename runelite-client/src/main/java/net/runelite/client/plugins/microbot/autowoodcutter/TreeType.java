package net.runelite.client.plugins.microbot.autowoodcutter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TreeType {
    TREE("Tree", 1),
    OAK("Oak tree", 15),
    WILLOW("Willow tree", 30),
    TEAK("Teak tree", 35),
    MAPLE("Maple tree", 45),
    MAHOGANY("Mahogany tree", 50),
    YEW("Yew tree", 60),
    MAGIC("Magic tree", 75),
    REDWOOD("Redwood tree", 90);

    /** Object name as it appears in game. Stumps are named differently, so an exact match skips them. */
    private final String objectName;
    private final int levelRequired;

    @Override
    public String toString() {
        return objectName;
    }
}
