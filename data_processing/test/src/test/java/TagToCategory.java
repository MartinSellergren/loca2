import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.io.FileReader;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Arrays;
import java.net.URLEncoder;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class TagToCategory {

    @Test
    public void main() throws MalformedURLException, IOException {
        System.out.println("...");

        JsonObject convTable = getConversionTable();
        JsonArray elems = getGeoObjects_();

        for (int i = 0; i < elems.size(); i++) {
            JsonObject elem = elems.get(i).getAsJsonObject();
            //new GeoObject(elem, elems, convTable);

            if (elem.has("tags")) {
                long id = elem.get("id").getAsLong();
                JsonObject tags = elem.getAsJsonObject("tags");
                String name = tags.get("name").getAsString();
                String category = getCategory(elem, convTable);
                // double[][] points = getPoints(id, elems);
                // double ranking = getRanking(category, points);

                //new GeoObject(name, category, points, ranking);

                //if (category != "none" || tags.has("route") || tags.has("was:route")) continue;
                System.out.println("");
                System.out.println(name + "(" + id + ")");
                System.out.println(category);
                System.out.println(elem.get("type").getAsString());
                //System.out.println(ranking);
                System.out.println(elem.getAsJsonObject("tags"));
                //System.out.println("Points:");
                //System.out.println(Arrays.toString(points));
            }
        }
    }


    /**
     * Load table for conversion from tags to category.
     */
    private JsonObject getConversionTable() throws IOException {
        String path = "../categories2.json";
        return new JsonParser().parse(new FileReader(path)).getAsJsonObject();
    }

    /**
     * Fetch elements from overpass.
     */
    private JsonArray getGeoObjects() throws MalformedURLException, IOException {
        URL url = getQuery();
        HttpURLConnection req = (HttpURLConnection) url.openConnection();
        req.connect();
        JsonObject jobj = new JsonParser().parse(new InputStreamReader((InputStream) req.getContent())).getAsJsonObject();

        return jobj.getAsJsonArray("elements");
    }

    /**
     * @return URL that holds geo-objects.
     */
    private URL getQuery() throws MalformedURLException {
        //uppsala
        double s = 59.78301472732963;
        double w = 17.558212280273438;
        double n = 59.91097597079679;
        double e = 17.731246948242188;

        //mefjärd
        s = 58.958251;
        w = 18.460774;
        n = 59.080544;
        e = 18.619389;

        String bbox = String.format("%s, %s, %s, %s", s, w, n, e);
        String query = String.format("[out:xml][timeout:900][bbox:{{bbox}}]; node[name]; out qt; way[name]; out qt ({{bbox}}) geom; convert WAY _id=id(), length=length(), is_closed=is_closed(); out qt noids; rel[name]; map_to_area; rel(pivot); convert REL ::id=id(), ::geom=hull(geom()), ::=::, version=version(), no_members=count_members(); out qt geom;", bbox, bbox, bbox);
        String url_str = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query);

        return new URL(url_str);
    }

    /**
     * Offline.
     */
    private JsonArray getGeoObjects_() throws IOException {
        String path = "response.json";
        JsonObject jobj = new JsonParser().parse(new FileReader(path)).getAsJsonObject();

        return jobj.getAsJsonArray("elements");
    }

    /**
     * @param elem OSM-element (geo-object).
     * @return Category of element, or "none".
     */
    private String getCategory(JsonObject elem, JsonObject convTable) {
        String[] tags = getTags(elem);
        String category = "none";
        int minIndex = Integer.MAX_VALUE;

        for (String tag : tags) {
            CatData cat = getCatData(tag, convTable);

            if (cat != null && cat.index < minIndex) {
                category = cat.text;
                minIndex = cat.index;
            }
        }

        return category;
    }

    /**
     * @return OSM-tags of OSM-element elem, ["key=value", ...].
     */
    private String[] getTags(JsonObject elem) {
        if (!elem.has("tags")) return new String[]{};

        JsonObject jtags = elem.getAsJsonObject("tags");
        Set<Entry<String, JsonElement>> tagSet = jtags.entrySet();
        String[] tags = new String[tagSet.size()];
        int i = 0;

        for (Entry<String, JsonElement> tagEntry : tagSet) {
            String key = tagEntry.getKey();
            String value = tagEntry.getValue().getAsString();

            String tag = key + "=" + value;
            tags[i++] = tag;
        }

        return tags;
    }


    class CatData {
        public final int index;
        public final String text;
        public CatData(int i, String txt) {
            index = i;
            text = txt;
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

        for (String main : mains) {
            JsonArray cats = table.getAsJsonArray(main);

            for (int i = 0; i < cats.size(); i++) {
                JsonArray cat = cats.get(i).getAsJsonArray();
                int catIndex = index++;
                String catName = cat.get(0).getAsString();

                if (containsTag(cat, tag))
                    return new CatData(catIndex, catName);
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
     * @param id ID of OSM-element: type=node -> has lat and lon
     *                              type=way -> has node-array (sorted)
     *                              type=relation -> has members
     *                      member: has ref='id to node/way/relation'
     * @param elems Json-array of elements.
     * @return [lon,lat] of element's shape.
     */
    // private double[][] getPoints(long id, JsonArray elems) {


    //     JsonObject elem = getElem(id, elems);
    //     String t = getType(elem);

    //     if (t.equals("node")) {

    //     }
    //     else if (t.equals("way")) {

    //     }
    //     else if (t.equals("relation")) {

    //     }
    //     else {
    //         throw new RuntimeException("bad basic component type");
    //         return null;
    //     }
    // }
}
