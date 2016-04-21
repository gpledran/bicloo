package fr.gpledran.bicloo.common;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ItineraryTask extends AsyncTask<Void, Integer, Boolean> {

    private static final String TOAST_MSG = "Calcul de l'itinéraire en cours";
    private static final String TOAST_ERR_MAJ = "Impossible de trouver un itinéraire";

    private Context context;
    private CoordinatorLayout coordinatorLayout;
    private GoogleMap gMap;
    private String origin;
    private String destination;
    private final ArrayList<LatLng> lstLatLng = new ArrayList<LatLng>();

    public ItineraryTask(final Context context, final CoordinatorLayout coordinatorLayout, final GoogleMap gMap, final String origin, final String destination) {
        this.context = context;
        this.coordinatorLayout = coordinatorLayout;
        this.gMap= gMap;
        this.origin = origin;
        this.destination = destination;
    }

    @Override
    protected void onPreExecute() {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, TOAST_MSG, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            // URL to call
            final StringBuilder url = new StringBuilder("http://maps.googleapis.com/maps/api/directions/xml?sensor=false&language=fr&mode=walking");
            url.append("&origin=");
            url.append(origin);
            url.append("&destination=");
            url.append(destination);

            // Calling API
            final InputStream stream = new URL(url.toString()).openStream();

            // Get datas
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setIgnoringComments(true);

            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            final Document document = documentBuilder.parse(stream);
            document.getDocumentElement().normalize();

            // Status request
            final String status = document.getElementsByTagName("status").item(0).getTextContent();
            if(!"OK".equals(status)) {
                return false;
            }

            // Steps
            final Element elementLeg = (Element) document.getElementsByTagName("leg").item(0);
            final NodeList nodeListStep = elementLeg.getElementsByTagName("step");
            final int length = nodeListStep.getLength();

            for(int i=0; i<length; i++) {
                final Node nodeStep = nodeListStep.item(i);

                if(nodeStep.getNodeType() == Node.ELEMENT_NODE) {
                    final Element elementStep = (Element) nodeStep;

                    // Points
                    decodePolylines(elementStep.getElementsByTagName("points").item(0).getTextContent());
                }
            }

            return true;
        }
        catch(final Exception e) {
            return false;
        }
    }

    private void decodePolylines(final String encodedPoints) {
        int index = 0;
        int lat = 0, lng = 0;

        while (index < encodedPoints.length()) {
            int b, shift = 0, result = 0;

            do {
                b = encodedPoints.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;

            do {
                b = encodedPoints.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            lstLatLng.add(new LatLng((double)lat/1E5, (double)lng/1E5));
        }
    }

    @Override
    protected void onPostExecute(final Boolean result) {
        if(!result) {
            Snackbar snackbar = Snackbar.make(coordinatorLayout, TOAST_ERR_MAJ, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
        else {
            //On déclare le polyline, c'est-à-dire le trait (ici bleu) que l'on ajoute sur la carte pour tracer l'itinéraire
            final PolylineOptions polylines = new PolylineOptions();
            polylines.color(Color.parseColor("#179FFF"));
            polylines.width(20);
            polylines.zIndex(2);

            final PolylineOptions borders = new PolylineOptions();
            borders.color(Color.parseColor("#116CCD"));
            borders.width(25);
            borders.zIndex(1);

            // Construct polyline & border
            for(final LatLng latLng : lstLatLng) {
                polylines.add(latLng);
                borders.add(latLng);
            }

            // Destination RED marker
            final MarkerOptions destinationMarker = new MarkerOptions();
            destinationMarker.position(lstLatLng.get(lstLatLng.size()-1));
            destinationMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            // Clear map for UX
            gMap.clear();

            // Update map with polylines
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lstLatLng.get(0), 16.0f));
            gMap.addPolyline(borders);
            gMap.addPolyline(polylines);
            gMap.addMarker(destinationMarker);

            // Update maps with circles
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(lstLatLng.get(0));
            circleOptions.radius(8);
            circleOptions.fillColor(Color.WHITE);
            circleOptions.strokeWidth(3.0f);
            circleOptions.strokeColor(Color.parseColor("#116CCD"));
            circleOptions.zIndex(3);
            gMap.addCircle(circleOptions);

            circleOptions.center(lstLatLng.get(lstLatLng.size()-1));
            gMap.addCircle(circleOptions);
        }
    }
}

