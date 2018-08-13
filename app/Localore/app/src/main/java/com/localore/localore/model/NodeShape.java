package com.localore.localore.model;


import java.io.Serializable;
import java.util.ArrayList;
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
     * New shape from well-formed list of nodes [lon lat].
     */
    public NodeShape(List<double[]> ns) {
        this.nodes = new ArrayList<>(ns);
    }

    /**
     * @return [lon lat]
     */
    public List<double[]> getNodes() {
        return new ArrayList<>(this.nodes);
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
        return endPointsDist < 0.00000001;
    }

    /**
     * @return Number of nodes.
     */
    public int size() {
        return this.nodes.size();
    }

    /**
     * @return True if shape is a node.
     */
    public boolean isNode() {
        return size() == 1;
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

    //region valid nodes: no intersection

    /**
     * @param nodes
     * @return True if nodes form a valid shape for node-shape construction. No segment-crossings!
     */
    public static boolean validNodeShapeNodes(List<double[]> nodes) {
        List<double[][]> segments = segments(nodes);

        while (segments.size() > 0) {
            double[][] segment = segments.get( segments.size() - 1 );

            if (intersectsAny(segment, segments)) {
                return false;
            }

            segments.remove( segments.size() - 1 );
        }
        return true;
    }

    /**
     * @param nodes
     * @return Segments from nodes.
     */
    private static List<double[][]> segments(List<double[]> nodes) {
        List<double[][]> segments = new ArrayList<>();

        for (int i = 0; i < nodes.size() - 1; i++) {
            double[] n1 = nodes.get(i);
            double[] n2 = nodes.get(i + 1);
            double[][] segment = new double[][]{n1, n2};
            segments.add(segment);
        }
        return segments;
    }

    /**
     * @param segment
     * @param segments
     * @return True if segment intersects any segment in segments.
     */
    private static boolean intersectsAny(double[][] segment, List<double[][]> segments) {
        for (double[][] segment2 : segments) {
            if (intersects(segment, segment2)) return true;
        }
        return false;
    }

    /**
     * @param s0
     * @param s1
     * @return True if segments intersect.
     */
    private static boolean intersects(double[][] s0, double[][] s1) {
        double[] p0 = s0[0];
        double[] v0 = minus(s0[1], s0[0]);

        double[] p1 = s1[0];
        double[] v1 = minus(s1[1], s1[0]);

        double[] dists = intersectDistance(p0, v0, p1, v1);

        return dists[0] > 0 && dists[0] < length(v0) &&
                dists[1] > 0 && dists[1] < length(v1);
    }

    /**
     * How long walk from point p0 along v0 (pDist), and from p1 along
     * v1 (qDist) until they intersect?
     *
     * @return [pDist, qDist]
     */
    public static double[] intersectDistance(double[] p0, double[] v0, double[] p1, double[] v1) {
        v0 = normalize(v0);
        v1 = normalize(scale(v1, -1));
        double[][] A = transpose(new double[][]{v0, v1});
        double[] b = minus(p1, p0);
        double[] xs = solve(A, b);
        double d0 = xs[0];
        double d1 = xs[1];
        return new double[]{d0, d1};
    }

    /**
     * @return Vector multiplied by scalar.
     */
    public static double[] scale(double[] v, double f) {
        return new double[]{v[0]*f, v[1]*f};
    }

    /**
     * 2D subtraction.
     */
    public static double[] minus(double[] v1, double[] v2) {
        return new double[]{v1[0] - v2[0], v1[1] - v2[1]};
    }

    /**
     * @param A 2*2 matrix.
     * @return A transposed.
     */
    public static double[][] transpose(double[][] A) {
        return new double[][]{
                new double[]{ A[0][0], A[1][0] },
                new double[]{ A[0][1], A[1][1] }};
    }

    /**
     * @return Normalized vector.
     */
    public static double[] normalize(double[] v) {
        double l = length(v);
        if (l == 0) return new double[]{0, 0};
        return new double[]{ v[0]/l, v[1]/l };
    }

    /**
     * @return Length of vector.
     */
    public static double length(double[] v) {
        return Math.sqrt(Math.pow(v[0], 2) + Math.pow(v[1], 2));
    }

    /**
     * Solves equation Ax=b.
     *
     * @param A 2*2 matrix.
     * @param b Length-2-vector.
     * @return Vector x (length 2), or null if no solution.
     */
    public static double[] solve(double[][] A, double[] b) {
        double[][] Ainv = inverse(A);
        if (Ainv == null) return null;
        return multiply(Ainv, b);
    }

    /**
     * @return Inverse of A, or null if no inverse.
     */
    public static double[][] inverse(double[][] A) {
        double det = 1 / (A[0][0]*A[1][1] - A[0][1]*A[1][0]);
        return new double[][]{
                scale(new double[]{ A[1][1], -A[0][1] }, det),
                scale(new double[]{ -A[1][0], A[0][0] }, det) };
    }

    /**
     * @param A 2*2.
     * @param b 2*1.
     * @return A*b (2*1).
     */
    public static double[] multiply(double[][] A, double[] b) {
        return new double[]{ A[0][0]*b[0] + A[0][1]*b[1],
                A[1][0]*b[0] + A[1][1]*b[1] };
    }
    //endregion


    public NodeShape asClosed() {
        NodeShape nodeShape = new NodeShape(this.getNodes());
        //if (!nodeShape.isClosed()) {
            nodeShape.nodes.add( nodeShape.nodes.get(0) );
        //}
        return nodeShape;
    }
}