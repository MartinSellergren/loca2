package com.localore.localore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;

import com.localore.localore.model.GeoObject;
import com.localore.localore.model.NodeShape;
import com.mapbox.mapboxsdk.annotations.Annotation;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper of the MapboxMap for displaying geo-objects represented as markers and poly-lines.
 * (Not persistent, so recreate when Activity created).
 */
public class GeoObjectMap {
    /**
     * Side-length of an icon of a node (diameter of circle).
     */
    private static final int NODE_ICON_SIDE_LENGTH = 30;

    private final int workingAreaPolylineId = -1;

    /**
     * Time used when objects are blinked/flashed..
     */
    private static final int BLINK_TIME = 300;

    private MapboxMap mapboxMap;
    private List<GeoObject> geoObjects;
    private NodeShape border;
    private FloatingActionButton toggleZoomButton;
    private Context context;

    /**
     * Default display-color of all geo-objects. -1 means individual colors based on
     * geo-object's property-color.
     */
    private int defaultGeoObjectColor;

    /**
     * Mappings: annotationId -> geoObjectId, of all geo-objects.
     */
    private Map<Long, Long> markersLookupMap = new HashMap<>();
    private Map<Long, Long> polylinesLookupMap = new HashMap<>();


    /**
     * Construct an empty map.
     *
     * @param mapboxMap
     * @param defaultGeoObjectColor
     * @param border
     * @param showBorder
     * @param toggleZoomButton
     * @param context
     */
    public GeoObjectMap(MapboxMap mapboxMap, int defaultGeoObjectColor, NodeShape border, boolean showBorder,
                        FloatingActionButton toggleZoomButton, Context context) {
        mapboxMap.clear();
        this.mapboxMap = mapboxMap;
        this.defaultGeoObjectColor = defaultGeoObjectColor;
        this.geoObjects = new ArrayList<>();
        this.border = border;
        this.toggleZoomButton = toggleZoomButton;
        this.context = context;

        if (showBorder)
            addPolyline(border.getNodes(), workingAreaPolylineId, Color.LTGRAY);

        setToggleZoomButton(OVERVIEW_ZOOM);
        toggleZoomButton.setOnClickListener(view -> {
            onToggleZoomClick();
        });
    }

    //region zoom-toggle-button
    private static final int OVERVIEW_ZOOM = 0;
    private static final int OBJECTS_ZOOM = 1;
    private static final int ZOOM_TO_OBJECTS_ICON = android.R.drawable.ic_menu_search;
    private static final int ZOOM_TO_OVERVIEW_ICON = android.R.drawable.ic_menu_revert;
    /**
     * @param currentZoomId
     */
    private void setToggleZoomButton(int currentZoomId) {
        if (currentZoomId == OVERVIEW_ZOOM) {
            toggleZoomButton.setImageResource(ZOOM_TO_OBJECTS_ICON);
            toggleZoomButton.setTag(OVERVIEW_ZOOM);
        }
        else if (currentZoomId == OBJECTS_ZOOM) {
            toggleZoomButton.setImageResource(ZOOM_TO_OVERVIEW_ICON);
            toggleZoomButton.setTag(OBJECTS_ZOOM);
        }
        else {
            throw new RuntimeException("Dead end");
        }
    }

    /**
     * Listener to zoom-toggle.
     */
    private void onToggleZoomClick() {
        int currentZoomId = (int)toggleZoomButton.getTag();

        if (currentZoomId == OVERVIEW_ZOOM) {
            setToggleZoomButton(OBJECTS_ZOOM);
            flyToObjects(SHORT_FLY_TIME);
        }
        else if (currentZoomId == OBJECTS_ZOOM) {
            setToggleZoomButton(OBJECTS_ZOOM);
            flyToOverview(SHORT_FLY_TIME);
        }
        else {
            throw new RuntimeException("Dead end");
        }
    }

    //endregion

    //region Draw geo-objects on map

    /**
     * Add multiple geo-objects at once.
     * @param geoObjects
     */
    public void addGeoObjects(List<GeoObject> geoObjects) {
        for (GeoObject geoObject : geoObjects) {
            addGeoObject(geoObject);
        }
    }

