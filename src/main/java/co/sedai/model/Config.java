package co.sedai.model;

public record Config(
        String filePath,
        int mapWidth,
        int mapHeight,
        String inputDelimiter,
        int inputSkipHeaderLines,
        char[] renderDensityChars,
        int latColumn,
        int longColumn,
        boolean htmlEnabled,
        String htmlFilePath,
        long errorCount,
        boolean enableParallelProcessing

) {

}
