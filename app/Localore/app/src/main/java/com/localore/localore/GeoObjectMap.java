package com.localore.localore;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.Nullable;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wrapper of the MapboxMap for displaying geo-objects represented as markers and poly-lines-annotations.
 * Annotations are normally colored after the standard-geo-object-color value of the map (which may be
 * individual colors based on geo-object-property). They can also be set to any color but toggling
 * between colors etc will not work as expected.
 *
 * (Not persistent, so recreate when Activity created).
 */
public class GeoObjectMap {

    /**
     * Side-length of the circle of a node-icon.
     */
    public static final int NODE_INSIDE_WAY_ICON_DIAMETER = 20;
    public static final int STANDALONE_NODE_ICON_DIAMETER = 50;

    /**
     * Thickness of the way-objects line-representation.
     */
    float GEO_OBJECT_LINE_WIDTH = 4;

    public static final int GEO_OBJECT_PROPERTY_COLOR = -1;
    public static final int BORDER_OBJECT_ID = -2;

    /**
     * Time used when objects are blinked/flashed..
     */
//    public static final int BLINK_TIME_LONG = 2000;
//    public static final int BLINK_TIME_SHORT = 1000;
    public static final int BLINK_TIME = 300;
    public static final int FLASH_TIME = 1000;

    private MapboxMap mapboxMap;
    private NodeShape border;
    private FloatingActionButton toggleZoomButton;
    private Context context;

    /**
     * Default display-color of all geo-objects. May be GEO_OBJECT_PROPERTY_COLOR for individual
     * colors based on geo-object's property-color.
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
                        @Nullable FloatingActionButton toggleZoomButton, Context context) {
        mapboxMap.clear();
        this.mapboxMap = mapboxMap;
        this.defaultGeoObjectColor = defaultGeoObjectColor;
        this.border = border;
        this.toggleZoomButton = toggleZoomButton;
        this.context = context;

        if (showBorder) {
            addPolyline(border.asExtraClosed().getNodes(), BORDER_OBJECT_ID, Color.LTGRAY);
        }

        if (toggleZoomButton != null) {
            setToggleZoomButton(OVERVIEW_ZOOM);
            toggleZoomButton.setOnClickListener(view -> {
                onToggleZoomClick();
            });
        }
    }

    /**
     * Remove all geo-objects. Keep border if present.
     */
    public void clearGeoObjects() {
        boolean showBorder = getKeysFromValue(BORDER_OBJECT_ID, polylinesLookupMap).size() != 0;
        mapboxMap.clear();
        markersLookupMap.clear();
        polylinesLookupMap.clear();

        if (showBorder)
            addPolyline(border.asExtraClosed().getNodes(), BORDER_OBJECT_ID, Color.LTGRAY);
    }

    /**
     * Reset camera to overview.
     */
    public void resetCamera(int flyTime) {
        setToggleZoomButton(OVERVIEW_ZOOM);
        flyToOverview(flyTime);
    }

    //region Zoom-toggle-button
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
            setToggleZoomButton(OVERVIEW_ZOOM);
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
     * @param color
     * @return Added stuff.
     */
    public void addGeoObject(GeoObject geoObject, int color) {
        if (geoObject.isNode()) {
            addMarker(geoObject.getNodes().get(0), geoObject.getId(), color, STANDALONE_NODE_ICON_DIAMETER);
        }
        else if (useMarkerSize(geoObject.getBounds())) {
            addMarker(geoObject.getCenter(), geoObject.getId(), color, STANDALONE_NODE_ICON_DIAMETER);
        }
        else {
            for (NodeShape nodeShape : geoObject.getShapes()) {
                if (nodeShape.getNodes().size() == 1) {
                    addMarker(nodeShape.getNodes().get(0), geoObject.getId(), color, NODE_INSIDE_WAY_ICON_DIAMETER);
                }
                else if (useMarkerSize(nodeShape.getBounds())) {
                    addMarker(nodeShape.getCenter(), geoObject.getId(), color, NODE_INSIDE_WAY_ICON_DIAMETER);
                }
                else {
                    nodeShape = nodeShape.isClosed() ? nodeShape.asExtraClosed() : nodeShape;
                    addPolyline(nodeShape.getNodes(), geoObject.getId(), color);
                }
            }
        }
    }

    /**
     * Geo-objects/ node-shapes with bounds of bs or less should be displayed as a single
     * marker on the map - if this function returns true.
     * @param bounds
     * @return True if use marker.
     */
    private boolean useMarkerSize(double[] bounds) {
        double SMALL_SIDE_LENGTH = 100; //meters

        double[] ws = new double[]{bounds[0], bounds[1]};
        double[] es = new double[]{bounds[2], bounds[1]};
        double[] wn = new double[]{bounds[0], bounds[3]};
        return LocaUtils.distance(ws, es) < SMALL_SIDE_LENGTH &&
                LocaUtils.distance(ws, wn) < SMALL_SIDE_LENGTH;
    }

