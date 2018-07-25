package com.localore.localore;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.content.Context;

import java.io.File;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
//import com.google.gson.JsonParser;

/**
 * Class representing geo-object.
 *
 * @inv Minimum one shape.
 */
@Entity
public class GeoObject {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String osmId = null;
    private String name = null;
    private double rank = -1;
    private String supercat = null;
    private String subcat = null;
    private List<NodeShape> shapes = null;

    private int exerciseId;
    private int quizId;

    public GeoObject() {}

     /**
      * Default constructor.
      */
     public GeoObject(String osmId, String name, List<NodeShape> nss, double rank, String supercat, String subcat) {
         this.osmId = osmId;
         this.name = name;
         this.shapes = nss;
         this.rank = rank;
         this.supercat = supercat;
         this.subcat = subcat;
     }

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
                this.osmId = ins.substring(3, ins.length());
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
            this.shapes = new ArrayList<NodeShape>();
            this.shapes.add(new NodeShape(ps));
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
        if (this.osmId == null ||
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
    public void addShape(NodeShape sh) {
        this.shapes.add(sh);
    }
    public void addShapes(List<NodeShape> shs) {
        for (NodeShape sh : shs) addShape(sh);
    }

    /**
     * @return Some kind of importance-ranking.
     */
    public double getRank(int version, List<String> tags) {
        return version + tags.size() * 0.25;
    }

    /**
     * @return Sum of all segment-lengths.
     */
    public double getLength() {
        double sum = 0;
        for (NodeShape sh : this.shapes) sum += sh.getLength();
        return sum;
    }

    /**
     * @return True if this object is a single node.
     */
    public boolean isNode() {
        return
                this.shapes.size() == 1 &&
                        this.shapes.get(0).size() == 1;
    }

    /**
     * @return 1d-list of nodes, without particular order.
     */
    public List<double[]> getNodes() {
        List<double[]> l = new ArrayList<double[]>();
        for (NodeShape sh : this.shapes) l.addAll(sh.getNodes());
        return l;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOsmId() {
        return osmId;
    }

    public void setOsmId(String osmId) {
        this.osmId = osmId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    public String getSupercat() {
        return supercat;
    }

    public void setSupercat(String supercat) {
        this.supercat = supercat;
    }

    public String getSubcat() {
        return subcat;
    }

    public void setSubcat(String subcat) {
        this.subcat = subcat;
    }

    public List<NodeShape> getShapes() {
        return shapes;
    }

    public void setShapes(List<NodeShape> shapes) {
        this.shapes = shapes;
    }

    public int getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(int exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
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
     * @param bs [wsen]
     * @return Number of nodes inside bs.
     */
    public int inNodeCount(double[] bs) {
        int count = 0;
        for (double[] n : this.getNodes())
            if (isInside(n, bs)) count++;
        return count;
    }

    /**
     * @return True if n[lon lat] is inside bs[wsen].
     */
    private boolean isInside(double[] n, double[] bs) {
        return
                n[0] > bs[0] &&
                        n[0] < bs[2] &&
                        n[1] > bs[1] &&
                        n[1] < bs[3];
    }

    /**
     * @return Link to this osm-object.
     */
    private String getLink() {
        return "https://www.openstreetmap.org/" + osmId.toLowerCase();
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
        if (this.shapes == null) {
            sStr.append("null");
        }
        else {
            for (NodeShape sh : this.shapes) {
                sStr.append(sh.toString());
                if (sh != shapes.get(shapes.size() - 1))
                    sStr.append("-\n");
            }
        }

        return
                "osmId: " + this.osmId + "\n" +
                        "name: " + this.name + "\n" +
                        "rank: " + this.rank + "\n" +
                        "supercat: " + this.supercat + "\n" +
                        "subcat: " + this.subcat + "\n" +
                        "url: " + getLink() + "\n" +
                        "shape:\n" + sStr.toString();
    }
}

class GeoObjectConverter {
    @TypeConverter
    public static List<NodeShape> fromString(String gosJson) {
        Type listType = new TypeToken<List<NodeShape>>(){}.getType();
        return new Gson().fromJson(gosJson, listType);
    }

    @TypeConverter
    public static String fromArrayList(List<NodeShape> gos) {
        Gson gson = new Gson();
        String gosJson = gson.toJson(gos);
        return gosJson;
    }
}
