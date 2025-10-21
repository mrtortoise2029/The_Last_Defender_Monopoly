package Ui;

import java.util.HashMap;
import java.util.Map;

class TerritorySpec {
    final String displayName;
    final int buyCost;
    final int attackRequirement;
    final int failHealthLoss;
    final int influenceRequirement;
    final int influenceOnClaim;
    final int attackBonusOnClaim;
    final int medicineBonusOnClaim;
    final int[] rentMoney;        // index 0..3 for level 0..3
    final int[] rentMedicine;     // optional medicine rent component per level
    final int[] upgradeCosts;     // to go from level n to n+1
    final int[] perRoundAttack;   // index 0..3
    final int[] perRoundMedicine; // index 0..3

    TerritorySpec(String displayName,
                  int buyCost,
                  int attackRequirement,
                  int failHealthLoss,
                  int influenceRequirement,
                  int influenceOnClaim,
                  int attackBonusOnClaim,
                  int medicineBonusOnClaim,
                  int[] rentMoney,
                  int[] rentMedicine,
                  int[] upgradeCosts,
                  int[] perRoundAttack,
                  int[] perRoundMedicine) {
        this.displayName = displayName;
        this.buyCost = buyCost;
        this.attackRequirement = attackRequirement;
        this.failHealthLoss = failHealthLoss;
        this.influenceRequirement = influenceRequirement;
        this.influenceOnClaim = influenceOnClaim;
        this.attackBonusOnClaim = attackBonusOnClaim;
        this.medicineBonusOnClaim = medicineBonusOnClaim;
        this.rentMoney = rentMoney;
        this.rentMedicine = rentMedicine;
        this.upgradeCosts = upgradeCosts;
        this.perRoundAttack = perRoundAttack;
        this.perRoundMedicine = perRoundMedicine;
    }

    String baseRentDescription() {
        String med = rentMedicine[0] > 0 ? (" + " + rentMedicine[0] + "Med") : "";
        return rentMoney[0] + "M" + med + " / " + rentMoney[1] + "M" + (rentMedicine[1] > 0 ? ("+" + rentMedicine[1] + "Med") : "") +
               " / " + rentMoney[2] + "M" + (rentMedicine[2] > 0 ? ("+" + rentMedicine[2] + "Med") : "") +
               " / " + rentMoney[3] + "M" + (rentMedicine[3] > 0 ? ("+" + rentMedicine[3] + "Med") : "");
    }

    String rentDescriptionForLevel(int level) {
        String med = rentMedicine[level] > 0 ? (" + " + rentMedicine[level] + "Med") : "";
        return rentMoney[level] + "M" + med;
    }
}

public class TerritoryCatalog {
    private static final Map<Integer, TerritorySpec> byPosition = new HashMap<>();

