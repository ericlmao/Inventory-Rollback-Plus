package me.danjono.inventoryrollback.inventory;

public enum RestoreAttribute {
    MAIN_INVENTORY("main inventory"),
    ENDER_CHEST("ender chest"),
    HEALTH("health"),
    HUNGER("hunger"),
    EXPERIENCE("experience");

    private final String displayName;

    RestoreAttribute(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
