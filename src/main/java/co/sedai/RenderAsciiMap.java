package co.sedai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.sedai.model.Bounds;
import co.sedai.model.Config;

/**
 * Utility class responsible for rendering a 2D data grid (representing point density
 * within geographical bounds) as a text-based ASCII map.
 * <p>
 * Includes map borders, coordinate labels derived from {@link Bounds}, and a legend
 * explaining the density characters defined in {@link Config}.
 */

public final class RenderAsciiMap {
    static final Logger logger = LoggerFactory.getLogger(RenderAsciiMap.class);

    /**
     * Renders the provided data grid as a formatted ASCII map, logging the result.
     * <p>
     * The map includes:
     * <ul>
     *     <li>Top/Bottom borders with North/South latitude labels.</li>
     *     <li>Left/Right borders with West/East longitude labels (centered vertically).</li>
     *     <li>Characters representing the density of points in each grid cell, based on {@code config.renderDensityChars()}.</li>
     *     <li>A legend explaining the point count ranges for each density character.</li>
     * </ul>
     * If the grid contains no points (maxCount is 0) and the input point count was also 0,
     * a specific message is printed to System.out.
     * Otherwise, the fully constructed map string is logged at the INFO level via SLF4J.
     *
     * @param grid   The 2D array (long[y][x]) holding the count for each cell. y corresponds to latitude rows, x to longitude columns.
     * @param config The application configuration, providing map dimensions ({@link Config#mapHeight()},
     *               {@link Config#mapWidth()}) and density characters ({@link Config#renderDensityChars()}).
     * @param bounds The geographical bounds of the map area, used for axis labels
     *               ({@link Bounds#maxLat()}, {@link Bounds#minLat()}, {@link Bounds#minLon()}, {@link Bounds#maxLon()}).
     */
    static void renderMap(long[][] grid, Config config, Bounds bounds) {
        long maxCount = 0;
        int mapHeight = config.mapHeight();
        int mapWidth = config.mapWidth();
        char[] densityChars = config.renderDensityChars();
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                maxCount = Math.max(maxCount, grid[y][x]);
            }
        }
        logger.info("Max points per cell: {}", maxCount);
        if (maxCount == 0 && bounds.pointCount() == 0) {
            System.out.println("(Map is empty or no points fell within the fixed bounds)");
            return;
        }

        StringBuilder mapBuilder = new StringBuilder(mapHeight * mapWidth + mapWidth * 4 + 200);
        mapBuilder.append('\n');
        mapBuilder.append(String.format("      %.4f N\n", bounds.maxLat()));
        mapBuilder.append("         +");
        for (int x = 0; x < mapWidth; x++)
            mapBuilder.append('-');
        mapBuilder.append("+\n");

        for (int y = 0; y < mapHeight; y++) {
            if (y == mapHeight / 2)
                mapBuilder.append(String.format("%.3f W |", bounds.minLon()));
            else
                mapBuilder.append("         |");

            for (int x = 0; x < mapWidth; x++) {
                mapBuilder.append(getDensityChar(grid[y][x], maxCount, config.renderDensityChars()));
            }

            if (y == mapHeight / 2)
                mapBuilder.append(String.format("| %.3f E", bounds.maxLon()));
            else
                mapBuilder.append('|');
            mapBuilder.append('\n');
        }

        mapBuilder.append("         +");
        for (int x = 0; x < mapWidth; x++)
            mapBuilder.append('-');
        mapBuilder.append("+\n");
        mapBuilder.append(String.format("      %.4f S\n", bounds.minLat()));
        mapBuilder.append("Legend (Points per cell):");
        mapBuilder.append(String.format("'%c': 0 ", densityChars[0]));
        int numLevels = densityChars.length - 1;
        if (numLevels > 0 && maxCount > 0) {
            long prevThreshold = 0;
            for (int i = 1; i <= numLevels; i++) {
                long threshold = (long) Math.ceil((double) maxCount * i / numLevels);
                if (threshold <= prevThreshold)
                    threshold = prevThreshold + 1;
                if (prevThreshold + 1 == threshold)
                    mapBuilder.append(String.format(" '%c': %d", densityChars[i], threshold));
                else
                    mapBuilder.append(String.format(" '%c': %d-%d", densityChars[i], prevThreshold + 1, threshold));
                prevThreshold = threshold;
            }
        } else if (maxCount == 1 && densityChars.length > 1)
            mapBuilder.append(String.format(" '%c': 1", densityChars[1]));
        logger.info("{}", mapBuilder.toString());
    }
    /**
     * Selects the appropriate character from the density character array to represent
     * the density level of a single grid cell.
     * <p>
     * The selection is based on the ratio of the cell's count to the maximum count
     * found in the grid, scaled to the number of available density levels (excluding
     * the character for zero count, typically at index 0).
     *
     * @param count        The count value for the specific grid cell.
     * @param maxCount     The maximum count found in any cell of the grid. Used for scaling.
     *                     If maxCount is 0 or less, but count is positive, the first density level char is used.
     * @param densityChars An array of characters used for rendering density. Index 0 is
     *                     assumed to be for zero count (e.g., ' '), and subsequent indices
     *                     represent increasing density levels. Must contain at least 2 characters.
     * @return The character representing the density level. Returns the character at index 0
     *         (e.g., ' ') if {@code count <= 0}. Returns '?' if {@code densityChars} has fewer than 2 elements.
     *         Returns the character at index 1 if {@code maxCount <= 0} but {@code count > 0}.
     *         Otherwise, returns the character at the calculated density level index (clamped between 1 and length-1).
     */
    static char getDensityChar(long count, long maxCount, char[] densityChars) {
        if (count <= 0)
            return ' ';
        int numLevels = densityChars.length - 1;
        // Handle case where there are points, but maxCount is somehow zero (shouldn't happen if count > 0)
        // or if maxCount is 1. Use the first density level character (index 1).
        if (maxCount <= 0)
            return densityChars[1];
        // Calculate the density fraction (0.0 to 1.0]
        double fraction = (double) count / maxCount;
        // Scale the fraction to the number of available density levels and find the ceiling index
        // Example: 5 levels (indices 1-5). fraction 0.1 -> ceil(0.1*5)=1. fraction 1.0 -> ceil(1.0*5)=5.
        int index = (int) Math.ceil(fraction * numLevels);
        // Clamp the index to be within the valid range of density characters [1, numLevels]
        index = Math.max(1, Math.min(numLevels, index));
        
        return densityChars[index];
    }

}