    public void addGeoObject(GeoObject geoObject) {
        int color = defaultGeoObjectColor == GEO_OBJECT_PROPERTY_COLOR ? geoObject.getColor() : defaultGeoObjectColor;
        addGeoObject(geoObject, color);
    }

    /**
     * Adds a marker. Updates id-map if not null.
     * @param node
     * @param geoObjectId -1 of not part of a geo-object.
     * @param color
     */
    public Marker addMarker(double[] node, long geoObjectId, int color, int diameter) {
        Marker marker = mapboxMap.addMarker(new MarkerOptions()
                .icon(generateNodeIcon(color, diameter))
                .position(toLatLng(node)));

        if (geoObjectId != -1) this.markersLookupMap.put(marker.getId(), geoObjectId);
        return marker;
    }

    /**
     * Adds a polyline. Updates id-map if not null.
     * @param nodes
     * @param geoObjectId -1 of not part of a geo-object.
     * @param color
     */
    public Polyline addPolyline(List<double[]> nodes, long geoObjectId, int color) {
        Polyline polyline = mapboxMap.addPolyline(new PolylineOptions()
                .addAll(toLatLngs(nodes))
                .color(color)
                .width( GEO_OBJECT_LINE_WIDTH ));

        if (geoObjectId != -1) this.polylinesLookupMap.put(polyline.getId(), geoObjectId);
        return polyline;
    }

    //endregion

    //region create a node-icon

    /**
     * @param color
     * @param diameter
     * @return Icon of a node-geo-object.
     */
    private Icon generateNodeIcon(int color, int diameter) {
        return LocaUtils.generateCircleIcon(color, diameter, context);
    }

    //endregion

    //region Remove geo-objects


    /**
     * @param geoObjectId
     */
    public void removeGeoObject(long geoObjectId) {
        List<Annotation> annotations = geoObjectAnnotations(geoObjectId);
        mapboxMap.removeAnnotations(annotations);

        for (long markerId : getKeysFromValue(geoObjectId, markersLookupMap)) {
            markersLookupMap.remove(markerId);
        }
        for (long polylineId : getKeysFromValue(geoObjectId, polylinesLookupMap)) {
            polylinesLookupMap.remove(polylineId);
        }
    }

    /**
     * Remove all except one.
     * @param geoObjectId
     */
    public void removeAllGeoObjectsExcept(long geoObjectId) {
        for (Iterator<Map.Entry<Long, Long>> iterator = markersLookupMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, Long> entry = iterator.next();
            if (entry.getValue() != geoObjectId) {
                iterator.remove();
                mapboxMap.removeAnnotation(entry.getKey());
            }
        }

        for (Iterator<Map.Entry<Long, Long>> iterator = polylinesLookupMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, Long> entry = iterator.next();
            if (entry.getValue() != geoObjectId) {
                iterator.remove();
                mapboxMap.removeAnnotation(entry.getKey());
            }
        }
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
    private void flyToOverview(int flyTime) {
        flyToFitBounds(toLatLngBounds(border.getBounds()), flyTime);
    }

    private void flyToObjects(int flyTime) {
        List<LatLng> allLatLngs = allPoints(mapboxMap.getAnnotations());
        if (allLatLngs.size() == 0) {
            flyToOverview(flyTime);
        }
        else if (allLatLngs.size() == 1) {
            flyToLocation(allLatLngs.get(0), CAMERA_FITTING_MAX_ZOOM, flyTime);
        }
        else {
            LatLngBounds bounds = new LatLngBounds.Builder().includes(allLatLngs).build();
            flyToFitBounds(bounds, flyTime);
        }
    }

    /**
     * Move camera gradually to fit the bounds including padding.
     * @param bounds
     * @param flyTime
     */
    private void flyToFitBounds(LatLngBounds bounds, int flyTime) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, CAMERA_FITTING_PADDING);
        CameraPosition cameraPosition = cameraUpdate.getCameraPosition(mapboxMap);