    /**
     * Adds a geo-object to the map. Also updates id-maps if not null.
     * @param geoObject
     * @return Added stuff.
     */
    public void addGeoObject(GeoObject geoObject) {
        double SMALL_SIDE_LENGTH = 100; //meters
        int color = defaultGeoObjectColor == -1 ? geoObject.getColor() : defaultGeoObjectColor;

        for (NodeShape nodeShape : geoObject.getShapes()) {
            double[] bounds = nodeShape.getBounds();
            double[] ws = new double[]{bounds[0], bounds[1]};
            double[] es = new double[]{bounds[2], bounds[1]};
            double[] wn = new double[]{bounds[0], bounds[3]};

            if (nodeShape.getNodes().size() == 1) {
                addMarker(nodeShape.getNodes().get(0), geoObject.getId(), color);
            }
            else if (LocaUtils.distance(ws, es) < SMALL_SIDE_LENGTH &&
                    LocaUtils.distance(ws, wn) < SMALL_SIDE_LENGTH) {
                addMarker(nodeShape.getCenter(), geoObject.getId(), color);
            }
            else {
                nodeShape = nodeShape.isClosed() ? nodeShape.asExtraClosed() : nodeShape;
                addPolyline(nodeShape.getNodes(), geoObject.getId(), color);
            }
        }
    }

    /**
     * Adds a marker. Updates id-map if not null.
     * @param node
     * @param geoObjectId
     * @param color
     */
    public Marker addMarker(double[] node, long geoObjectId, int color) {
        Marker marker = mapboxMap.addMarker(new MarkerOptions()
                .icon(generateNodeIcon(color))
                .position(toLatLng(node)));

        this.markersLookupMap.put(marker.getId(), geoObjectId);
        return marker;
    }

    /**
     * Adds a polyline. Updates id-map if not null.
     * @param nodes
     * @param geoObjectId
     * @param color
     */
    public Polyline addPolyline(List<double[]> nodes, long geoObjectId, int color) {
        int GEO_OBJECT_LINE_WIDTH = 4;

        Polyline polyline = mapboxMap.addPolyline(new PolylineOptions()
                .addAll(toLatLngs(nodes))
                .color(color)
                .width( GEO_OBJECT_LINE_WIDTH ));

        this.polylinesLookupMap.put(polyline.getId(), geoObjectId);
        return polyline;
    }

    /**
     * @param color
     * @return Icon of a node-geo-object.
     */
    private Icon generateNodeIcon(int color) {
        int s = NODE_ICON_SIDE_LENGTH;

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(s, s, conf);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawCircle(s, s, s/2f, paint);

        return IconFactory.getInstance(context).fromBitmap(bmp);
    }

    //endregion

    //region Map-camera motion

    /**
     * Time in ms for flying (moving camera to new pos).
     */
    public static final int SHORT_FLY_TIME = 2000;
    public static final int LONG_FLY_TIME = 5000;

    /**
     * When fitting camera to shape, don't zoom more than this.
     */
    private static final double CAMERA_FITTING_MAX_ZOOM = 18;

    /**
     * Padding around working area for the initial camera-view.
     */
    private static final int CAMERA_FITTING_PADDING = 50;

    /**
     * Fit all object in camera-view.
     */
    public void flyToOverview(int flyTime) {
        flyToFitBounds(border.getBounds(), flyTime);
    }

    public void flyToObjects(int flyTime) {
        flyToFitBounds(GeoObject.getBounds(geoObjects), flyTime);
    }

    /**
     * Zoom in on geo-object.
     * @param geoObject
     */
    public void flyToGeoObject(GeoObject geoObject) {

    }

    /**
     * Move camera gradually to fit the bounds including padding.
     * @param bs
     * @param flyTime
     */
    private void flyToFitBounds(double[] bs, int flyTime) {
        LatLngBounds latLngBounds = toLatLngBounds(bs);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, CAMERA_FITTING_PADDING);
        CameraPosition cameraPosition = cameraUpdate.getCameraPosition(mapboxMap);