    static {
        // Residential: bottom row positions — aligned with BoardController mapping
        byPosition.put(21, new TerritorySpec("Abandoned House", 100, 3, 15, 0, 15, 2, 0,
                new int[]{20, 30, 40, 40}, new int[]{0, 0, 0, 0},
                new int[]{40, 80, 120}, new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}));
        byPosition.put(23, new TerritorySpec("Abandoned House", 100, 3, 15, 0, 15, 2, 0,
                new int[]{20, 30, 40, 40}, new int[]{0, 0, 0, 0},
                new int[]{40, 80, 120}, new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}));
        byPosition.put(26, new TerritorySpec("Apartment Complex", 150, 3, 15, 0, 20, 3, 0,
                new int[]{30, 50, 70, 70}, new int[]{0, 0, 0, 0},
                new int[]{60, 90, 130}, new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}));
        byPosition.put(28, new TerritorySpec("Apartment Complex", 150, 3, 15, 0, 20, 3, 0,
                new int[]{30, 50, 70, 70}, new int[]{0, 0, 0, 0},
                new int[]{60, 90, 130}, new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}));
        byPosition.put(29, new TerritorySpec("Motel Shelter", 180, 3, 15, 0, 25, 0, 1,
                new int[]{40, 60, 80, 80}, new int[]{0, 0, 0, 0},
                new int[]{70, 100, 150}, new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}));

        // Commercial: left column example 4
        byPosition.put(32, new TerritorySpec("Supermarket", 220, 6, 25, 0, 40, 5, 0,
                new int[]{50, 70, 90, 90}, new int[]{0, 0, 0, 0},
                new int[]{80, 120, 160}, new int[]{0, 5, 9, 13}, new int[]{0, 0, 0, 0}));
        byPosition.put(34, new TerritorySpec("Gas Station", 250, 6, 25, 0, 50, 6, 0,
                new int[]{60, 80, 100, 100}, new int[]{0, 0, 0, 0},
                new int[]{100, 140, 180}, new int[]{0, 6, 10, 14}, new int[]{0, 0, 0, 0}));
        byPosition.put(36, new TerritorySpec("Pharmacy", 280, 6, 25, 0, 60, 0, 0,
                new int[]{70, 90, 110, 110}, new int[]{1, 1, 2, 3},
                new int[]{110, 150, 190}, new int[]{0, 0, 0, 0}, new int[]{0, 1, 2, 3}));
        byPosition.put(39, new TerritorySpec("Hardware Store", 300, 6, 25, 0, 70, 7, 0,
                new int[]{75, 95, 120, 120}, new int[]{0, 0, 0, 0},
                new int[]{130, 170, 210}, new int[]{0, 9, 12, 15}, new int[]{0, 0, 0, 0}));

        // Industrial: top row examples (positions 1..9). We'll map 1, 2, 4, 6 as examples
        byPosition.put(1, new TerritorySpec("Factory", 350, 10, 40, 65, 95, 0, 0,
                new int[]{90, 110, 140, 140}, new int[]{0, 0, 0, 0},
                new int[]{150, 175, 220}, new int[]{0, 5, 10, 15}, new int[]{0, 10, 15, 20}));
        byPosition.put(2, new TerritorySpec("Power Plant", 400, 10, 40, 65, 95, 0, 0,
                new int[]{100, 120, 160, 160}, new int[]{0, 0, 0, 0},
                new int[]{170, 195, 230}, new int[]{0, 10, 15, 20}, new int[]{0, 3, 4, 5}));
        byPosition.put(4, new TerritorySpec("Warehouse", 420, 10, 40, 85, 105, 0, 0,
                new int[]{110, 130, 170, 170}, new int[]{0, 0, 0, 0},
                new int[]{190, 210, 250}, new int[]{0, 10, 15, 20}, new int[]{0, 0, 0, 0}));
        byPosition.put(9, new TerritorySpec("Radio Tower", 450, 10, 40, 180, 115, 0, 0,
                new int[]{120, 130, 130, 130}, new int[]{0, 0, 0, 0},
                new int[]{200, 600, 600}, new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}));

        // Military: right column examples (positions 11..19) - subset mapped for now
        byPosition.put(12, new TerritorySpec("Research Lab", 500, 32, 60, 200, 0, 0, 0,
                new int[]{130, 130, 130, 130}, new int[]{0, 0, 0, 0},
                new int[]{0, 0, 0}, new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}));
        byPosition.put(17, new TerritorySpec("Military Base", 600, 40, 60, 350, 200, 0, 0,
                new int[]{150, 170, 220, 220}, new int[]{0, 0, 0, 0},
                new int[]{0, 0, 0}, new int[]{0, 20, 25, 30}, new int[]{0, 0, 0, 0}));
        byPosition.put(18, new TerritorySpec("Hospital", 650, 35, 60, 180, 120, 0, 0,
                new int[]{160, 160, 160, 160}, new int[]{0, 0, 0, 0},
                new int[]{0, 0, 0}, new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}));
        // Airport is special/unbuyable, handled elsewhere
    }

    public static TerritorySpec getSpecForPosition(int position) {
        return byPosition.get(position);
    }
}


