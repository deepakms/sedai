package co.sedai;

import co.sedai.model.Config;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    private Config invokeLoadConfig(String resourceName) throws Exception {
        Method method = Main.class.getDeclaredMethod("loadConfig", String.class);
        method.setAccessible(true);
        try {
            return (Config) method.invoke(null, resourceName);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw e;
            }
        }
    }

    @Test
    void loadConfig_Success() throws Exception {
        Config config = invokeLoadConfig("test.properties");

        assertNotNull(config);
        assertTrue(config.filePath().endsWith("test-points.csv"));
        assertEquals(10, config.mapWidth());
        assertEquals(5, config.mapHeight());
        assertEquals(",", config.inputDelimiter());
        assertEquals(1, config.inputSkipHeaderLines());
        assertEquals(0, config.latColumn());
        assertEquals(1, config.longColumn());
        assertArrayEquals(".123".toCharArray(), config.renderDensityChars());
        assertEquals(true, config.enableParallelProcessing());
        assertEquals(true, config.htmlEnabled());
    }

    @Test
    void loadConfig_MissingFile() {
        assertThrows(ConfigurationException.class, () -> {
            invokeLoadConfig("non-existent-config.properties");
        }, "Loading a non-existent config file should throw RuntimeException");
    }

    @Test
    void loadConfig_MissingRequiredProperty() {
        // Expect IllegalArgumentException (unchecked, thrown directly by invoke)
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            invokeLoadConfig("test-missing-file.properties");
        });
        // Check the message matches the one thrown in loadConfig
        assertTrue(thrown.getMessage().contains("Missing required configuration property: input.file_path"));
    }

    @Test
    void loadConfig_InvalidPropertyValue() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            // Ensure "test-invalid-input.properties" exists and has invalid width/height
            invokeLoadConfig("test-invalid-input.properties");
        });
        assertTrue(thrown.getMessage().contains("Map width and height must be >0"),
                   "Exception message should indicate invalid dimensions. Actual: " + thrown.getMessage());
    }
    
    @Test
    void main_Placeholder() {
        assertTrue(true, "Testing main() execution flow typically requires integration testing setup.");
    }
}