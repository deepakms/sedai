package co.sedai;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.sedai.model.Bounds;
import co.sedai.model.Config;

/**
 * Utility class to find the geographical bounds (min/max latitude and
 * longitude)
 * from a data file based on the provided configuration.
 */
public class GetDatBounds {

    private static final Logger logger = LoggerFactory.getLogger(GetDatBounds.class);

    /**
     * Reads the input file according to the configuration and determines the
     * min/msx latitude and longitude of the valid data points.
     *
     * @param config The application configuration file
     * @return A Bounds object containing the calculated min/max lat/lon and point
     *         count.
     * @throws IOException If an error occurs reading the input file.
     */

    public static Bounds findDataBounds(Config config) throws IOException {
        Bounds bounds = new Bounds();
        long lineNum = 0;
        long errorCount = 0;
        String delimiter = config.inputDelimiter();
        long startTime = System.nanoTime();
        final long loggingErrorCount = config.errorCount() == -1 ? Long.MAX_VALUE : config.errorCount();

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
                if (parts.length < 4) {
                    if (errorCount <= loggingErrorCount) {
                        logger.warn("(Pass 1, Line {}): Skipping invalid line. Expected 4 columns, found {}",
                                lineNum, parts.length);
                    }
                }

                try {
                    double lat = Double.parseDouble(parts[config.latColumn()].trim());
                    double lon = Double.parseDouble(parts[config.longColumn()].trim());
                    if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                        if (errorCount <= loggingErrorCount) {
                            logger.warn(
                                    "(Pass 1, Line {}): Skipping out of range coord (Lat:{},Lon:{})",
                                    lineNum, lat, lon);
                        }
                        errorCount++;
                        continue;
                    }
                    bounds.minLat = Math.min(bounds.minLat, lat);
                    bounds.maxLat = Math.max(bounds.maxLat, lat);
                    bounds.minLon = Math.min(bounds.minLon, lon);
                    bounds.maxLon = Math.max(bounds.maxLon, lon);
                    bounds.pointCount++;
                } catch (NumberFormatException e) {
                    if (errorCount <= loggingErrorCount)
                        Main.logger.warn("(Pass 1, Line %d): Skipping non-numeric: %s (%s)", lineNum, line,
                                e.getMessage());
                    errorCount++;
                } catch (ArrayIndexOutOfBoundsException e) {
                    if (errorCount <= loggingErrorCount)
                        Main.logger.warn("(Pass 1, Line %d): Skipping bad format: %s", lineNum, line);
                    errorCount++;
                }
            }
        }
        if (errorCount > loggingErrorCount)
            logger.warn("Encountered {} total parse errors (first {} shown).", errorCount, loggingErrorCount);
        else if (errorCount > 0)
            System.err.printf("Encountered {} total parse errors.", errorCount);

        validateBounds(bounds);
        long endTime = System.nanoTime();
        logger.info("Sequential bounds processing completed in {} seconds.", (endTime - startTime) / 1_000_000_000.0);
        logger.info("Bounds found using sequential processing : {}", bounds.toString());
        return bounds;

    }

    public static Bounds findDataBoundsConcurrently(Config config) throws IOException {
        logger.info("Finding data bounds using parallel stream...");
        long startTime = System.nanoTime();
        final AtomicLong totalErrorCount = new AtomicLong(0);

        BoundsAccumulator finalAccumulator;
        try (Stream<String> lines = Files.lines(Paths.get(config.filePath()))) {
            finalAccumulator = lines.parallel()
                    .skip(config.inputSkipHeaderLines())
                    .map(line -> {
                        if (line == null)
                            return null;
                        String trimmedLine = line.trim();
                        if (trimmedLine.isEmpty())
                            return null;

                        String[] parts = trimmedLine.split(config.inputDelimiter());

                        try {
                            double lat = Double.parseDouble(parts[config.latColumn()].trim());
                            double lon = Double.parseDouble(parts[config.longColumn()].trim());
                            if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                                logger.warn(
                                        "(Pass 1): Skipping out of range coord (Lat:{},Lon:{})",
                                        lat, lon);
                                return null;
                            }
                            return new LatLon(lat, lon);
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                            if (totalErrorCount.incrementAndGet() <= config.errorCount()) {
                                logger.warn("Warning (Pass 1 Parallel): Skipping invalid coordinate data: {} ({})",
                                        trimmedLine, e.getMessage());
                            }
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(BoundsAccumulator::new, // Supplier: creates new accumulator
                            BoundsAccumulator::accept, // Accumulator: adds a LatLon point
                            BoundsAccumulator::combine); // Combiner: merges two accumulators
        } catch (IOException e) {
            logger.error("Error reading data file '{}' during parallel bounds calculation: {}", config.filePath(),
                    e.getMessage(), e);
            throw e;
        }

        long endTime = System.nanoTime();
        logger.info("Parallel bounds finding completed in {} seconds.", (endTime - startTime) / 1_000_000_000.0);

        long errors = totalErrorCount.get();
        if (errors > 0) {
            logger.warn("Encountered {} total parsing errors during parallel bounds finding (logged up to {}).", errors,
                    config.errorCount());
        }

        // Handle case where no valid points were found
        if (finalAccumulator.pointCount == 0) {
            logger.warn("No valid data points found in the file. Using default bounds [0,0,0,0].");
            return new Bounds(0, 0, 0, 0, 0);
        }
        Bounds bounds = new Bounds(
                finalAccumulator.minLat, finalAccumulator.maxLat,
                finalAccumulator.minLon, finalAccumulator.maxLon,
                finalAccumulator.pointCount);
        logger.info("Bounds found using parallel processing : {}", bounds.toString());
        return bounds;

    }

    private static void validateBounds(Bounds bounds) {
        if (!bounds.isValid()) {
            logger.error("No valid coordinate data found in the file matching config criteria.");
            System.exit(1);
        }
        if (!bounds.hasRange()) {
            logger.warn(
                    "All valid points are identical or very close.");
        }
    }

    private record LatLon(double lat, double lon) {
    }

    private static class BoundsAccumulator {
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        long pointCount = 0;

        void accept(LatLon point) {
            if (point != null) {
                minLat = Math.min(minLat, point.lat());
                maxLat = Math.max(maxLat, point.lat());
                minLon = Math.min(minLon, point.lon());
                maxLon = Math.max(maxLon, point.lon());
                pointCount++;
            }
        }

        BoundsAccumulator combine(BoundsAccumulator other) {
            minLat = Math.min(minLat, other.minLat);
            maxLat = Math.max(maxLat, other.maxLat);
            minLon = Math.min(minLon, other.minLon);
            maxLon = Math.max(maxLon, other.maxLon);
            pointCount += other.pointCount;
            return this;
        }
    }
}
