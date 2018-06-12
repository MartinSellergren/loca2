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

public class AcquireGeoObjects {

    @Test
    public void main() {
        Shape area = getArea();
        GeoObjInstructionsIter iter = new GeoObjInstructionsIter(area);
        iter.open();
        String[] instr;

        while ((instr=iter.next()) != null) {
            System.out.println(instr);
            // GeoObject go = new GeoObject(instr);
            // saveToDB(go);
        }
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

        return new Shape( new double[][]{
                new double[]{w, s},
                new double[]{w, n},
                new double[]{e, n},
                new double[]{e, s}
            });
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
         *  "version 1",
         *  "name Ängsön-Marskär",
         *  "tag natural=coastline",
         *  "tag place=island"]
         *
         * @return Next instruction, or NULL if no more.
         */
        public String[] next() {
            if (this.scanner.hasNextLine()) {
                String line = scanner.nextLine();

                //...

                return new String[]{line};
            }
            else {
                scanner.close();
                return null;
            }
        }
    }

    /**
     * Representation of a shape.
     *
     * @inv No segment crossings.
     * @inv No lon=180-crossings.
     */
    class Shape {

        /**
         * [lon, lat]. */
        private double[][] points;

        public Shape(double[][] ps) {
            this.points = ps;
        }

        public double[][] getPoints() {
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
            return Arrays.toString(this.points);
        }
    }

    /**
     * Class representing a geo-object.
     */
    class GeoObject {
        private String name;
        private Shape shape;
        private int rank;
        private String supercat;
        private String subcat;

        /**
         * Construct the object from instructions.
         */
        public GeoObject(String[] instr) {
            //...
        }

        @Override
        public String toString() {
            return
                "name: " + this.name + "\n" +
                "rank: " + this.rank + "\n" +
                "supercat: " + this.supercat + "\n" +
                "subcat: " + this.subcat + "\n" +
                "shape: " + this.shape.toString();
        }
    }
}