        if (cameraPosition.zoom > CAMERA_FITTING_MAX_ZOOM) {
            flyToLocation(cameraPosition.target, CAMERA_FITTING_MAX_ZOOM, mapboxMap, flyTime);
        }
        else {
            mapboxMap.animateCamera(cameraUpdate, flyTime);
        }
    }

    /**
     * Move camera gradually to new location.
     * @param latLng
     * @param map
     */
    private void flyToLocation(LatLng latLng, double zoom, MapboxMap map, int flyTime) {
        CameraPosition position = new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoom)
                .build();

        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), flyTime);
    }
    private void flyToLocation(Location location, double zoom, MapboxMap map, int flyTime) {
        LatLng latLng = toLatLng(new double[]{location.getLongitude(), location.getLatitude()});
        flyToLocation(latLng, zoom, map, flyTime);
    }

    //endregion

    //region LatLng converters

    /**
     * @param point [lon lat]
     * @return Converted to LatLng-mapbox-object
     */
    public static LatLng toLatLng(double[] point) {
        return new LatLng(point[1], point[0]);
    }

    /**
     *
     * @param points [lon lat]
     * @return Converted to LatLng-mapbox-objects
     */
    public static List<LatLng> toLatLngs(List<double[]> points) {
        List<LatLng> latLngs = new ArrayList<>();
        for (double[] point : points) latLngs.add(toLatLng(point));
        return latLngs;
    }

    /**
     * @param bounds [wsen|
     * @return Converted to LatLngBounds-mapbox-object.
     */
    public static LatLngBounds toLatLngBounds(double[] bounds) {
        return new LatLngBounds.Builder()
                .include( new LatLng(bounds[3], bounds[2]) ) // Northeast
                .include( new LatLng(bounds[1], bounds[0])) // Southwest
                .build();
    }

    //endregion

    //region geo-object -> annotations

    /**
     * @param geoObjectId
     * @return All annotations used to draw specified geo-object.
     */
    private List<Annotation> geoObjectAnnotations(long geoObjectId) {
        List<Long> markerIds = getKeysFromValue(geoObjectId, markersLookupMap);
        List<Long> polylinesIds = getKeysFromValue(geoObjectId, polylinesLookupMap);

        List<Annotation> annotations = new ArrayList<>();
        annotations.addAll(getMarkersFromIds(markerIds));
        annotations.addAll(getPolylinesFromIds(polylinesIds));
        return annotations;
    }

    /**
     * @param value
     * @param map
     * @return Keys with certain value.
     */
    private static List<Long> getKeysFromValue(long value, Map<Long,Long> map) {
        List<Long> keys = new ArrayList<>();
        for (Map.Entry<Long,Long> entry : map.entrySet()) {
            if (entry.getValue() == value) keys.add(entry.getKey());
        }
        return keys;
    }

    /**
     * @param ids
     * @return Markers in map with id in ids.
     */
    private List<Marker> getMarkersFromIds(List<Long> ids) {
        List<Marker> markers = new ArrayList<>();
        for (Marker marker : mapboxMap.getMarkers()) {
            if (ids.contains(marker.getId())) markers.add(marker);
        }
        return markers;
    }

    /**
     * @param ids
     * @return Polylines in map with id in ids.
     */
    private List<Polyline> getPolylinesFromIds(List<Long> ids) {
        List<Polyline> polylines = new ArrayList<>();
        for (Polyline polyline : mapboxMap.getPolylines()) {
            if (ids.contains(polyline.getId())) polylines.add(polyline);
        }
        return polylines;
    }

    //endregion

    //region blink annotations


    /**
     * Remove, then add geo-object.
     */
    public void blinkGeoObject(long geoObjectId) {
        List<Icon> markerIcons = new ArrayList<>();
        List<Integer> polylineColors = new ArrayList<>();

        for (Annotation annotation : geoObjectAnnotations(geoObjectId)) {
            if (annotation instanceof Marker) {
                Marker marker = (Marker)annotation;
                markerIcons.add(marker.getIcon());
                marker.setIcon(IconFactory.getInstance(context)
                        .fromResource(R.drawable.transparent_icon));
                mapboxMap.updateMarker(marker);
            }
            else if (annotation instanceof Polyline) {
                Polyline polyline = (Polyline)annotation;
                polylineColors.add(polyline.getColor());
                polyline.setColor(0x00000000);
                mapboxMap.updatePolyline(polyline);
            }
        }

        new Handler().postDelayed(() -> {
            for (Annotation annotation : geoObjectAnnotations(geoObjectId)) {
                if (annotation instanceof Marker) {
                    Marker marker = (Marker)annotation;
                    marker.setIcon(markerIcons.remove(0));
                    mapboxMap.updateMarker(marker);
                }
                else if (annotation instanceof Polyline) {
                    Polyline polyline = (Polyline)annotation;
                    polyline.setColor(polylineColors.remove(0));
                    mapboxMap.updatePolyline(polyline);
                }
            }

        }, BLINK_TIME);
    }

    /**
     * Briefly change color of geo-object.
     * @param geoObjectId
     */
    public void flashGeoObjectInColor(long geoObjectId, int color) {
        setGeoObjectColor(geoObjectId, color);
        new Handler().postDelayed(() -> setGeoObjectColor(geoObjectId, defaultGeoObjectColor), BLINK_TIME);
    }

    /**
     * @param geoObjectId
     * @param color
     */
    public void setGeoObjectColor(long geoObjectId, int color) {
        for (Annotation annotation : geoObjectAnnotations(geoObjectId)) {
            if (annotation instanceof Marker) {
                Marker marker = (Marker)annotation;
                marker.setIcon(generateNodeIcon(color));
                mapboxMap.updateMarker(marker);
            }
            else if (annotation instanceof  Polyline) {
                Polyline polyline = (Polyline)annotation;
                polyline.setColor(color);
                mapboxMap.updatePolyline(polyline);
            }
            else {
                throw new RuntimeException("Dead end");
            }
        }
    }

    //endregion

    //region working area highlighting fail
