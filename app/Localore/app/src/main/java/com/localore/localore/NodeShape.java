package com.localore.localore;


import java.io.Serializable;
import java.util.List;

/**
 * Representation of a shape as an order list of nodes (lon,lat).
 *
 * @inv No segment crossings.
 * @inv No lon=180-crossings.
 * @inv Number of points > 0.
 */
public class NodeShape implements Serializable {

    /**
     * [lon lat]. */
    private List<double[]> nodes;

    /**
     * New shape from well-formed list of nodes.
     */
    public NodeShape(List<double[]> ns) {
        this.nodes = ns;
    }

    /**
     * @return [lon lat]
     */
    public List<double[]> getNodes() {
        return this.nodes;
    }

    /**
     * [lon lat] of first node.
     */
    public double[] getFirst() {
        return this.nodes.get(0);
    }

    /**
     * [lon lat] of last node.
     */
    public double[] getLast() {
        return this.nodes.get(size()-1);
    }

    /**
     * @return True if p[0] == p[1].
     */
    public boolean isClosed() {
        double endPointsDist = distance(getFirst(), getLast());
        return endPointsDist < 0.0001;
    }

    /**
     * @return Number of nodes.
     */
    public int size() {
        return this.nodes.size();
    }

    /**
     * @return Sum of all segment-lengths.
     */
    public double getLength() {
        double sum = 0;
        for (int i = 0; i < this.nodes.size()-1; i++)
            sum += distance(nodes.get(i), nodes.get(i+1));
        return sum;
    }

    /**
     * @return [wsen]
     */
    public double[] getBounds() {
        double w = Double.POSITIVE_INFINITY;
        double s = Double.POSITIVE_INFINITY;
        double e = Double.NEGATIVE_INFINITY;
        double n = Double.NEGATIVE_INFINITY;

        for (double[] node : this.nodes) {
            if (node[0] < w) w = node[0];
            if (node[0] > e) e = node[0];
            if (node[1] < s) s = node[1];
            if (node[1] > n) n = node[1];
        }
        return new double[]{w, s, e, n};
    }

    /**
     * @return "lat1 lon1 lat2 lon2 lat3 lon3 ..."
     * Note: lat lon, not lon lat.
     */
    public String toRawString() {
        StringBuilder sb = new StringBuilder();
        for (double[] n : this.nodes)
            sb.append(n[1] + " " + n[0] + " ");
        String raw = sb.toString();
        return raw.substring(0, raw.length()-1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (double[] n : this.nodes)
            sb.append(n[1] + "," + n[0] + "\n");
        return sb.toString();
    }

    /**
     * @param lon,lat
     * @return Distance in meters between points.
     */
    public static double distance(double[] lon, double[] lat) {
        double lon1 = lon[0]; double lat1 = lon[1];
        double lon2 = lat[0]; double lat2 = lat[1];

        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lon2-lon1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;

        return dist;
    }
}