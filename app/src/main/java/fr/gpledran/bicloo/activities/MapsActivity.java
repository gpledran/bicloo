package fr.gpledran.bicloo.activities;

import android.Manifest;
import android.location.Location;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.gpledran.bicloo.R;
import fr.gpledran.bicloo.api.JCDecauxService;
import fr.gpledran.bicloo.common.ItineraryTask;
import fr.gpledran.bicloo.model.Station;
import fr.gpledran.bicloo.model.Stations;
import fr.gpledran.bicloo.common.StationsDeserializerJson;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private List<Station> stationList;
    private Marker myMarker;
    private Station selectedStation;
    private Map<String, Integer> hashMapMarkers;

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

        // Obtain the CoordinateLayout
        final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinate_layout);

        // Obtain the BottomSheet
        final View bottomSheet = findViewById(R.id.bottom_sheet);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {}

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setPeekHeight(600);

        // My Location
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // My position FAB
        FloatingActionButton positionFab = (FloatingActionButton) findViewById(R.id.my_position_fab);
        assert positionFab != null;
        positionFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid());
                lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

                if (lastLocation != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 16.0f));
                    if (myMarker != null) {
                        myMarker.remove();
                    }
                    myMarker = map.addMarker(new MarkerOptions().position(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())));
                }
            }
        });

        // Itinerary FAB
        FloatingActionButton itineraryFab = (FloatingActionButton) findViewById(R.id.itinerary_fab);
        assert itineraryFab != null;
        itineraryFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid());
                lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

                if (lastLocation != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 16.0f));

                    // Hide BottomSheet
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                    // Async drawing itinerary
                    String origin = String.valueOf(lastLocation.getLatitude()) + "," + String.valueOf(lastLocation.getLongitude());
                    String destination = String.valueOf(selectedStation.getPosition().getLat()) + "," + String.valueOf(selectedStation.getPosition().getLng());
                    new ItineraryTask(MapsActivity.this, coordinatorLayout, map, origin, destination, selectedStation.getName()).execute();
                }
            }
        });
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

        // Get stations from API
        getStationsFromJCDecauxAPI();

        // Marker click listener
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

    private void getStationsFromJCDecauxAPI() {
        // Create an adapter for retrofit with base url
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(JCDecauxService.BASE_URL)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .registerTypeAdapter(Stations.class, new StationsDeserializerJson())
                        .create()))
                .build();

        // Creating a service for adapter with our GET class
        JCDecauxService jcDecauxService = restAdapter.create(JCDecauxService.class);

        // Retrofit callback
        jcDecauxService.listStations("Nantes", JCDecauxService.API_KEY, new Callback<Stations>() {
            @Override
            public void success(Stations stations, Response response) {
                stationList = stations.getStations();
                addStationsToMap();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("JCDecaux API ERROR" , "Error : " + error.toString());
                showSnackbar("Erreur lors de la récupération des données");
            }
        });
    }

    private void addStationsToMap() {
        // Center camera on Nantes
        LatLng nantes = new LatLng(47.2185256, -1.55408);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(nantes, 15.0f));

        // Clear all markers, polylines and circle
        map.clear();

        // Instantiate HashMap use to show station in bottom sheet
        hashMapMarkers = new HashMap<>();

        // Add markers station
        Station currentStation;
        Marker currentMarker;
        for (int i=0; i<stationList.size(); i++) {
            currentStation = stationList.get(i);
            currentMarker = map.addMarker(new MarkerOptions()
                                .position(new LatLng(currentStation.getPosition().getLat(), currentStation.getPosition().getLng()))
                                .title(currentStation.getName())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            hashMapMarkers.put(currentMarker.getId(), i);
        }
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        //showSnackbar("Connecté");
    }

    @Override
    public void onConnectionSuspended(int i) {
        showSnackbar("Déconnecté des services Google");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showSnackbar("Pas de connexion aux services Google");
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
            case R.id.action_search:
                return true;

            case R.id.action_refresh:
                // Get stations from API
                getStationsFromJCDecauxAPI();
                return true;

            case R.id.action_settings:
                return true;

            default:
                // No user's action, invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void openBottomSheet(Marker marker) {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Don't show bottom sheet on user's location marker
        if (myMarker != null && myMarker.equals(marker)) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }

        // Fill station informations
        setBottomSheetInformations(marker);

        // Show only important's informations : name, available bikes and stands
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void setBottomSheetInformations(Marker marker) {
        // Get the selection station to show
        selectedStation =  stationList.get(hashMapMarkers.get(marker.getId()));

        TextView name = (TextView) findViewById(R.id.station_name);
        assert name != null;
        name.setText(selectedStation.getName());

        TextView banking = (TextView) findViewById(R.id.station_banking);
        assert banking != null;
        banking.setText(selectedStation.getBanking() ? "Avec terminal de paiement" : "Sans terminal de paiement");

        TextView status = (TextView) findViewById(R.id.station_status);
        assert status != null;
        status.setText("OPEN".equalsIgnoreCase(selectedStation.getStatus()) ? "Station ouverte" : "Station fermée");

        TextView availableBikeStands = (TextView) findViewById(R.id.station_available_bike_stands);
        assert availableBikeStands != null;
        availableBikeStands.setText(String.format("%s %s",
                selectedStation.getAvailableBikeStands().toString(),
                (selectedStation.getAvailableBikeStands() > 0 ? "places disponibles" : "place disponible")));

        TextView availableBikes = (TextView) findViewById(R.id.station_available_bikes);
        assert availableBikes != null;
        availableBikes.setText(String.format("%s %s",
                selectedStation.getAvailableBikes().toString(),
                (selectedStation.getAvailableBikes() > 0 ? "vélos disponibles" : "vélo disponible")));
    }

    private void showSnackbar(String text) {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinate_layout);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_LONG);

        snackbar.show();
    }
}
