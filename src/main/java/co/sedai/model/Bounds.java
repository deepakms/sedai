package co.sedai.model;

public class Bounds {
    public double minLat = Double.POSITIVE_INFINITY;
    public double maxLat = Double.NEGATIVE_INFINITY;
    public double minLon = Double.POSITIVE_INFINITY;
    public double maxLon = Double.NEGATIVE_INFINITY;
    public long pointCount = 0;

    public Bounds() {
        this(0, 0, 0, 0, 0);
    }
    
    public Bounds(double minLat, double maxLat, double minLon, double maxLon, long pointCount){
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.pointCount = pointCount;
    }
    public double minLat() {
        return minLat;
    }

    public double maxLat() {
        return maxLat;
    }

    public double minLon() {
        return minLon;
    }

    public double maxLon() {
        return maxLon;
    }

    public long pointCount() {
        return pointCount;
    }

    public boolean isValid() {
        return pointCount > 0 &&
                minLat != Double.POSITIVE_INFINITY &&
                maxLat != Double.NEGATIVE_INFINITY &&
                minLon != Double.POSITIVE_INFINITY &&
                maxLon != Double.NEGATIVE_INFINITY;
    }


    public boolean hasRange() {
        return isValid() && (maxLat > minLat || maxLon > minLon);
    }

    @Override
    public String toString() {
        return String.format("Bounds [Lat: %.4f to %.4f, Lon: %.4f to %.4f], Points: %d",
                minLat, maxLat, minLon, maxLon, pointCount);
    }
}