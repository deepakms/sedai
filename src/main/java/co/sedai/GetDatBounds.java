package co.sedai;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.sedai.model.Bounds;
import co.sedai.model.Config;

/**
 * Utility class to find the geographical bounds (min/max latitude and longitude)
 * from a data file based on the provided configuration.
 */
public class GetDatBounds {
    private Bounds bounds = new Bounds();
    private static final Logger logger = LoggerFactory.getLogger(GetDatBounds.class);

    /**
     * Reads the input file according to the configuration and determines the
     * min/msx latitude and longitude of the valid data points.
     *
     * @param config The application configuration file
     * @return A Bounds object containing the calculated min/max lat/lon and point count.
     * @throws IOException If an error occurs reading the input file.
     */
    
    public Bounds findDataBounds(Config config) throws IOException {
        long lineNum = 0;
        long errorCount = 0;
        String delimiter = config.inputDelimiter();
        
    
        try (BufferedReader reader = new BufferedReader(new FileReader(config.filePath()))) {
            String line;
            for (int i = 0; i < config.inputSkipHeaderLines() && (line = reader.readLine()) != null; i++) {
                lineNum++;
            }
    
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty())
                    continue;
    
                String[] parts = line.split(delimiter);
                if (parts.length < 2) {
                    if (errorCount < 10)
                        logger.warn("(Pass 1, Line {}): Skipping invalid line (delimiter '{}'): {}",
                                lineNum, config.inputDelimiter(), line);
                    errorCount++;
                    continue;
                }
                try {
                    double lat = Double.parseDouble(parts[config.latColumn()].trim());
                    double lon = Double.parseDouble(parts[config.longColumn()].trim());
                    if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                        if (errorCount < 10)
                            logger.warn(
                                    "(Pass 1, Line {}): Skipping out of range coord (Lat:{},Lon:{})",
                                    lineNum, lat, lon);
                        errorCount++;
                        continue;
                    }
                    bounds.minLat = Math.min(bounds.minLat, lat);
                    bounds.maxLat = Math.max(bounds.maxLat, lat);
                    bounds.minLon = Math.min(bounds.minLon, lon);
                    bounds.maxLon = Math.max(bounds.maxLon, lon);
                    bounds.pointCount++;
                } catch (NumberFormatException e) {
                    if (errorCount < 10)
                        Main.logger.warn("(Pass 1, Line %d): Skipping non-numeric: %s (%s)", lineNum, line,
                                e.getMessage());
                    errorCount++;
                } catch (ArrayIndexOutOfBoundsException e) {
                    if (errorCount < 10)
                        Main.logger.warn("(Pass 1, Line %d): Skipping bad format: %s", lineNum, line);
                    errorCount++;
                }
            }
        }
        if (errorCount > 10)
            logger.warn("Encountered {} total parse errors (first 10 shown).", errorCount);
        else if (errorCount > 0)
            System.err.printf("Encountered {} total parse errors.", errorCount);
        validateBounds();
        return bounds;
    }

    private void validateBounds(){
        if (!bounds.isValid()) {
            logger.error("No valid coordinate data found in the file matching config criteria.");
            System.exit(1);
        }
        if (!bounds.hasRange()) {
            logger.warn(
                    "All valid points are identical or very close.");
        }
    }       
}
