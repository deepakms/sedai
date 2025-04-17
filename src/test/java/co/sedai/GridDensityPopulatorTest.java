package co.sedai;

import co.sedai.model.Bounds;
import co.sedai.model.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GridDensityPopulatorTest {

    private Config testConfig;
    private Bounds testBounds;

    // Use a temporary directory for test files if needed, but reading from resources is often easier
    // @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        
         char[] densityChars = " .123".toCharArray();
         // Path to the test CSV in resources
         String testCsvPath = getClass().getClassLoader().getResource("test-points.csv").getPath();

         testConfig = new Config(
                testCsvPath, // Use path from resources
                10, 5, ",", 1, densityChars,
                0, 1, true, "./map.html" , 10// lat=col 0, lon=col 1
        );

        // // Bounds matching the data in test-points.csv (excluding header and out-of-bounds points)
        // // MinLat=1.0, MaxLat=9.0, MinLon=1.0, MaxLon=9.0, PointCount=6 (calculated manually for test)
        testBounds = new Bounds();
        testBounds.maxLat = 9.0;
        testBounds.minLat =1.0;
        testBounds.maxLon = 9.0;
        testBounds.minLon = 1.0;
        testBounds.pointCount = 6;
    }

    @Test
    void populate_SimpleGrid() throws IOException {
        GridDensityPopulator populator = new GridDensityPopulator(testConfig, testBounds);
        long[][] grid = populator.populate();

        assertNotNull(grid);
        assertEquals(testConfig.mapHeight(), grid.length, "Grid height should match config");
        assertEquals(testConfig.mapWidth(), grid[0].length, "Grid width should match config");

        // --- Assert specific cell counts based on test-points.csv and bounds ---
        // Map Height = 5, Map Width = 10
        // Lat Range = 9.0 - 1.0 = 8.0
        // Lon Range = 9.0 - 1.0 = 8.0

        // Point: 1.0, 1.0 (Lat, Lon) -> Top-Left Area
        // gridY = floor(((9.0 - 1.0) / 8.0) * 5) = floor(1.0 * 5) = 5 -> clamped to 4 (max index)
        // gridX = floor(((1.0 - 1.0) / 8.0) * 10) = floor(0.0 * 10) = 0
        assertEquals(1, grid[4][0], "Point (1.0, 1.0) should be in grid[4][0]");

        // Point: 9.0, 9.0 -> Bottom-Right Area
        // gridY = floor(((9.0 - 9.0) / 8.0) * 5) = floor(0.0 * 5) = 0
        // gridX = floor(((9.0 - 1.0) / 8.0) * 10) = floor(1.0 * 10) = 10 -> clamped to 9 (max index)
        assertEquals(1, grid[0][9], "Point (9.0, 9.0) should be in grid[0][9]");

        // Points: 5.0, 5.0 and 5.1, 5.1 -> Center Area (should land in same cell)
        // gridY (5.0) = floor(((9.0 - 5.0) / 8.0) * 5) = floor(0.5 * 5) = floor(2.5) = 2
        // gridX (5.0) = floor(((5.0 - 1.0) / 8.0) * 10) = floor(0.5 * 10) = floor(5.0) = 5
        // gridY (5.1) = floor(((9.0 - 5.1) / 8.0) * 5) = floor(3.9 / 8.0 * 5) = floor(0.4875 * 5) = floor(2.4375) = 2
        // gridX (5.1) = floor(((5.1 - 1.0) / 8.0) * 10) = floor(4.1 / 8.0 * 10) = floor(0.5125 * 10) = floor(5.125) = 5
        assertEquals(2, grid[2][5], "Points (5.0, 5.0) and (5.1, 5.1) should be in grid[2][5]");

        // Point: 1.5, 8.5 -> Top-Right Area
        // gridY = floor(((9.0 - 1.5) / 8.0) * 5) = floor(7.5 / 8.0 * 5) = floor(0.9375 * 5) = floor(4.6875) = 4
        // gridX = floor(((8.5 - 1.0) / 8.0) * 10) = floor(7.5 / 8.0 * 10) = floor(0.9375 * 10) = floor(9.375) = 9
        assertEquals(1, grid[4][9], "Point (1.5, 8.5) should be in grid[4][9]");

        // Point: 8.5, 1.5 -> Bottom-Left Area
        // gridY = floor(((9.0 - 8.5) / 8.0) * 5) = floor(0.5 / 8.0 * 5) = floor(0.0625 * 5) = floor(0.3125) = 0
        // gridX = floor(((1.5 - 1.0) / 8.0) * 10) = floor(0.5 / 8.0 * 10) = floor(0.0625 * 10) = floor(0.625) = 0
        assertEquals(1, grid[0][0], "Point (8.5, 1.5) should be in grid[0][0]");

        // Check a few empty cells
        assertEquals(0, grid[0][1], "Cell grid[0][1] should be empty");
        assertEquals(0, grid[2][2], "Cell grid[2][2] should be empty");
        assertEquals(0, grid[4][4], "Cell grid[4][4] should be empty");

        // Verify total count matches points within bounds
        long totalGridCount = 0;
        for (long[] row : grid) {
            for (long cell : row) {
                totalGridCount += cell;
            }
        }
        assertEquals(testBounds.pointCount(), totalGridCount, "Total count in grid should match bounds point count");
    }

     @Test
     void populate_HandlesEmptyFile(@TempDir Path tempDir) throws IOException {
         // Create an empty file
         File emptyFile = tempDir.resolve("empty.csv").toFile();
         emptyFile.createNewFile();

         Config emptyConfig = new Config(
                emptyFile.getAbsolutePath(),
                10, 5, ",", 0, " .".toCharArray(), 0, 1,true, "./map.html",10
         );
         Bounds emptyBounds = new Bounds();
         testBounds.maxLat = 0.0;
         testBounds.minLat =0.0;
         testBounds.maxLon = 0.0;
         testBounds.minLon = 0.0;

         GridDensityPopulator populator = new GridDensityPopulator(emptyConfig, emptyBounds);
         long[][] grid = populator.populate();

         // Verify grid is all zeros
         for (long[] row : grid) {
             for (long cell : row) {
                 assertEquals(0, cell);
             }
         }
     }
}
