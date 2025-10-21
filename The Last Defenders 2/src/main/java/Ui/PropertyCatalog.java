package Ui;

import java.util.*;

/**
 * Defines property tiles, grouped by zone and board side.
 */
public class PropertyCatalog {

    public static class PropertyDef {
        public final String name;
        public final int cost;
        public final int attackRequired; // attack needed to clear/restore
        public final int healthLoss; // health lost if fail to clear
        public final int[] rents; // base rents per upgrade level 0..3
        public final int[] upgradeCosts; // length 3 for levels 1..3
        public final int influenceBonus; // on purchase
        public final int attackBonus; // on purchase
        public final int medicineBonus; // on purchase (if any)
        public final int influenceRequired; // minimum influence to unlock purchase
        public final boolean hasPlayerBonus; // gives ongoing bonuses to owner
        public final int[] playerAttackBonus; // ongoing attack bonus per level
        public final int[] playerInfluenceBonus; // ongoing influence bonus per level
        public final int[] playerMedicineBonus; // ongoing medicine bonus per level
        
        public PropertyDef(String name, int cost, int attackRequired, int healthLoss, int[] rents, 
                          int[] upgradeCosts, int influenceBonus, int attackBonus, int medicineBonus,
                          int influenceRequired, boolean hasPlayerBonus, 
                          int[] playerAttackBonus, int[] playerInfluenceBonus, int[] playerMedicineBonus) {
            this.name = name; this.cost = cost; this.attackRequired = attackRequired; this.healthLoss = healthLoss;
            this.rents = rents; this.upgradeCosts = upgradeCosts; this.influenceBonus = influenceBonus;
            this.attackBonus = attackBonus; this.medicineBonus = medicineBonus; this.influenceRequired = influenceRequired;
            this.hasPlayerBonus = hasPlayerBonus; this.playerAttackBonus = playerAttackBonus;
            this.playerInfluenceBonus = playerInfluenceBonus; this.playerMedicineBonus = playerMedicineBonus;
        }
    }

    // Residential Set (Cheap → Early Game)
    public static final List<PropertyDef> RESIDENTIAL = List.of(
            new PropertyDef("Abandoned House 1", 100, 3, 15, new int[]{20,30,40}, new int[]{40,80,120}, 15, 2, 0, 0, false, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Abandoned House 2", 100, 3, 15, new int[]{20,30,40}, new int[]{40,80,120}, 15, 2, 0, 0, false, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Apartment Complex 1", 150, 3, 15, new int[]{30,50,70}, new int[]{60,90,130}, 20, 3, 0, 0, false, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Apartment Complex 2", 150, 3, 15, new int[]{30,50,70}, new int[]{60,90,130}, 20, 3, 0, 0, false, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Motel Shelter", 180, 3, 15, new int[]{40,60,80}, new int[]{70,100,150}, 25, 1, 1, 0, false, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{0,0,0})
    );

