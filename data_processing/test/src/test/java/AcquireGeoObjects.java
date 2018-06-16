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
    public static final Logger LOGGER = Logger.getLogger("");

    /**
     * Level used when logging. Ignores messages with lower level.
     * Severe, warning, info, config, fine, finer, finest. */
    public static final Level LOG_LEVEL = Level.INFO;

    static {
         LOGGER.setLevel(LOG_LEVEL);
         LOGGER.getHandlers()[0].setLevel(LOG_LEVEL);

         //log to file
         try {
             LogManager.getLogManager().reset();
             FileHandler fh = new FileHandler("log");
             SimpleFormatter formatter = new SimpleFormatter();
             fh.setFormatter(formatter);
             LOGGER.addHandler(fh);
         }
         catch (IOException e) {
             System.out.println("Failed file logging");
         }
    }


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
                LOGGER.info("Can't build:\n" + e.toString());
            }
        }

        db.dedupe();
        LOGGER.info(db.toString());
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
                LOGGER.info("Query: " + url.toString());
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
                LOGGER.info("Query: " + url.toString());
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
     * Representation of a shape.
     *
     * @inv No segment crossings.
     * @inv No lon=180-crossings.
     * @inv Number of points > 0.
     */
    class Shape {

        /**
         * [lon lat]. */
        private List<double[]> nodes;

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
         * @return True if p[0] == p[1].
         */
        public boolean isClosed() {
            double endPointsDist = distance(this.nodes.get(0), this.nodes.get(nodes.size()-1));
            return endPointsDist < 0.0001;
        }

        /**
         * @return Number of nodes.
         */
        public int size() {
            return this.nodes.size();
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
    }

    /**
     * Class representing a geo-object.
     */
    class GeoObject {
        private String id = null;
        private String name = null;
        private Shape shape = null;
        private double rank = -1;
        private String supercat = null;
        private String subcat = null;


        /**
         * Default constructor.
         */
        public GeoObject(String id, String name, Shape shape, double rank, String supercat, String subcat) {
            this.id = id;
            this.name = name;
            this.shape = shape;
            this.rank = rank;
            this.supercat = supercat;
            this.subcat = subcat;
        }

        /**
         * Construct the object from instructions.
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

            if (ps.size() > 0) this.shape = new Shape(ps);
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
            if (this.id == null || this.name == null || this.shape == null || this.rank == -1 || this.supercat == null || this.subcat == null)
                throw new Exception("Field not set");

            if (name.length() < 2) throw new Exception("Name too short");
        }

        /**
         * @return Some kind of importance-ranking.
         */
        private double getRank(int version, List<String> tags) {
            return version + tags.size() * 0.25;
        }

        private String getID() { return this.id; }
        private String getName() { return this.name; }
        private Shape getShape() { return this.shape; }
        private double getRank() { return this.rank; }
        private String getSuperCat() { return this.supercat; }
        private String getSubCat() { return this.subcat; }

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

        @Override
        public String toString() {
            return
                "id: " + this.id + "\n" +
                "name: " + this.name + "\n" +
                "rank: " + this.rank + "\n" +
                "supercat: " + this.supercat + "\n" +
                "subcat: " + this.subcat + "\n" +
                "https://www.openstreetmap.org/" + id.toLowerCase() + "\n";
                //"shape:\n" + this.shape.toString() + "\n";
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
                    return goMerge;
                }
            }
            return null;
        }

        /**
         * @return Merged object, or NULL if merge not possible.
         * @pre o1, o2 has same (or similar) name.
         */
        private GeoObject merge(GeoObject o1, GeoObject o2) {
            double LIMIT = 20; //in meters

            Shape s1 = o1.getShape();
            Shape s2 = o2.getShape();
            if (s1.size() < 2 || s1.isClosed()) return null;
            if (s2.size() < 2 || s2.isClosed()) return null;
            if (!o1.getSuperCat().equals(o2.getSuperCat())) return null;
            if (!o1.getSubCat().equals(o2.getSubCat())) return null;

            List<double[]> ns1 = s1.getNodes();
            List<double[]> ns2 = s2.getNodes();
            List<double[]> ns3 = new ArrayList<double[]>();

            if (distance(ns1.get(ns1.size()-1), ns2.get(0)) < LIMIT) {
                ns3.addAll(ns1);
                ns3.addAll(ns2);
            }
            else if (distance(ns2.get(ns2.size()-1), ns1.get(0)) < LIMIT) {
                ns3.addAll(ns2);
                ns3.addAll(ns1);
            }
            else if (distance(ns1.get(ns1.size()-1), ns2.get(ns2.size()-1)) < LIMIT) {
                ns3.addAll(ns1);
                ns3.addAll(reverse(ns2));
            }
            else if (distance(ns1.get(0), ns2.get(0)) < LIMIT) {
                ns3.addAll(reverse(ns1));
                ns3.addAll(ns2);
            }
            else {
                return null;
            }

            if (distance(ns3.get(0), ns3.get(ns3.size()-1)) > 0 &&
                distance(ns3.get(0), ns3.get(ns3.size()-1)) < LIMIT) {

                ns3.add(ns3.get(0));
            }

            Shape s3 = new Shape(ns3);
            GeoObject prio = o1;
            if (o1.getRank() < o2.getRank()) prio = o2;

            return new GeoObject(prio.getID(), prio.getName(), s3, prio.getRank(), prio.getSuperCat(), prio.getSubCat());
        }

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

    /**
     * @return xs reversed.
     */
    public static <T> List<T> reverse(List<T> xs) {
        List<T> rev = new ArrayList<T>();

        for (int i = xs.size()-1; i >= 0; i--) {
            rev.add(xs.get(i));
        }
        return rev;
    }
}
