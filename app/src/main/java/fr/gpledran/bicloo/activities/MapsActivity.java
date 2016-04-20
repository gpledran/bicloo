package fr.gpledran.bicloo.activities;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.GsonBuilder;

import java.util.List;

import fr.gpledran.bicloo.R;
import fr.gpledran.bicloo.api.JCDecauxService;
import fr.gpledran.bicloo.model.Station;
import fr.gpledran.bicloo.model.Stations;
import fr.gpledran.bicloo.model.StationsDeserializerJson;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private List<Station> stationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Obtain the Material Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtain the BottomSheet
        final View bottomSheet = findViewById(R.id.bottom_sheet);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // React to dragging events

            }
        });
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setPeekHeight(600);

        // My position FAB
//        FloatingActionButton myPositionFAB = findViewById(R.id.my_position_fab);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Center camera on Nantes
        LatLng nantes = new LatLng(47.2185256, -1.55408);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(nantes, 15.0f));

        // Create an adapter for retrofit with base url
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(JCDecauxService.BASE_URL)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .registerTypeAdapter(Stations.class, new StationsDeserializerJson())
                        .create()))
                .build();

        // Creating a service for adapter with our GET class
        JCDecauxService jcDecauxService = restAdapter.create(JCDecauxService.class);

        //Now ,we need to call for response
        //Retrofit using gson for JSON-POJO conversion
        jcDecauxService.listStations("Nantes", JCDecauxService.API_KEY, new Callback<Stations>() {
            @Override
            public void success(Stations stations, Response response) {
                stationList = stations.getStations();
                Station currentStation;
                for (int i=0; i<stationList.size(); i++) {
                    currentStation = stationList.get(i);
                    map.addMarker(new MarkerOptions()
                                        .position(new LatLng(currentStation.getPosition().getLat(), currentStation.getPosition().getLng()))
                                        .title(currentStation.getName())
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

                }
                map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        marker.showInfoWindow();
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15.0f));

                        openBottomSheet(marker);
                        return true;
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("JCDecaux API ERROR" , "Error : " + error.toString());

                CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinate_layout);
                Snackbar snackbar = Snackbar.make(coordinatorLayout, "Erreur lors de la récupération des données", Snackbar.LENGTH_LONG);

                snackbar.show();
            }
        });
    }

    private void openBottomSheet(Marker marker) {
        int markerPosition = Integer.parseInt(marker.getId().substring(1));
        Station stationToShow =  stationList.get(markerPosition);

        TextView name = (TextView) findViewById(R.id.station_name);
        name.setText(stationToShow.getName());

        TextView banking = (TextView) findViewById(R.id.station_banking);
        banking.setText(stationToShow.getBanking() ? "Avec terminal de paiement" : "Sans terminal de paiement");

        TextView status = (TextView) findViewById(R.id.station_status);
        status.setText("OPEN".equalsIgnoreCase(stationToShow.getStatus()) ? "Station ouverte" : "Station fermée");

        TextView availableBikeStands = (TextView) findViewById(R.id.station_available_bike_stands);
        availableBikeStands.setText(stationToShow.getAvailableBikeStands().toString() +
                (stationToShow.getAvailableBikeStands() > 0 ? " places disponibles" : " place disponible"));

        TextView availableBikes = (TextView) findViewById(R.id.station_available_bikes);
        availableBikes.setText(stationToShow.getAvailableBikes().toString() +
                (stationToShow.getAvailableBikes() > 0 ? " vélos disponibles" : " vélo disponible"));

        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.action_search:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
