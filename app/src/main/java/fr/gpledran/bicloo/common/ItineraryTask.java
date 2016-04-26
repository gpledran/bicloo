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

import fr.gpledran.bicloo.R;

/**
 * The class Itinerary task.
 */
public class ItineraryTask extends AsyncTask<Void, Integer, Boolean> {

    private static final String TOAST_ERR_MAJ = "Impossible de trouver un itinéraire";

    private Context context;
    private CoordinatorLayout coordinatorLayout;
    private GoogleMap gMap;
    private String origin;
    private String destination;
    private String title;
    private final ArrayList<LatLng> listLatLng = new ArrayList<LatLng>();

    /**
     * Instantiates a new Itinerary task.
     *
     * @param context           the context
     * @param coordinatorLayout the coordinator layout
     * @param gMap              the g map
     * @param origin            the origin
     * @param destination       the destination
     * @param title             the title
     */
    public ItineraryTask(final Context context, final CoordinatorLayout coordinatorLayout, final GoogleMap gMap, final String origin, final String destination, final String title) {
        this.context = context;
        this.coordinatorLayout = coordinatorLayout;
        this.gMap= gMap;
        this.origin = origin;
        this.destination = destination;
        this.title = title;
    }

    @Override
    protected void onPreExecute() {
        Toolbox.showProgressBar(coordinatorLayout.findViewById(R.id.progress_overlay));
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
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

            listLatLng.add(new LatLng((double)lat/1E5, (double)lng/1E5));
        }
    }

    @Override
    protected void onPostExecute(final Boolean result) {
        if(!result) {
            Snackbar snackbar = Snackbar.make(coordinatorLayout, TOAST_ERR_MAJ, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
        else {
            final PolylineOptions polylines = new PolylineOptions();
            polylines.color(Color.parseColor("#179FFF"));
            polylines.width(20);
            polylines.zIndex(2);

            final PolylineOptions borders = new PolylineOptions();
            borders.color(Color.parseColor("#116CCD"));
            borders.width(25);
            borders.zIndex(1);

            // Construct polyline & border
            for(final LatLng latLng : listLatLng) {
                polylines.add(latLng);
                borders.add(latLng);
            }

            // Destination RED marker
            final MarkerOptions destinationMarker = new MarkerOptions();
            destinationMarker.position(listLatLng.get(listLatLng.size()-1));
            destinationMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            destinationMarker.title(title);

            // Clear map for UX
            gMap.clear();

            // Update map with polylines
            gMap.addPolyline(borders);
            gMap.addPolyline(polylines);
            gMap.addMarker(destinationMarker).showInfoWindow();

            // Update maps with circles
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(listLatLng.get(0));
            circleOptions.radius(8);
            circleOptions.fillColor(Color.WHITE);
            circleOptions.strokeWidth(3.0f);
            circleOptions.strokeColor(Color.parseColor("#116CCD"));
            circleOptions.zIndex(3);
            gMap.addCircle(circleOptions);

            circleOptions.center(listLatLng.get(listLatLng.size()-1));
            gMap.addCircle(circleOptions);

            // Zoom
//            LatLngBounds.Builder builder = new LatLngBounds.Builder();
//            builder.include(listLatLng.get(0));
//            builder.include(listLatLng.get(listLatLng.size()-1));
//            gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 250));
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(listLatLng.get(0), 16.0f));
        }

        Toolbox.hideProgressBar(coordinatorLayout.findViewById(R.id.progress_overlay));
    }
}

