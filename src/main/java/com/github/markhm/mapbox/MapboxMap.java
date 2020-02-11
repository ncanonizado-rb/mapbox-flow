package com.github.markhm.mapbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.internal.JsonSerializer;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

public class MapboxMap extends Div
{
    private static Log log = LogFactory.getLog(MapboxMap.class);

    private Page page = null;

    boolean alreadyRendered = false;

    private GeoLocation initialView = null;
    private int initialZoom;
    boolean dkMap = false;

    private MapboxOptions options = null;

    private MapboxMap()
    {
        setId("map");
        getStyle().set("align-self", "center");
        getStyle().set("border", "1px solid black");

        setWidth("1200px");
        setHeight("700px");

        page = UI.getCurrent().getPage();
    }

    public MapboxMap(MapboxOptions options)
    {
        this();

        this.options = options;
    }

    public MapboxMap(GeoLocation initialView, int initialZoom)
    {
        this(initialView, initialZoom, false);
    }


    public MapboxMap(GeoLocation initialView, int initialZoom, boolean dkMap)
    {
        this();

        this.initialView = initialView;
        this.initialZoom = initialZoom;
        this.dkMap = dkMap;

        if (!alreadyRendered)
        {
            render(dkMap);
            alreadyRendered = true;
        }
    }

    private void render(boolean dkMap)
    {
        page.addStyleSheet("https://api.tiles.mapbox.com/mapbox-gl-js/v1.7.0/mapbox-gl.css");
        page.addJavaScript("https://api.tiles.mapbox.com/mapbox-gl-js/v1.7.0/mapbox-gl.js");
        page.addJavaScript("https://api.tiles.mapbox.com/mapbox.js/plugins/turf/v2.0.0/turf.min.js");

        page.addStyleSheet("./mapbox.css");
        page.addJavaScript("./mapbox.js");

        String accessToken = AccessToken.getToken();

        executeJs("mapboxgl.accessToken = '" + accessToken + "';");

        // render mapbox
        options = new MapboxOptions();
        options.setInitialZoom(initialZoom);
        options.setInitialView(initialView);

        // This works to create a map, but should not.
        executeJs("renderDefaultMap(" + initialView.getLongLat() + ", " + initialZoom + ")");

        // The correct way should be as follows, but this does not work.
        // executeJs("renderCustomMap($0, $1, $2);", getMapStyle(dkMap), initialView.getLongLat(), initialZoom);

        // This does not work, neither does the call that follows.
        // executeJs("renderOptionsMap($0);", getJsonObject());
        // executeJs("renderOptionsMap(" + options + ");");

        // add full screen control
        executeJs("map.addControl(new mapboxgl.FullscreenControl())");
    }

    private String getMapStyle(boolean dkMap)
    {
        if (dkMap)
        {
            return "mapbox://styles/markhm/ck4b4hiy41bmh1ck5ns089mhh";
        }
        else
        {
            return "mapbox://styles/mapbox/streets-v11";
        }
    }

    public void addAnimatedItem(AnimatedItem animatedItem)
    {
        Layer carLayer = new Layer(animatedItem.getLayerId(), "symbol");

        GeoLocation initialPosition = animatedItem.getLocation();
        log.info("Adding " + animatedItem.getDescription() + " to initial position " + initialPosition + " on map.");

        Layer.Properties carProperties = new Layer.Properties("", animatedItem.getSprite().toString());
        Layer.Feature carFeature = new Layer.Feature("Feature", carProperties, initialPosition);
        carLayer.addFeature(carFeature);

        log.info("CARLAYER: "+carLayer); // see below

        executeJs("addLayer($0);", carLayer.toString());
    }

    public void addLine(Geometry geometry, Color color)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter writer = objectMapper.writerFor(Geometry.class);

        String geometryAsString = null;

        try
        {
            geometryAsString = writer.writeValueAsString(geometry);
        }
        catch (JsonProcessingException jpe)
        {
            log.error(jpe);
        }

        // This should work, but it doesn't.
        // executeJs("addLine($0, $1);", geometryAsString, color.toStringForJS());

        // This shouldn't work, but it does.
        page.executeJs("addLine("+geometryAsString +", "+ color.toStringForJS()+")");

