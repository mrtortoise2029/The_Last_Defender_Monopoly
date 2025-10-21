package Ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PlayerHistory class to track all player activities and transactions
 */
public class PlayerHistory {
    private final List<HistoryEntry> history = new ArrayList<>();
    
    public PlayerHistory(Player player) {
        // Player reference not needed for current implementation
    }
    
    /**
     * Add a new history entry
     */
    public void addEntry(HistoryEntry entry) {
        history.add(entry);
    }
    
    /**
     * Get all history entries
     */
    public List<HistoryEntry> getHistory() {
        return new ArrayList<>(history);
    }
    
    /**
     * Get history entries by type
     */
    public List<HistoryEntry> getHistoryByType(HistoryEntry.Type type) {
        return history.stream()
                .filter(entry -> entry.getType() == type)
                .toList();
    }
    
    /**
     * Get recent history (last N entries)
     */
    public List<HistoryEntry> getRecentHistory(int count) {
        int start = Math.max(0, history.size() - count);
        return history.subList(start, history.size());
    }
    
    /**
     * Get owned tiles count
     */
    public int getOwnedTilesCount() {
        return (int) history.stream()
                .filter(entry -> entry.getType() == HistoryEntry.Type.BOUGHT_TILE)
                .count();
    }
    
    /**
     * Get total money spent
     */
    public int getTotalMoneySpent() {
        return history.stream()
                .filter(entry -> entry.getType() == HistoryEntry.Type.BOUGHT_TILE || 
                               entry.getType() == HistoryEntry.Type.UPGRADED_TILE)
                .mapToInt(HistoryEntry::getAmount)
                .sum();
    }
    
    /**
     * Get total money earned
     */
    public int getTotalMoneyEarned() {
        return history.stream()
                .filter(entry -> entry.getType() == HistoryEntry.Type.SOLD_TILE ||
                               entry.getType() == HistoryEntry.Type.INCOME)
                .mapToInt(HistoryEntry::getAmount)
                .sum();
    }
    
    /**
     * Get owned tiles list
     */
    public List<String> getOwnedTiles() {
        return history.stream()
                .filter(entry -> entry.getType() == HistoryEntry.Type.BOUGHT_TILE)
                .map(HistoryEntry::getDescription)
                .toList();
    }
    
    /**
     * Clear all history (for new games)
     */
    public void clear() {
        history.clear();
    }
    
    /**
     * History entry class
     */
    public static class HistoryEntry {
        public enum Type {
            BOUGHT_TILE("Bought Tile"),
            SOLD_TILE("Sold Tile"),
            UPGRADED_TILE("Upgraded Tile"),
            INCOME("Income"),
            EXPENSE("Expense"),
            ATTACK("Attack"),
            DEFEND("Defend"),
            MEDICINE_USED("Medicine Used"),
            BONUS("Bonus"),
            OTHER("Other");
            
            private final String displayName;
            
            Type(String displayName) {
                this.displayName = displayName;
            }
            
            public String getDisplayName() {
                return displayName;
            }
        }
        
        private final Type type;
        private final String description;
        private final int amount;
        private final LocalDateTime timestamp;
        
        public HistoryEntry(Type type, String description, int amount) {
            this.type = type;
            this.description = description;
            this.amount = amount;
            this.timestamp = LocalDateTime.now();
        }
        
        public Type getType() { return type; }
        public String getDescription() { return description; }
        public int getAmount() { return amount; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public String getFormattedTimestamp() {
            return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s (%s%d)", 
                getFormattedTimestamp(), 
                type.getDisplayName(), 
                description, 
                amount >= 0 ? "+" : "", 
                amount);
        }
    }
}