        if (cameraPosition.zoom > CAMERA_FITTING_MAX_ZOOM) {
            flyToLocation(cameraPosition.target, CAMERA_FITTING_MAX_ZOOM, flyTime);
        }
        else {
            mapboxMap.animateCamera(cameraUpdate, flyTime);
        }
    }

    /**
     * Move camera gradually to new location.
     * @param latLng
     */
    private void flyToLocation(LatLng latLng, double zoom, int flyTime) {
        CameraPosition position = new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoom)
                .build();

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), flyTime);
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

    //region Geo-object -> annotations

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

    //region Blink geo-objects


    /**
     * Briefly displays a geo-object not currently in the map.
     * @param geoObject
     */
    public void flashGeoObject(GeoObject geoObject, int color) {
        if (isInMap(geoObject.getId())) return;

        int displayTime = 1500;
        addGeoObject(geoObject, color);
        new Handler().postDelayed(() -> removeGeoObject(geoObject.getId()), displayTime);
    }
    public void flashGeoObject(GeoObject geoObject) {
        flashGeoObject(geoObject, geoObject.getColor());
    }

    /**
     * Hide, then show a geo-object in the map.
     */
    public void blinkGeoObject(long geoObjectId) {
        if (!isInMap(geoObjectId)) return;

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
            if (!isInMap(geoObjectId)) return;

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
     * Briefly change color of geo-object in the map. The back again to default-geo-object-color (map-defined).
     * @param geoObject
     */
    public void flashGeoObjectInColor(GeoObject geoObject, int color) {
        if (!isInMap(geoObject.getId())) return;

        setGeoObjectColor(geoObject, color);
        new Handler().postDelayed(() -> {
            if (!isInMap(geoObject.getId())) return;
            setGeoObjectColor(geoObject, defaultGeoObjectColor);
        }, FLASH_TIME);
    }
    public void flashGeoObjectInColor(GeoObject geoObject) {
        flashGeoObjectInColor(geoObject, geoObject.getColor());
    }

    /**
     * Sets new color of geo-object in map. (A an unwanted side-effect, marker-size reverted to default).
     * @param geoObject
     * @param color
     */
    public void setGeoObjectColor(GeoObject geoObject, int color) {
        if (!isInMap(geoObject.getId())) return;

        color = color == GEO_OBJECT_PROPERTY_COLOR ? geoObject.getColor() : color;
        for (Annotation annotation : geoObjectAnnotations(geoObject.getId())) {
            if (annotation instanceof Marker) {
                Marker marker = (Marker)annotation;
                if (geoObject.isNode() || useMarkerSize(geoObject.getBounds())) {
                    marker.setIcon(generateNodeIcon(color, STANDALONE_NODE_ICON_DIAMETER));
                }
                else {
                    marker.setIcon(generateNodeIcon(color, NODE_INSIDE_WAY_ICON_DIAMETER));
                }
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
    public void setGeoObjectColor(GeoObject geoObject) {
        setGeoObjectColor(geoObject, geoObject.getColor());
    }

    /**
     * Boost the display-size of a geo-object.
     * @param geoObject
     */
    public void boostGeoObjectSize(GeoObject geoObject, Context context) {
        for (Annotation annotation : geoObjectAnnotations(geoObject.getId())) {
            if (annotation instanceof Marker) {
                Marker marker = (Marker)annotation;
                Icon currentIcon = marker.getIcon();
                int currentIconDiameter = currentIcon.getBitmap().getWidth();
                marker.setIcon(LocaUtils.generateCircleIcon(geoObject.getColor(), currentIconDiameter, context));
                mapboxMap.updateMarker(marker);
            }
            else if (annotation instanceof Polyline) {
                Polyline polyline = (Polyline)annotation;
                float width = GEO_OBJECT_LINE_WIDTH * 2;
                polyline.setWidth(width);
                mapboxMap.updatePolyline(polyline);
            }
            else {
                throw new RuntimeException("Dead end");
            }
        }
    }

    //endregion

    //region Working area highlighting fail
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

    //region Map-interaction

    /**
     * Specify action when an object is clicked.
     * @param listener
     */
    public void setOnGeoObjectClick(OnGeoObjectClickListener listener) {
        mapboxMap.setOnMarkerClickListener(marker -> {
            Long geoObjectId = markersLookupMap.get(marker.getId());
            if (geoObjectId == null) return true;
            else listener.onGeoObjectClick(geoObjectId);
            return true;
        });

        mapboxMap.setOnPolylineClickListener(polyline -> {
            Long geoObjectId = polylinesLookupMap.get(polyline.getId());
            if (geoObjectId == null) return;
            if (geoObjectId == BORDER_OBJECT_ID) listener.onWorkingAreaBorderClick();
            else listener.onGeoObjectClick(geoObjectId);
        });
    }

    public interface OnGeoObjectClickListener {
        void onGeoObjectClick(long geoObjectId);
        default void onWorkingAreaBorderClick() {}
    }

    //endregion

    //region Utils

    /**
     * @param geoObjectId
     * @return True if geoObject is currently in the map.
     */
    public boolean isInMap(long geoObjectId) {
        return getKeysFromValue(geoObjectId, markersLookupMap).size() > 0 ||
                getKeysFromValue(geoObjectId, polylinesLookupMap).size() > 0;
    }

    /**
     * Remove map-listeners.
     */
    public void removeMapListeners() {
        mapboxMap.setOnMarkerClickListener(null);
        mapboxMap.setOnPolylineClickListener(null);
    }

    /**
     * @param annotations Markers and poly-lines.
     * @return All points making up annotations, except border.
     */
    private List<LatLng> allPoints(List<Annotation> annotations) {
        List<LatLng> latLngs = new ArrayList<>();

        for (Annotation annotation : annotations) {
            if (annotation instanceof Marker) {
                latLngs.add(((Marker)annotation).getPosition());
            }
            else if (annotation instanceof Polyline) {
                if (polylinesLookupMap.get(annotation.getId()) == BORDER_OBJECT_ID) continue;
                latLngs.addAll(((Polyline)annotation).getPoints());
            }
            else {
                throw new RuntimeException("Dead end");
            }
        }
        return latLngs;
    }

    //endregion
}