        // This should be identical to the previous call, and yes, here it works (NB: in drawOriginDestinationFlight(..) below, it does not).
        // executeJs("addLine("+geometryAsString +", "+ color.toStringForJS()+")");
    }

    public void removeAnimatedItem(AnimatedItem animatedItem)
    {
        executeJs("removeLayer($0);", animatedItem.getLayerId());
    }

    public void zoomTo(GeoLocation geoLocation, int zoomLevel)
    {
        // This should work, but does not
        // executeJs("map.flyTo({center: $0, zoomLevel: $1});", geoLocation.getLongLat(), zoomLevel);

        // This should not work, but does.
        executeJs("map.flyTo({center: " + geoLocation.getLongLat() + ", zoom: " + zoomLevel + "})");
    }

    public void flyTo(GeoLocation geoLocation)
    {
        // This should work, but does not
        // executeJs("map.flyTo({center: $0 });", geoLocation.getLongLat());

        // This works
        executeJs("map.flyTo({center: "+geoLocation.getLongLat()+"})");
    }

    public void zoomTo(int zoomLevel)
    {
        page.executeJs("zoomTo($0);", zoomLevel);
    }

    public void startAnimation()
    {
        // This works as expected
        executeJs("startAnimation();");
    }

    public void drawOriginDestinationFlight(GeoLocation origin, GeoLocation destination)
    {
        // Expected to work but does not.
        // executeJs("fromOriginToDestination($0, $1);", origin.getLongLat() ,destination.getLongLat());

        // Should not work, but does
        page.executeJs("fromOriginToDestination(" + origin.getLongLat() + ", " + destination.getLongLat() + ");");

        // Even more strange, the following does not work, which is really identical to the previous method.
        // executeJs("fromOriginToDestination(" + origin.getLongLat() + ", " + origin.getLongLat() + ")");
    }

    private JsonObject getJsonObject()
    {
        JsonObject jsonObject = Json.createObject();

        jsonObject.put(MapboxOptions.OptionType.container.toString(), "map");
        jsonObject.put(MapboxOptions.OptionType.style.toString(), "mapbox://styles/mapbox/streets-v11");


        jsonObject.put(MapboxOptions.OptionType.center.toString(), GeoLocation.InitialView_Denmark.getLongLat());
        jsonObject.put(MapboxOptions.OptionType.zoom.toString(), 6);

        return jsonObject;
    }

    public void executeJs(String javaScript, Serializable... parameters)
    {
        page.executeJs(javaScript, parameters);
    }

    public void executeJs(String javaScript)
    {
        page.executeJs(javaScript);
    }

    // Documents regarding importing
    // https://github.com/vaadin/flow/issues/6582
    // https://vaadin.com/forum/thread/14045163/how-to-pack-server-side-java-script-in-executable-jar-file
    // https://vaadin.com/docs/v14/flow/importing-dependencies/tutorial-ways-of-importing.html
    // https://vaadin.com/forum/thread/18059914
}

// --------------------------------------------------------------------------------------------------------------------

// DRAGONS here, please ignore

//TODO The problem seems to be that the String returned from initialView.getLongLat() arrives at the other side as a String,
// where it needs to be an object.
// Strangely enough, the toJSON() method does not help sufficiently.
// WE SHOULD PROBABLY USE VAADIN's OWN JSON FRAMEWORK

// --------------------------------------------------------------------------------------------------------------------
//        {
//            "id":"routeLine",
//                "type":"line",
//                "source":
//            {
//                "type":"geojson",
//                    "data":
//                {
//                    "type":"Feature",
//                        "properties":{},
//                    "geometry":
//                    {
//                        "coordinates":[
//                    [55.755825,37.617298],
//                    [55.6761,12.5683]
//                    ],
//                        "type":"LineString"
//                    }
//                }
//            },
//            "layout":{"line-join":"round","line-cap":"round"},
//            "paint":{"line-color":{"line-color":"#377E21"},"line-width":3}
//        }

//        UI ui = getUI().get();
//        ui.access(() ->
//        {
//
//        });

// --------------------------------------------------------------------------------------------------------------------

//    {
//        "layout":
//        {
//            "text-field":["get","title"],
//            "text-offset":[0,0.7],
//            "text-anchor":"top",
//                "icon-image":["concat",["get","icon"],"-15"],
//            "text-font":["Open Sans  Semibold"]
//        },
//        "id":"cars",
//            "source":
//        {
//            "data":
//            {
//                "features":
//                        [{"geometry":
//                {
//                    "coordinates":[5.55361,52.026443],
//                    "type":"Point"
//                },
//                "type":"Feature",
//                        "properties": {"icon":"car","title":""}}
//                        ],
//                "type":"FeatureCollection"
//            },
//            "type":"geojson"
//        },
//        "type":"symbol"
//    }