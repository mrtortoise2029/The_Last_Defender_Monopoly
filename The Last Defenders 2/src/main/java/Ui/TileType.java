package Ui;

public enum TileType {
    START("Start", "lightgreen"),
    FREE("Free", "lightgray"),
    CHECKPOST("Check-post", "orange"),
    QUARANTINE("Quarantine", "red"),
    SAFEHAVEN("Safe Haven", "lightblue"),
    TERRITORY("Territory", "lightgreen"),
    ZOMBIE("Zombie", "salmon"),
    RESOURCE("Resource", "khaki"),
    SCENARIO("Scenario", "orange"),
    SPECIAL("Special", "violet");

    private final String displayName;
    private final String color;

    TileType(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }
}

