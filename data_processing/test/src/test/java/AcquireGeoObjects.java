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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.FileReader;

public class AcquireGeoObjects {

    @Test
    public void main() throws IOException {
        JsonObject convTable = getConversionTable();
        Shape area = getArea();
        GeoObjInstructionsIter iter = new GeoObjInstructionsIter(area);
        iter.open();
        List<String> instr;

        while ((instr=iter.next()) != null) {
            try {
                GeoObject go = new GeoObject(instr, convTable);
                saveToDB(go);
            }
            catch (GeoObject.BuildException e) {
                System.out.println(e + "\n");
            }
        }
    }

    /**
     * Load table for conversion from tags to category.
     */
    private JsonObject getConversionTable() throws IOException {
        String path = "../categories.json";
        return new JsonParser().parse(new FileReader(path)).getAsJsonObject();
    }

    /**
     * @return Area of interest.
     */
    private Shape getArea() {
        //uppsala
        // double w = 17.558212280273438;
        // double s = 59.78301472732963;
        // double e = 17.731246948242188;
        // double n = 59.91097597079679;

        //mefjärd
        double w = 18.460774;
        double s = 58.958251;
        double e = 18.619389;
        double n = 59.080544;

        return new Shape(Arrays.asList(new double[][]{
                    new double[]{w, s},
                    new double[]{w, n},
                    new double[]{e, n},
                    new double[]{e, s}
                }));
    }

    /**
     * Save to database...print.
     */
    private void saveToDB(GeoObject go) {
        System.out.println(go);
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
                // System.out.println(url);
                // System.exit(0);
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
            String query = readFile("../query.ql");
            query = query.replace("{{bbox}}", bs_str);
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
         * [lon, lat]. */
        private List<double[]> points;

        public Shape(List<double[]> ps) {
            this.points = ps;
        }

        public List<double[]> getPoints() {
            return this.points;
        }

        /**
         * @return True if p[0] == p[1].
         */
        public boolean isClosed() {
            return false;
        }

        /**
         * @return [wsen]
         */
        public double[] getBounds() {
            double w = Double.POSITIVE_INFINITY;
            double s = Double.POSITIVE_INFINITY;
            double e = Double.NEGATIVE_INFINITY;
            double n = Double.NEGATIVE_INFINITY;

            for (double[] p : this.points) {
                if (p[0] < w) w = p[0];
                if (p[0] > e) e = p[0];
                if (p[1] < s) s = p[1];
                if (p[1] > n) n = p[1];
            }
            return new double[]{w, s, e, n};
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (double[] p : this.points)
                sb.append("lon: " + p[0] + ", lat: " + p[1] + "\n");
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
                "subcat: " + this.subcat + "\n";
            //"shape:\n" + this.shape.toString() + "\n";
        }
    }
}
