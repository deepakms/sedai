package co.sedai.model;

public record Config(
        String filePath,
        int mapWidth,
        int mapHeight,
        String inputDelimiter,
        int inputSkipHeaderLines,
        char[] renderDensityChars,
        double boundsFixedMinLat,
        double boundsFixedMaxLat,
        double boundsFixedMinLon,
        double boundsFixedMaxLon,
        int latColumn,
        int longColumn

) {

}
