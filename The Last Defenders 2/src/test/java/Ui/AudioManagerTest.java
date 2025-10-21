package Ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify AudioManager functionality
 */
public class AudioManagerTest {
    
    @Test
    public void testAudioManagerSingleton() {
        AudioManager manager1 = AudioManager.get();
        AudioManager manager2 = AudioManager.get();
        assertSame(manager1, manager2, "AudioManager should be a singleton");
    }
    
    @Test
    public void testAudioManagerEnabledState() {
        AudioManager manager = AudioManager.get();
        
        // Test initial state
        assertTrue(manager.isEnabled(), "AudioManager should be enabled by default");
        
        // Test disabling
        manager.setEnabled(false);
        assertFalse(manager.isEnabled(), "AudioManager should be disabled after setEnabled(false)");
        
        // Test re-enabling
        manager.setEnabled(true);
        assertTrue(manager.isEnabled(), "AudioManager should be enabled after setEnabled(true)");
    }
    
    @Test
    public void testAudioManagerMethods() {
        AudioManager manager = AudioManager.get();
        
        // These methods should not throw exceptions
        assertDoesNotThrow(() -> manager.playMenuLoop(), "playMenuLoop should not throw exception");
        assertDoesNotThrow(() -> manager.playBoardLoop(), "playBoardLoop should not throw exception");
        assertDoesNotThrow(() -> manager.stopAll(), "stopAll should not throw exception");
    }
}

