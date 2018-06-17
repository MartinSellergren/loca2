import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collections;
import java.util.Comparator;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.FileReader;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.LogManager;

public class AcquireGeoObjects {

    /**
     * Usage:
     * LOGGER.log( Level.FINE, "processing {0} entries", list.size() );
     * LOGGER.log( Level.FINER, "processing[{0}]: {1}", new Object[]{ i, list.get(i) } );
     * exception ex:
     * LOGGER.log( Level.SEVERE, ex.toString(), ex );
     *
     *	severe(String msg)
     *  warning(String msg)
     *	info(String msg)
     * 	config(String msg)
     * 	fine(String msg)
     *  finer(String msg)
     * 	finest(String msg) */
    public static final Logger TLOG = Logger.getLogger("tlog");
    public static final Logger FLOG = Logger.getLogger("flog");
    public static final Level TLOG_LEVEL = Level.INFO;
    public static final Level FLOG_LEVEL = Level.FINE;


    /**
     * Max allowed distance in meters between endpoints of two
     * geo-objects for a merge.
     */
    public static final double MERGE_LIMIT = 0.5;


    @Test
    public void main() throws IOException {
        JsonObject convTable = getConversionTable();
        DB db = new DB();

        Shape area = getArea();
        GeoObjInstructionsIter iter = new GeoObjInstructionsIter();
        iter.open();
        List<String> instr;

        while ((instr=iter.next()) != null) {
            try {
                GeoObject go = new GeoObject(instr, convTable);
                db.add(go);
            }
            catch (GeoObject.BuildException e) {
                TLOG.fine("Can't build:\n" + e.toString());
            }
        }

        db.dedupe();
        FLOG.info(db.toString());
    }

    /**
     * Load table for conversion from tags to category.
     */
    private JsonObject getConversionTable() throws IOException {
        String path = "../categories.json";
        return new JsonParser().parse(new FileReader(path)).getAsJsonObject();
    }

    /**
     * @return Area of interest. A closed shape.
     */
    private Shape getArea() {
        //uppsala
        double w = 17.558212280273438;
        double s = 59.78301472732963;
        double e = 17.731246948242188;
        double n = 59.91097597079679;

        //mefjärd
        // double w = 18.460774;
        // double s = 58.958251;
        // double e = 18.619389;
        // double n = 59.080544;

        return new Shape(Arrays.asList(new double[][]{
                    new double[]{w, s},
                    new double[]{w, n},
                    new double[]{e, n},
                    new double[]{e, s}
                }));
    }

