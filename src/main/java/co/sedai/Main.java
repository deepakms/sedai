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
 * This class orchestrates the process of plotting geographical point data onto a
 * text-based map. It performs the following steps:
 * <ol>
 *     <li>Loads configuration settings from a properties file ({@code plotter.properties}).</li>
 *     <li>Performs a first pass over the data file to determine the geographical bounds (min/max latitude and longitude) using {@link GetDatBounds}.</li>
 *     <li>Performs a second pass over the data file to populate a 2D grid representing point density within the calculated bounds using {@link GridDensityPopulator}.</li>
 *     <li>Renders the populated grid as an ASCII map to the console (via logger) using {@link RenderAsciiMap}.</li>
 * </ol>
 * The application exits with a non-zero status code if configuration loading fails or
 * if errors occur during file processing.
 */

public class Main {
    static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_CONFIG_RESOURCE = "plotter.properties";

    public static void main(String[] args) {

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

        try {
            Bounds bounds;
            logger.info("Finding data bounds...");
            GetDatBounds dataBoundsFinder = new GetDatBounds();
            bounds = dataBoundsFinder.findDataBounds(config);
            logger.info(bounds.toString());
            logger.info("Populating grid...");
            GridDensityPopulator populator = new GridDensityPopulator(config, bounds);
            long densityGrid[][] = populator.populate();
            RenderAsciiMap.renderMap(densityGrid, config, bounds);
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
     * Loads application configuration from a properties file located on the classpath.
     * <p>
     * Uses Apache Commons Configuration to read the properties file and validates
     * essential configuration values.
     *
     * @param configurationResourceName The name of the properties file resource (e.g., "plotter.properties").
     * @return A {@link Config} object populated with the settings, or {@code null} if
     *         loading or validation fails (errors are logged).
     */
    private static Config loadConfig(String configurationResourceName) {
        Configurations configs = new Configurations();
        Configuration configData = null;
        logger.info("Loading configuration from classpath resource: {}", configurationResourceName);
        try {
            configData = configs.properties(configurationResourceName);
            String filePath = configData.getString("input.file_path");
            int width = configData.getInt("map.width");
            int height = configData.getInt("map.height");
            String delimiter = configData.getString("input.delimiter");
            int skipLines = configData.getInt("input.skip_header_lines");
            String densityCharsStr = configData.getString("render.density_chars");
            boolean useFixed = configData.getBoolean("bounds.use_fixed");
            double fixedMinLat = configData.getDouble("bounds.fixed.min_lat");
            double fixedMaxLat = configData.getDouble("bounds.fixed.max_lat");
            double fixedMinLon = configData.getDouble("bounds.fixed.min_lon");
            double fixedMaxLon = configData.getDouble("bounds.fixed.max_lon");
            int latColumn = configData.getInt("input.lat_column");
            int longColumn = configData.getInt("input.long_column");

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
            if (useFixed && (fixedMinLat >= fixedMaxLat || fixedMinLon >= fixedMaxLon)) {
                throw new IllegalArgumentException("Fixed bounds are invalid: min must be less than max.");
            }
            return new Config(
                    filePath.trim(), width, height, delimiter, skipLines,
                    densityCharsStr.toCharArray(), fixedMinLat, fixedMaxLat, fixedMinLon, fixedMaxLon,
                    latColumn, longColumn);
        } catch (ConfigurationException e) {
            logger.error("Error loading configuration from classpath resource: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) { 
            logger.error("Error processing configuration value from classpath resource: {}", e.getMessage());
            return null;
        }
    }
}