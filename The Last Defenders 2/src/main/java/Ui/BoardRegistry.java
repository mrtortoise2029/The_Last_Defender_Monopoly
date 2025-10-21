 package Ui;

import java.util.HashMap;
import java.util.Map;

public class BoardRegistry {
    private static final Map<Integer, Tile> byPosition = new HashMap<>();

    public static Tile getOrCreate(int position, TileType type, String name) {
        Tile tile = byPosition.get(position);
        if (tile == null) {
            tile = new Tile(type, name, position);
            byPosition.put(position, tile);
        } else {
            // keep existing owner/level; update type/name if changed mapping
            // only if they differ and tile not yet owned
            if (!tile.isOwned()) {
                // recreate lightweight representation to carry label/type
                tile = new Tile(type, name, position);
                byPosition.put(position, tile);
            }
        }
        return tile;
    }

    public static Map<Integer, Tile> tiles() { return byPosition; }
    
    /**
     * Clear all tiles and reset the board registry
     * This should be called when starting a new game
     */
    public static void clearAll() {
        byPosition.clear();
    }
}