    /**
     * @return Whole content of file.
     */
    public String readFile(String path) {
        try {
            Scanner scanner = new Scanner( new File(path) );
            String text = scanner.useDelimiter("\\A").next();
            scanner.close();
            return text;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
     * Iterator that fetches geo-object data and iterates over
     * instructions for building the geo-objects.
     */
    class GeoObjInstructionsIter {
        private URL url = null;
        private Scanner scanner = null;

        public GeoObjInstructionsIter(Shape area) {
            try {
                this.url = getQueryURL(area);
                TLOG.fine("Query: " + url.toString());
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        /**
         * Constructor for offline (testing).
         */
        public GeoObjInstructionsIter() {
            try {
                this.url = new File("../resp_uppsala.xml").toURI().toURL();
                TLOG.fine("Query: " + url.toString());
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }


        /**
         * @return URL that provides geo-objects-data.
         */
        private URL getQueryURL(Shape area) throws MalformedURLException {
            double[] bs = area.getBounds();
            String bs_str = String.format("%s,%s,%s,%s", bs[1], bs[0], bs[3], bs[2]);
            String poly_str = area.toRawString();

            String query = readFile("../query.ql");
            query = query.replace("{{bbox}}", bs_str);
            query = query.replace("{{poly}}", poly_str);
            query = URLEncoder.encode(query);

            return new URL("https://overpass-api.de/api/interpreter?data=" + query);
        }

        /**
         * Call before next().
         */
        public void open() {
            try {
                this.scanner = new Scanner(this.url.openStream());
            }
            catch(IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        /**
         * Returns instructions to build geo-objects. Closes connection
         * if called when no more instructions.
         *
         * e.g:
         * ["lat_lon 59.0206802 18.5453333",
         *  "lat_lon ... ...",
         *  "id NODE/43664464",
         *  "version 1",
         *  "name Ängsön-Marskär",
         *  "tag natural=coastline",
         *  "tag place=island"]
         *
         * @return Next instruction, or NULL if no more.
         */
        public List<String> next() {
            List<String> instr = new ArrayList<String>();

            while (this.scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                if (line.equals("</NODE>") ||
                    line.equals("</WAY>") ||
                    line.equals("</REL>")) {

                    return instr;
                }

                String res;
                if ((res=extractID(line)) != null) {
                    instr.add("id " + res);
                }
                if ((res=extractName(line)) != null) {
                    instr.add("name " + res);
                }
                else if ((res=extractPoint(line)) != null) {
                    instr.add("lat_lon " + res);
                }
                else if ((res=extractVersion(line)) != null) {
                    instr.add("version " + res);
                }
                else if ((res=extractTag(line)) != null) {
                    instr.add("tag " + res);
                }
            }

            scanner.close();
            return null;
        }
    }

    /**
     * @return "type/number" or null.
     * type = NODE/WAY/REL
     */
    private String extractID(String line) {
        Pattern p = Pattern.compile("<(.*?) id=\"(.*?)\">");
        Matcher m = p.matcher(line);
        if (m.find()) return m.group(1) + "/" + m.group(2);
        else return null;
    }

    /**
     * @return "Name" or NULL.
     */
    private String extractName(String line) {
        Pattern p = Pattern.compile("k=\"name\" v=\"(.*?)\"");
        Matcher m = p.matcher(line);
        if (m.find()) return m.group(1);
        else return null;
    }

    /**
     * @return "lat lon" or NULL.
     */
    private String extractPoint(String line) {
        Pattern p = Pattern.compile("lat=\"(.*?)\" lon=\"(.*?)\"");
        Matcher m = p.matcher(line);
        if (m.find()) return m.group(1) + " " + m.group(2);
        else return null;
    }

    /**
     * @return "ver" or NULL.
     */
    private String extractVersion(String line) {
        Pattern p = Pattern.compile("k=\"version\" v=\"(.*?)\"");
        Matcher m = p.matcher(line);
        if (m.find()) return m.group(1);
        else return null;
    }

    /**
     * @return "key=value" or NULL.
     */
    private String extractTag(String line) {
        Pattern p = Pattern.compile("k=\"(.*?)\" v=\"(.*?)\"");
        Matcher m = p.matcher(line);

        if (m.find()) return m.group(1) + "=" + m.group(2);
        else return null;
    }

    /**
     * Representation of a shape as an order list of nodes.
     *
     * @inv No segment crossings.
     * @inv No lon=180-crossings.
     * @inv Number of points > 0.
     */
    class Shape {

        /**
         * [lon lat]. */
        private List<double[]> nodes;

        /**
         * New shape from well-formed list of nodes.
         */
        public Shape(List<double[]> ns) {
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

        // /**
        //  * Reverse order of nodes.
        //  */
        // public void reverse() {
        //     this.nodes = AcquireGeoObjects.reverse(this.nodes);
        // }

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
    }

    /**
     * Class representing geo-object.
     *
     * @inv Minimum one shape.
     */
    class GeoObject {
        private String id = null;
        private String name = null;
        private List<Shape> shapes = null;
        private double rank = -1;
        private String supercat = null;
        private String subcat = null;


        // /**
        //  * Default constructor.
        //  */
        // public GeoObject(String id, String name, List<Shape> ms, double rank, String supercat, String subcat) {
        //     this.id = id;
        //     this.name = name;
        //     this.shapes = ms;
        //     this.rank = rank;
        //     this.supercat = supercat;
        //     this.subcat = subcat;
        // }

        /**
         * Construct the object from instructions.
         * Constructs geo-object with one shape (not multiple).
         */
        public GeoObject(List<String> instr, JsonObject convTable) throws BuildException {
            try {
                setFields(instr, convTable);
                testFields();
            }
            catch (Exception e) {
                throw new BuildException(e.getMessage(), instr);
            }
        }
        class BuildException extends Exception {
            public BuildException(String msg, List<String> instr) {
                super(msg + "\n" + Arrays.toString(instr.toArray()));
            }
        }

        /**
         * Set fields of this object as indicated by the instructions.
         * Absent/bad instructions results in field not set.
         */
        private void setFields(List<String> instr, JsonObject convTable) {
            List<double[]> ps = new ArrayList<double[]>();
            List<String> tags = new ArrayList<String>();
            int version = -1;

            for (String ins : instr) {
                String[] parts = ins.split(" ");

                if (parts[0].equals("id")) {
                    this.id = ins.substring(3, ins.length());
                }
                else if (parts[0].equals("name")) {
                    this.name = ins.substring(5, ins.length());
                }
                else if (parts[0].equals("version")) {
                    version = Integer.parseInt(parts[1]);
                }
                else if (parts[0].equals("lat_lon")) {
                    double lat = Double.parseDouble(parts[1]);
                    double lon = Double.parseDouble(parts[2]);
                    ps.add(new double[]{lon, lat});
                }
                else if (parts[0].equals("tag")) {
                    tags.add(ins.substring(4, ins.length()));
                }
                else {
                    throw new RuntimeException("Illegal instruction: " + ins);
                }
            }

            if (ps.size() > 0) {
                this.shapes = new ArrayList<Shape>();
                this.shapes.add(new Shape(ps));
            }
            if (version != -1) this.rank = getRank(version, tags);

            String cat = findCategory(tags, convTable);
            if (cat != null) {
                this.supercat = cat.split(":")[0];
                this.subcat = cat.split(":")[1];
            }
        }

        /**
         * @throws Exception with message if a field doesn't have
         * a proper value.
         */
        private void testFields() throws Exception {
            if (this.id == null ||
                this.name == null ||
                this.shapes == null ||
                this.rank == -1 ||
                this.supercat == null ||
                this.subcat == null)
                throw new Exception("Field not set");

            if (name.length() < 2) throw new Exception("Name too short");
        }

        /**
         * @pre sh should be close in proximity.
         */
        public void addShape(Shape sh) {
            this.shapes.add(sh);
        }
        public void addShapes(List<Shape> shs) {
            for (Shape sh : shs) addShape(sh);
        }

        /**
         * @return Some kind of importance-ranking.
         */
        public double getRank(int version, List<String> tags) {
            return version + tags.size() * 0.25;
        }

        //private String getID() { return this.id; }
        public String getName() { return this.name; }
        public List<Shape> getShapes() { return this.shapes; }
        public double getRank() { return this.rank; }
        public String getSuperCat() { return this.supercat; }
        public String getSubCat() { return this.subcat; }
        public String getGeoJson() { return null; }

        /**
         * @return True if this object is a single node.
         */
        public boolean isNode() {
            return
                this.shapes.size() == 1 &&
                this.shapes.get(0).size() == 1;
        }

        /**
         * Extracts relevant category from conversion-table.
         *
         * @param tags ["key=value"]
         * @return "supercat:subcat" or NULL.
         */
        private String findCategory(List<String> tags, JsonObject convTable) {
            CatData cat = null;
            int minIndex = Integer.MAX_VALUE;

            for (String tag : tags) {
                CatData c = getCatData(tag, convTable);

                if (c != null && c.index < minIndex) {
                    cat = c;
                }
            }

            if (cat == null) return null;
            else return cat.supercat + ":" + cat.subcat;
        }

        class CatData {
            public final int index;
            public final String supercat;
            public final String subcat;
            public CatData(int i, String sup, String sub) {
                index = i;
                supercat = sup;
                subcat = sub;
            }
        }
        /**
         * @param tag Arbitrary OSM-tag.
         * @param table Conversion table.
         * @return Category and importance (index in table) of category
         * of tag, or NULL if tag not described in table.
         */
        private CatData getCatData(String tag, JsonObject table) {
            String[] mains = new String[]{"transport", "constructions", "nature", "settlements", "roads"};
            int index = 0;

            for (String supercat : mains) {
                JsonArray cats = table.getAsJsonArray(supercat);

                for (int i = 0; i < cats.size(); i++) {
                    JsonArray cat = cats.get(i).getAsJsonArray();
                    int catIndex = index++;
                    String subcat = cat.get(0).getAsString();

                    if (containsTag(cat, tag))
                        return new CatData(catIndex, supercat, subcat);
                }
            }
            return null;
        }

        /**
         * @param cat Section of the conv-table describing tag-conversion
         * of one particular category.
         * @param tag A tag "key=value".
         * @return True if tag in section cat.
         */
        private boolean containsTag(JsonArray cat, String tag) {
            for (int i = 1; i < cat.size(); i++) {
                String tag2 = cat.get(i).getAsString();
                if (tag.equals(tag2))
                    return true;

                String key = tag.split("=")[0];
                String value = tag.split("=")[1];
                String key2 = tag2.split("=")[0];
                String value2 = tag2.split("=")[1];

                if (key.equals(key2) && value2.equals("*") && !value.equals("no"))
                    return true;

            }
            return false;
        }

        /**
         * @return Link to this osm-object.
         */
        private String getLink() {
            return "https://www.openstreetmap.org/" + id.toLowerCase();
        }

        /**
         * @return Compact representation.
         */
        public String toCompactString() {
            return String.format("%s (%s, %s, %s)\n%s",
                                 this.name,
                                 this.supercat,
                                 this.subcat,
                                 this.rank,
                                 getLink());
        }

        @Override
        public String toString() {
            StringBuilder sStr = new StringBuilder();
            for (Shape sh : this.shapes) {
                sStr.append(sh.toString());
                if (sh != shapes.get(shapes.size()-1))
                    sStr.append("-\n");
            }

            return
                "id: " + this.id + "\n" +
                "name: " + this.name + "\n" +
                "rank: " + this.rank + "\n" +
                "supercat: " + this.supercat + "\n" +
                "subcat: " + this.subcat + "\n" +
                "url: " + getLink() + "\n" +
                "shape:\n" + sStr.toString();
        }
    }

    /**
     * Database interface.
     */
    class DB {
        List<GeoObject> db = new ArrayList<GeoObject>();

        public void add(GeoObject go) {
            db.add(go);
        }

        /**
         * Attempt to merge object-pices into one object.
         * Same (similar) name + close proximity -> merge.
         */
        public void dedupe() {
            List<GeoObject> db2 = new ArrayList<GeoObject>();

            while (this.db.size() > 0) {
                GeoObject go = db.remove(0);
                List<GeoObject> sameNames = removeAll(go.getName());
                sameNames.add(go);
                List<GeoObject> merged = mergeMergables(sameNames);
                db2.addAll(merged);
            }
            this.db = db2;
        }

        /**
         * Remove all objects with specified name (or similar) from db.
         * @return Removed objects.
         */
        private List<GeoObject> removeAll(String name) {
            List<GeoObject> rmvs = new ArrayList<GeoObject>();
            ListIterator<GeoObject> iter = this.db.listIterator();

            while (iter.hasNext()) {
                GeoObject go = iter.next();
                if (go.getName().equalsIgnoreCase(name)) {
                    iter.remove();
                    rmvs.add(go);
                }
            }
            return rmvs;
        }

        /**
         * @param sameNames All have same (or similar) name.
         * @return List with merged (if possible) geo-objects.
         */
        private List<GeoObject> mergeMergables(List<GeoObject> sameNames) {
            List<GeoObject> merged = new ArrayList<GeoObject>();

            while (sameNames.size() > 0) {
                GeoObject accum = sameNames.remove(0);
                GeoObject temp;

                while ((temp=mergeFirst(accum, sameNames)) != null) {
                    accum = temp;
                }
                merged.add(accum);
            }
            return merged;
        }

        /**
         * Merges geo-object with one in list (first mergable one).
         * Also removes the merged object from the list.
         *
         * @param sameNames Geo-objects with same (or similar) names
         * as go.
         * @return Merge between go and first mergable from sameNames,
         * or NULL if no merge possible.
         * @pre All geo-objects has same (or similar names).
         */
        private GeoObject mergeFirst(GeoObject go, List<GeoObject> sameNames) {
            for (GeoObject go2 : sameNames) {
                GeoObject goMerge = merge(go, go2);
                if (goMerge != null) {
                    sameNames.remove(go2);

                    if (sameNames.size() >= 2)
                        TLOG.info(String.format("-%s\n-%s\n-%s",
                                                go.toCompactString(),
                                                go2.toCompactString(),
                                                goMerge.toCompactString()));

                    return goMerge;
                }
            }
            return null;
        }

        /**
         * @return Merged object, or NULL if merge not possible.
         * @pre o1, o2 has same (or similar) name.
         */
        private GeoObject merge(GeoObject g1, GeoObject g2) {
            if (!g1.getSuperCat().equals(g2.getSuperCat()) ||
                !g1.getSubCat().equals(g2.getSubCat()) ||
                g1.isNode() || g2.isNode())
                return null;

            double d = minDistance(g1, g2);
            if (d > MERGE_LIMIT) return null;

            if (g1.getRank() > g2.getRank()) {
                g1.addShapes(g2.getShapes());
                return g1;
            }
            else {
                g2.addShapes(g1.getShapes());
                return g2;
            }
        }

        /**
         * @return Distance in meters between g1 and g2's closest nodes.
         */
        private double minDistance(GeoObject g1, GeoObject g2) {
            return 0;
        }
        //     if (!isMergable(g1, g2)) return null;

        //     Shape seg1 = g1.getShape();
        //     Shape seg2 = g2.getShape();

        //     int mergeType = getMergeType(seg1, seg2);
        //     if (mergeType == 1) {
        //         //pass
        //     }
        //     else if (mergeType == 2) {
        //         Shape temp = seg2;
        //         seg2 = seg1;
        //         seg1 = temp;
        //     }
        //     else if (mergeType == 3) {
        //         seg2.reverse();
        //     }
        //     else if (mergeType == 4) {
        //         seg1.reverse();
        //     }
        //     else {
        //         throw new RuntimeException("Bad merge type");
        //     }

        //     Shape sh = new Shape(seg1, seg2);

        //     GeoObject prio = g1;
        //     if (g1.getRank() < g2.getRank()) prio = g2;

        //     return new GeoObject(prio.getID(), prio.getName(), sh, prio.getRank(), prio.getSuperCat(), prio.getSubCat());
        // }

        // /**
        //  * @return True if g1 and g2 can be merged.
        //  */
        // private boolean isMergable(GeoObject g1, GeoObject g2) {
        //     Shape s1 = g1.getShape();
        //     Shape s2 = g2.getShape();
        //     return
        //         s1.size() >= 2 && s2.size() >= 2 &&
        //         !s1.isClosed() && !s2.isClosed() &&
        //         g1.getSuperCat().equals(g2.getSuperCat()) &&
        //         g1.getSubCat().equals(g2.getSubCat()) &&
        //         minEndpointDistance(g1, g2) < MERGE_LIMIT;
        // }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (GeoObject go : this.db)
                sb.append(go.toString() + "\n");
            return sb.toString();
        }
    }

    /**
     * @param lon,lat
     * @return Distance in meters between points.
     */
    public static double distance(double[] x, double[] y) {
        double lng1 = x[0]; double lat1 = x[1];
        double lng2 = y[0]; double lat2 = y[1];

        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
            * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;

        return dist;
    }

    // /**
    //  * @return xs reversed.
    //  */
    // public static <T> List<T> reverse(List<T> xs) {
    //     List<T> rev = new ArrayList<T>();

    //     for (int i = xs.size()-1; i >= 0; i--) {
    //         rev.add(xs.get(i));
    //     }
    //     return rev;
    // }

    // /**
    //  * @return A shallow copy of xs.
    //  */
    // public static <T> List<T> copy(List<T> xs) {
    //     List<T> cpy = new ArrayList<T>();
    //     for (T x : xs) cpy.add(x);
    //     return cpy;
    // }






    static {
        LogManager.getLogManager().reset();
        TLOG.setLevel(TLOG_LEVEL);
        FLOG.setLevel(FLOG_LEVEL);

        ConsoleHandler th = new ConsoleHandler();
        th.setFormatter(new MyFormatter());
        th.setLevel(TLOG_LEVEL);
        TLOG.addHandler(th);

         //log to file
         try {
             FileHandler fh = new FileHandler("log.out");
             fh.setFormatter(new MyFormatter());
             fh.setLevel(FLOG_LEVEL);
             FLOG.addHandler(fh);
         }
         catch (IOException e) {
             System.out.println("Failed file logging");
         }
    }
}