    // Commercial Set (Mid-Game)
    public static final List<PropertyDef> COMMERCIAL = List.of(
            new PropertyDef("Super Market 1", 220, 6, 25, new int[]{50,70,90}, new int[]{80,120,160}, 40, 5, 0, 0, true, new int[]{5,9,13}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Super Market 2", 220, 6, 25, new int[]{50,70,90}, new int[]{80,120,160}, 40, 5, 0, 0, true, new int[]{5,9,13}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Gas Station", 250, 6, 25, new int[]{60,80,100}, new int[]{100,140,180}, 50, 6, 0, 0, true, new int[]{6,10,14}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Pharmacy", 280, 6, 25, new int[]{70,90,110}, new int[]{110,150,190}, 60, 0, 2, 0, true, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{1,2,3}),
            new PropertyDef("Hardware Store", 300, 6, 25, new int[]{75,95,120}, new int[]{130,170,210}, 70, 7, 0, 0, true, new int[]{9,12,15}, new int[]{0,0,0}, new int[]{0,0,0})
    );

    // Industrial Set (Late Mid-Game)
    public static final List<PropertyDef> INDUSTRIAL = List.of(
            new PropertyDef("Factory 1", 350, 10, 40, new int[]{90,110,140}, new int[]{150,175,220}, 95, 0, 0, 65, true, new int[]{5,10,15}, new int[]{10,15,20}, new int[]{0,0,0}),
            new PropertyDef("Factory 2", 350, 10, 40, new int[]{90,110,140}, new int[]{150,175,220}, 95, 0, 0, 65, true, new int[]{5,10,15}, new int[]{10,15,20}, new int[]{0,0,0}),
            new PropertyDef("Power Plant", 400, 10, 40, new int[]{100,120,160}, new int[]{170,195,230}, 95, 0, 0, 65, true, new int[]{10,15,20}, new int[]{3,4,5}, new int[]{0,0,0}),
            new PropertyDef("Warehouse 1", 420, 10, 40, new int[]{110,130,170}, new int[]{190,210,250}, 105, 0, 0, 85, true, new int[]{10,15,20}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Warehouse 2", 420, 10, 40, new int[]{110,130,170}, new int[]{190,210,250}, 105, 0, 0, 85, true, new int[]{10,15,20}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Radio Tower", 450, 10, 40, new int[]{120,130}, new int[]{200,600}, 115, 0, 0, 180, false, new int[]{0,0}, new int[]{0,0}, new int[]{0,0})
    );

    // Military/Strategic Set (High Cost → Endgame)
    public static final List<PropertyDef> MILITARY = List.of(
            new PropertyDef("Military Base 1", 600, 40, 60, new int[]{150,170,220}, new int[]{0,0,0}, 200, 0, 0, 350, true, new int[]{20,25,30}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Military Base 2", 600, 40, 60, new int[]{150,170,220}, new int[]{0,0,0}, 200, 0, 0, 350, true, new int[]{20,25,30}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Hospital", 650, 35, 60, new int[]{160}, new int[]{0,0,0}, 120, 0, 0, 180, true, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{5,5,5}),
            new PropertyDef("Research Lab", 500, 32, 60, new int[]{130}, new int[]{0,0,0}, 0, 0, 0, 200, false, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{0,0,0}),
            new PropertyDef("Airport", 0, 0, 0, new int[]{0}, new int[]{0,0,0}, 0, 0, 0, 0, false, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{0,0,0}) // Special unbuyable tile
    );

    public enum Zone { RESIDENTIAL, COMMERCIAL, INDUSTRIAL, MILITARY }

    public static class PropertyTile {
        public final Zone zone;
        public final int position; // board position
        public final PropertyDef def;
        public Player owner;
        public int upgradeLevel; // 0..3
        public boolean isTrap; // if set to trap instead of restore
        public boolean isRestored; // if property has been restored/cleared
        
        public PropertyTile(Zone zone, int position, PropertyDef def) { 
            this.zone = zone; this.position = position; this.def = def; 
            this.owner = null; this.upgradeLevel = 0; this.isTrap = false; this.isRestored = false;
        }
    }

    public static Map<Integer, PropertyTile> generateBoardMapping() {
        Map<Integer, PropertyTile> map = new HashMap<>();
        
        // Based on the 12x12 board layout from the image
        // Bottom row (Residential Zone) - positions 1-10
        int[] residentialPositions = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int i = 0; i < Math.min(residentialPositions.length, RESIDENTIAL.size()); i++) {
            map.put(residentialPositions[i], new PropertyTile(Zone.RESIDENTIAL, residentialPositions[i], RESIDENTIAL.get(i)));
        }
        
        // Left column (Commercial Zone) - positions 11-20
        int[] commercialPositions = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        for (int i = 0; i < Math.min(commercialPositions.length, COMMERCIAL.size()); i++) {
            map.put(commercialPositions[i], new PropertyTile(Zone.COMMERCIAL, commercialPositions[i], COMMERCIAL.get(i)));
        }
        
        // Top row (Industrial Zone) - positions 21-30
        int[] industrialPositions = {21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
        for (int i = 0; i < Math.min(industrialPositions.length, INDUSTRIAL.size()); i++) {
            map.put(industrialPositions[i], new PropertyTile(Zone.INDUSTRIAL, industrialPositions[i], INDUSTRIAL.get(i)));
        }
        
        // Right column (Military Zone) - positions 31-40
        int[] militaryPositions = {31, 32, 33, 34, 35, 36, 37, 38, 39, 40};
        for (int i = 0; i < Math.min(militaryPositions.length, MILITARY.size()); i++) {
            map.put(militaryPositions[i], new PropertyTile(Zone.MILITARY, militaryPositions[i], MILITARY.get(i)));
        }
        
        return map;
    }
}



