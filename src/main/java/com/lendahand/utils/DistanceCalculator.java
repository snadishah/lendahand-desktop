package com.lendahand.utils;

/**
 * Pure-Java implementation of the Haversine formula.
 * Calculates great-circle distance between two GPS coordinates.
 */
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Returns the distance in kilometres between two lat/lon points.
     *
     * @param lat1 latitude  of point A (degrees)
     * @param lon1 longitude of point A (degrees)
     * @param lat2 latitude  of point B (degrees)
     * @param lon2 longitude of point B (degrees)
     */
    public static double calculateDistance(double lat1, double lon1,
                                           double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
