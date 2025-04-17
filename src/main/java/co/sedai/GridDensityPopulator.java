package co.sedai;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.sedai.model.Bounds;
import co.sedai.model.Config;

/**
 * Handles the second pass of the plotting process. This class reads
 * geographical
 * point data (latitude, longitude) from a specified file, calculates which cell
 * each point falls into within a predefined grid based on calculated
 * {@link Bounds},
 * and increments the count for that cell. The result is a 2D array representing
 * the density of points across the geographical area.
 */
public class GridDensityPopulator {

    private final Config config;
    private final Bounds bounds;
    public static Logger logger = LoggerFactory.getLogger(GridDensityPopulator.class);

    public GridDensityPopulator(Config config, Bounds bounds) {
        this.config = config;
        this.bounds = bounds;
    }

    /**
     * Reads the data file specified in the configuration, processes each line
     * containing
     * latitude and longitude, maps each valid point to a cell in a 2D grid, and
     * counts
     * the number of points per cell.
     * <p>
     * Skips header lines as configured. Validates input lines based on the
     * delimiter
     * and expected number of columns. Parses latitude and longitude, skipping lines
     * with non-numeric data or points falling outside the pre-calculated
     * {@code bounds}.
     * Handles edge cases where the latitude or longitude range is zero.
     *
     * @return A 2D long array (`long[mapHeight][mapWidth]`) where each element
     *         represents the count of points that fall into the corresponding grid
     *         cell.
     * @throws IOException If an error occurs while reading the input data file.
     */
    public long[][] populate() throws IOException {
        long[][] grid = new long[config.mapHeight()][config.mapWidth()];
        logger.info("Populating desntiy grids");
        double latRange = bounds.maxLat() - bounds.minLat();
        double lonRange = bounds.maxLon() - bounds.minLon();
        long pointsProcessed = 0;
        long lineNum = 0;
        boolean singleLat = latRange == 0.0;
        boolean singleLon = lonRange == 0.0;
        int mapWidth = config.mapWidth();
        int mapHeight = config.mapHeight();
        String filePath = config.filePath();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            for (int i = 0; i < config.inputSkipHeaderLines() && (line = reader.readLine()) != null; i++) {
                lineNum++;
            }

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String delimiter = config.inputDelimiter();

                String[] parts = line.split(delimiter);

                if (parts.length < 2) {

                    logger.warn(
                            "Warning (Pass 2, Line {}): Skipping invalid line (expected delimiter '{}'): {}",
                            lineNum, config.inputDelimiter(), line);

                    continue;
                }

                try {
                    double lat = Double.parseDouble(parts[config.latColumn()].trim());
                    double lon = Double.parseDouble(parts[config.longColumn()].trim());
                    int gridX, gridY;
                    // Check if the point falls within the calculated bounds.
                    // Points outside the bounds determined in the first pass are skipped.
                    if (lat < bounds.minLat() || lat > bounds.maxLat() || lon < bounds.minLon()
                            || lon > bounds.maxLon()) {

                        logger.warn(
                                "Info (Pass 2, Line %d): Skipping point outside fixed bounds (Lat: {}, Lon: {})",
                                lineNum, lat, lon);
                        continue;
                    }
                    // Calculate the X coordinate (longitude -> column)

                    if (singleLon)
                        // If all points have the same longitude, place them in the middle column
                        gridX = mapWidth / 2;
                    else
                        // Map longitude to grid column index
                        // Formula: ((current_lon - min_lon) / total_lon_range) * map_width
                        gridX = (int) (((lon - bounds.minLon()) / lonRange) * mapWidth);
                    // Calculate the Y coordinate (latitude -> row)

                    if (singleLat)
                        // If all points have the same latitude, place them in the middle row
                        gridY = mapHeight / 2;
                    else
                        // Map latitude to grid row index
                        // Formula: ((max_lat - current_lat) / total_lat_range) * map_height
                        gridY = (int) (((bounds.maxLat() - lat) / latRange) * mapHeight);
                    // Clamp coordinates to ensure they are within the valid grid array bounds [0, width-1] and [0, height-1]
                    gridX = Math.max(0, Math.min(mapWidth - 1, gridX));
                    gridY = Math.max(0, Math.min(mapHeight - 1, gridY));
                    
                    // Increment the count for the calculated grid cell
                    grid[gridY][gridX]++;
                    pointsProcessed++;

                } catch (NumberFormatException e) {

                    logger.warn("Warning (Pass 2, Line {}): Skipping non-numeric data: {} ({})", lineNum,
                            line, e.getMessage());

                } catch (ArrayIndexOutOfBoundsException e) {

                    logger.warn("Warning (Pass 2, Line {}): Skipping invalid line format: {}", lineNum,
                            line);

                }
            }
            logger.info("Processed {} points during grid population.",
                    pointsProcessed);
        } catch (IOException e) {
            logger.error("Error during file processing: " + e.getMessage());
            throw e;
        }
        return grid;
    }
}