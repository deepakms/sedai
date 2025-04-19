package co.sedai;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.sedai.model.Bounds;
import co.sedai.model.Config;

import java.io.IOException;

/**
 * Main entry point for the ASCII Map Plotter application.
 * <p>
 * This class orchestrates the process of plotting geographical point data onto
 * a
 * text-based map. It performs the following steps:
 * <ol>
 * <li>Loads configuration settings from a properties file
 * ({@code plotter.properties}).</li>
 * <li>Performs a first pass over the data file to determine the geographical
 * bounds (min/max latitude and longitude) using {@link GetDatBounds}.</li>
 * <li>Performs a second pass over the data file to populate a 2D grid
 * representing point density within the calculated bounds using
 * {@link GridDensityPopulator}.</li>
 * <li>Renders the populated grid as an ASCII map to the console (via logger)
 * using {@link RenderAsciiMap}.</li>
 * </ol>
 * The application exits with a non-zero status code if configuration loading
 * fails or
 * if errors occur during file processing.
 */

public class Main {
    static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_CONFIG_RESOURCE = "plotter.properties";

    public static void main(String[] args) {

        try {
            Config config = loadConfig(DEFAULT_CONFIG_RESOURCE);
            if (config == null) {
                System.exit(1);
            }
            logger.info("Starting ASCII Map Plotter:");
            logger.info("Input Data File: " + config.filePath());
            logger.info("Using Config: (" + DEFAULT_CONFIG_RESOURCE + ")");
            logger.info("Map Size: {} x {}", config.mapWidth(), config.mapHeight());
            logger.info("Delimiter: '" + config.inputDelimiter() + "'");
            logger.info("Skip Header Lines: " + config.inputSkipHeaderLines());
            logger.info("Render HTML output : " + config.htmlEnabled());
            ;

            Bounds bounds;
            logger.info("Finding data bounds...");
            long densityGrid[][];
            if (config.enableParallelProcessing()) {
                bounds = GetDatBounds.findDataBoundsConcurrently(config);
                densityGrid = GridDensityPopulator.populateGridConcurrently(config, bounds);

            } else {
                bounds = GetDatBounds.findDataBounds(config);
                densityGrid = GridDensityPopulator.populate(config, bounds);
            }
            RenderAsciiMap.renderOutputAsciiMap(densityGrid, config, bounds);
            ;

        } catch (ConfigurationException e) {
            logger.error("FATAL: Error loading configuration file '{}': {}", DEFAULT_CONFIG_RESOURCE, e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            logger.error("FATAL: Invalid configuration value in '{}': {}", DEFAULT_CONFIG_RESOURCE, e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            logger.error("Error during file processing: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            logger.error("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Loads application configuration from a properties file located on the
     * classpath.
     * <p>
     * Uses Apache Commons Configuration to read the properties file and validates
     * essential configuration values.
     *
     * @param configurationResourceName The name of the properties file resource
     *                                  (e.g., "plotter.properties").
     * @return A {@link Config} object populated with the settings, or {@code null}
     *         if
     *         loading or validation fails (errors are logged).
     * @throws ConfigurationException   If the configuration file cannot be loaded.
     * @throws IllegalArgumentException If essential configuration properties are
     *                                  missing or invalid.
     */
    private static Config loadConfig(String configurationResourceName)
            throws ConfigurationException, IllegalArgumentException {
        Configurations configs = new Configurations();
        Configuration configData = null;
        logger.info("Loading configuration from classpath resource: {}", configurationResourceName);

        configData = configs.properties(configurationResourceName);
        String filePath = configData.getString("input.file_path");
        int width = configData.getInt("map.width", 0);
        int height = configData.getInt("map.height", 0);
        String delimiter = configData.getString("input.delimiter");
        int skipLines = configData.getInt("input.skip_header_lines");
        String densityCharsStr = configData.getString("render.density_chars");
        int latColumn = configData.getInt("input.lat_column");
        int longColumn = configData.getInt("input.long_column");
        boolean htmlEnabled = configData.getBoolean("render.html_enabled");
        String htmlFilePath = configData.getString("render.file_path");
        long errorCount = configData.getLong("log.error_count");
        boolean enableParallelProcessing = configData.getBoolean("enable.parallel.processing");

        if (filePath == null || filePath.trim().isEmpty())
            throw new IllegalArgumentException("Missing required configuration property: input.file_path");
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Map width and height must be >0.");
        if (skipLines < 0)
            throw new IllegalArgumentException("input.skip_header_lines cannot be negative.");
        if (densityCharsStr == null || densityCharsStr.length() < 3)
            throw new IllegalArgumentException("render.density_chars must contain at least 2 characters.");
        if (latColumn < 0 || longColumn < 0 || latColumn == longColumn) {
            throw new IllegalArgumentException(
                    "input.lat_column and input.long_column must be non-negative and different.");
        }
        return new Config(
                filePath.trim(), width, height, delimiter, skipLines,
                densityCharsStr.toCharArray(),
                latColumn, longColumn, htmlEnabled, htmlFilePath, errorCount, enableParallelProcessing);

    }
}