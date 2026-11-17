package net.runelite.client.plugins.microbot.autowoodcutter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TreeType {
    TREE("Tree"),
    OAK("Oak tree"),
    WILLOW("Willow tree"),
    TEAK("Teak tree"),
    MAPLE("Maple tree"),
    MAHOGANY("Mahogany tree"),
    YEW("Yew tree"),
    MAGIC("Magic tree"),
    REDWOOD("Redwood tree");

    /** Object name as it appears in game. Stumps are named differently, so an exact match skips them. */
    private final String objectName;

    @Override
    public String toString() {
        return objectName;
    }
}