//    /**
//     * @param mapboxMap
//     * @param workingArea
//     */
//    public static void highlightWorkingArea(MapboxMap mapboxMap, NodeShape workingArea) {
////        JsonArray tl = new JsonArray(); tl.add(-180); tl.add(-LAT_MAX);
////        JsonArray tr = new JsonArray(); tr.add(180); tr.add(-LAT_MAX);
////        JsonArray br = new JsonArray(); br.add(180); br.add(LAT_MAX);
////        JsonArray bl = new JsonArray(); bl.add(-180); bl.add(LAT_MAX);
////        JsonArray outerPolygon = new JsonArray();
////        outerPolygon.add(tl); outerPolygon.add(tr); outerPolygon.add(br); outerPolygon.add(bl);
////
////        JsonArray hole = new JsonArray();
////        for (double[] node : workingArea.asClosed().getNodes()) {
////            JsonArray holeNode = new JsonArray();
////            holeNode.add(node[0]);
////            holeNode.add(node[1]);
////            hole.add(holeNode);
////        }
////
////
////        JsonArray coordinates = new JsonArray();
////        coordinates.add(outerPolygon);
////        coordinates.add(hole);
////
////        JsonObject top = new JsonObject();
////        top.addProperty("type", "Feature");
////
////        JsonObject properties = new JsonObject();
////        properties.addProperty("name", "GeoObject");
////        top.add("properties", properties);
////
////        JsonObject geometry = new JsonObject();
////        geometry.addProperty("type", "Polygon");
////        geometry.add("coordinates", coordinates);
////        top.add("geometry", geometry);
////
////        String geoJson = new Gson().toJson(top);
////
////        Feature polygon = Feature.fromJson(geoJson);
////        FeatureCollection collection = FeatureCollection.fromFeatures(new Feature[]{polygon});
////
////        Source geoJsonSource = new GeoJsonSource("poly-source", collection);
////        mapboxMap.addSource(geoJsonSource);
////
////        FillLayer fillLayer = new FillLayer("layerId", "poly-source");
////        fillLayer.setProperties(
////                fillColor(DIM_OVERLAY_COLOR)
////                //fillOpacity(0.7f)
////        );
////
////        mapboxMap.addLayer(fillLayer);
//
//
//        List<LatLng> hole = toLatLngs(workingArea.asClosed().getNodes());
////        Polygon polygon = mapboxMap.addPolygon(new PolygonOptions()
////                .addAll(LocaUtils.WORLD_CORNER_COORDINATES)
////                .addHole(hole)
////                .fillColor(LocaUtils.DIM_OVERLAY_COLOR));
//
//        mapboxMap.addPolyline(new PolylineOptions()
//                .addAll(hole)
//                .width(5)
//                .color(LocaUtils.DIM_OVERLAY_COLOR));
//    }

    //endregion

    //region map-interaction

    /**
     * Specify action when an object is clicked.
     * @param listener
     */
    public void setOnGeoObjectClick(OnGeoObjectClickListener listener) {
        mapboxMap.setOnMarkerClickListener(marker -> {
            Long geoObjectId = markersLookupMap.get(marker.getId());
            if (geoObjectId == null) return true;

            if (geoObjectId == workingAreaPolylineId) listener.onWorkingAreaBorderClick();
            else listener.onGeoObjectClick(geoObjectId);
            return true;
        });

        mapboxMap.setOnPolylineClickListener(polyline -> {
            Long geoObjectId = polylinesLookupMap.get(polyline.getId());
            if (geoObjectId == null) return;

            if (geoObjectId == workingAreaPolylineId) listener.onWorkingAreaBorderClick();
            else listener.onGeoObjectClick(geoObjectId);
        });
    }

    public interface OnGeoObjectClickListener {
        void onGeoObjectClick(long geoObjectId);
        default void onWorkingAreaBorderClick() {}
    }

    //endregion
}
