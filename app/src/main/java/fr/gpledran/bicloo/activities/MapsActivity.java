package fr.gpledran.bicloo.activities;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.gpledran.bicloo.R;
import fr.gpledran.bicloo.api.JCDecauxService;
import fr.gpledran.bicloo.common.DatabaseTask;
import fr.gpledran.bicloo.common.ItineraryTask;
import fr.gpledran.bicloo.common.Toolbox;
import fr.gpledran.bicloo.database.DatabaseHelper;
import fr.gpledran.bicloo.model.Station;
import fr.gpledran.bicloo.model.Stations;
import fr.gpledran.bicloo.common.StationsDeserializerJson;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

/**
 * The Maps activity.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final int REQUEST_FINE_LOCATION = 0;
    private static final String STATUS_OPEN = "OPEN";

    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private DatabaseHelper dbHelper;
    private Location lastLocation;
    private List<Station> stationList;
    private Marker myMarker;
    private Station selectedStation;
    private Marker selectedMarker;
    private Map<String, Integer> hashMapMarkers;
    private boolean isFilterAvailableBikesEnabled = false;
    private boolean isFilterAvailableBikeStandsEnabled = false;
    private boolean isFilterOpenStationEnabled = false;
    private boolean isShowItineraryFab = false;
    private boolean isAnimateShrink = false;
    private boolean isModeItinerary = false;
    private boolean isModeOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Chack location permissions
        checkLocationPermission();

        // SQLite database helper
        dbHelper = new DatabaseHelper(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Obtain the Material Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtain the CoordinateLayout
        final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinate_layout);

        // Floating Action Button
        final FloatingActionButton filterAvailibleBikeStandsFab = (FloatingActionButton) findViewById(R.id.available_bike_stands_filter_fab);
        final FloatingActionButton filterAvailibleBikesFab = (FloatingActionButton) findViewById(R.id.available_bikes_filter_fab);
        final FloatingActionButton filterOpenStationsFab = (FloatingActionButton) findViewById(R.id.status_open_filter_fab);
        final FloatingActionButton itineraryFab = (FloatingActionButton) findViewById(R.id.itinerary_fab);
        final FloatingActionButton positionFab = (FloatingActionButton) findViewById(R.id.my_position_fab);

        // Animation
        final Animation growAnimation = AnimationUtils.loadAnimation(this, R.anim.grow);
        final Animation shrinkAnimation = AnimationUtils.loadAnimation(this, R.anim.shrink);
        shrinkAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimateShrink = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                itineraryFab.setVisibility(View.GONE);
                isAnimateShrink = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // Obtain the BottomSheet
        final View bottomSheet = findViewById(R.id.bottom_sheet);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setPeekHeight(600);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        isShowItineraryFab = true;
                        // No itinerary in offline mode, because of calling google direction api
                        if (!isModeOffline) {
                            itineraryFab.setVisibility(View.VISIBLE);
                        }
                        break;

                    case BottomSheetBehavior.STATE_HIDDEN:
                        isShowItineraryFab = false;
                        if (selectedMarker != null) {
                            selectedMarker.hideInfoWindow();
                        }
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // No itinerary in offline mode, because of calling google direction api
                if (!isModeOffline) {
                    if (slideOffset < -0.2 && isShowItineraryFab && !isAnimateShrink) {
                        itineraryFab.startAnimation(shrinkAnimation);
                    } else if (slideOffset >= -0.2 && !isShowItineraryFab) {
                        itineraryFab.startAnimation(growAnimation);
                    }
                }
            }
        });

        // My Location
        if (googleApiClient == null) {
            // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
            // See https://g.co/AppIndexing/AndroidStudio for more information.
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(AppIndex.API).build();
        }

        // Available bikes FAB
        assert filterAvailibleBikesFab != null;
        filterAvailibleBikesFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFilterAvailableBikesEnabled = !isFilterAvailableBikesEnabled;
                refreshStationsOnMap(filterStations());
                changeStateFabAvailableBikes(filterAvailibleBikesFab);
            }
        });

        // Available bike stands FAB
        assert filterAvailibleBikeStandsFab != null;
        filterAvailibleBikeStandsFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFilterAvailableBikeStandsEnabled = !isFilterAvailableBikeStandsEnabled;
                refreshStationsOnMap(filterStations());
                changeStateFabAvailableBikeStands(filterAvailibleBikeStandsFab);
            }
        });

        // Open stations FAB
        assert filterOpenStationsFab != null;
        filterOpenStationsFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFilterOpenStationEnabled = !isFilterOpenStationEnabled;
                refreshStationsOnMap(filterStations());
                changeStateFabOpenStations(filterOpenStationsFab);
            }
        });

        // My position FAB
        assert positionFab != null;
        positionFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check Permission
                if (checkLocationPermission()) {
                    if (Toolbox.isLocationAvailable(MapsActivity.this) && lastLocation != null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 16.0f));
                        if (myMarker != null) {
                            myMarker.remove();
                        }
                        myMarker = map.addMarker(new MarkerOptions().position(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())));
                    } else {
                        showSnackbar("Localisation désactivée");
                    }
                }
            }
        });

        // Itinerary FAB
        assert itineraryFab != null;
        itineraryFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hide BottomSheet
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                // Check Permission
                if (checkLocationPermission()) {
                    // Check GPS & location
                    if (Toolbox.isLocationAvailable(MapsActivity.this) && lastLocation != null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 16.0f));

                        // Mode itinerary
                        isModeItinerary = true;

                        // Async drawing itinerary
                        String origin = String.valueOf(lastLocation.getLatitude()) + "," + String.valueOf(lastLocation.getLongitude());
                        String destination = String.valueOf(selectedStation.getPosition().getLat()) + "," + String.valueOf(selectedStation.getPosition().getLng());
                        new ItineraryTask(MapsActivity.this, coordinatorLayout, map, origin, destination, selectedStation.getName()).execute();

                        // Hide Menu FAB
                        FloatingActionsMenu menuFab = (FloatingActionsMenu) findViewById(R.id.fab_menu);
                        menuFab.setVisibility(View.GONE);
                    } else {
                        showSnackbar("Localisation désactivée");
                    }
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
        if (Toolbox.isNetworkAvailable(this)) {
            getStationsFromJCDecauxAPI();
        }
        else {
            // Offline Mode only on application loading
            isModeOffline = true;

            // Hide Menu FAB
            FloatingActionsMenu menuFab = (FloatingActionsMenu) findViewById(R.id.fab_menu);
            menuFab.setVisibility(View.GONE);

            // Get Stations for SQLite databse
            stationList = dbHelper.getAllStations();

            if (stationList.size() > 0) {
                // Center map & show stations
                LatLng nantes = new LatLng(47.2185256, -1.55408);
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(nantes, 15.0f));
                refreshStationsOnMap(stationList);
                showSnackbar("Mode hors connexion");
            }
            else {
                showSnackbar("Connexion internet requise au premier lancement de l'application");
            }
        }

        // Marker click listener
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                selectedMarker = marker;
                selectedMarker.showInfoWindow();
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedMarker.getPosition(), 15.0f));

                collapseMenuFab();
                openBottomSheet(marker);
                return true;
            }
        });
    }

    private void getStationsFromJCDecauxAPI() {
        Toolbox.showProgressBar(findViewById(R.id.progress_overlay));

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

                // Center camera on Nantes, only if no station is selected
                if (!isShowingStation()) {
                    LatLng nantes = new LatLng(47.2185256, -1.55408);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(nantes, 15.0f));
                }

                // Add marker on map
                refreshStationsOnMap(filterStations());

                // Async insert stations in database
                new DatabaseTask(MapsActivity.this, stationList).execute();

                Toolbox.hideProgressBar(findViewById(R.id.progress_overlay));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("JCDecaux API ERROR" , "Error : " + error.toString());
                showSnackbar("Erreur lors de la récupération des données");

                Toolbox.hideProgressBar(findViewById(R.id.progress_overlay));
            }
        });
    }

    private List<Station> filterStations() {
        List<Station> filteredList = new ArrayList<>();

        Station currentStation;
        for (int i=0; i<stationList.size(); i++) {
            currentStation = stationList.get(i);
            if (isFilterAvailableBikesEnabled && isFilterAvailableBikeStandsEnabled && isFilterOpenStationEnabled) {
                if (currentStation.getAvailableBikes() > 0 && currentStation.getAvailableBikeStands() > 0 && STATUS_OPEN.equalsIgnoreCase(currentStation.getStatus())) {
                    filteredList.add(currentStation);
                }
            }
            else if (isFilterAvailableBikesEnabled && isFilterAvailableBikeStandsEnabled) {
                if (currentStation.getAvailableBikes() > 0 && currentStation.getAvailableBikeStands() > 0) {
                    filteredList.add(currentStation);
                }
            }
            else if (isFilterAvailableBikesEnabled && isFilterOpenStationEnabled) {
                if (currentStation.getAvailableBikes() > 0 && STATUS_OPEN.equalsIgnoreCase(currentStation.getStatus())) {
                    filteredList.add(currentStation);
                }
            }
            else if (isFilterAvailableBikeStandsEnabled && isFilterOpenStationEnabled) {
                if (currentStation.getAvailableBikeStands() > 0 && STATUS_OPEN.equalsIgnoreCase(currentStation.getStatus())) {
                    filteredList.add(currentStation);
                }
            }
            else if (isFilterAvailableBikesEnabled) {
                if (currentStation.getAvailableBikes() > 0) {
                    filteredList.add(currentStation);
                }
            }
            else if (isFilterAvailableBikeStandsEnabled) {
                if (currentStation.getAvailableBikeStands() > 0) {
                    filteredList.add(currentStation);
                }
            }
            else if (isFilterOpenStationEnabled) {
                if (STATUS_OPEN.equalsIgnoreCase(currentStation.getStatus())) {
                    filteredList.add(currentStation);
                }
            }
            else {
                filteredList.add(currentStation);
            }
        }

        return filteredList;
    }

    private void refreshStationsOnMap(List<Station> filteredList) {
        // Clear all markers, polylines and circle
        map.clear();

        // Instantiate HashMap use to show station in bottom sheet
        hashMapMarkers = new HashMap<>();

        // Add markers station
        Station currentStation;
        Marker currentMarker;
        for (int i=0; i<filteredList.size(); i++) {
            currentStation = filteredList.get(i);
            currentStation.setName(currentStation.getName().substring(currentStation.getName().indexOf("-")+1).trim());
            currentMarker = map.addMarker(new MarkerOptions()
                                .position(new LatLng(currentStation.getPosition().getLat(), currentStation.getPosition().getLng()))
                                .title(currentStation.getName())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

            // Hashmap to retrieve station by marker id
            // Search position on full station list because method receives a filtered list
            hashMapMarkers.put(currentMarker.getId(), stationList.indexOf(currentStation));

            // On refresh action, show title on marker station selected
            if (isShowingStation() && currentStation.getNumber().equals(selectedStation.getNumber())) {
                currentMarker.showInfoWindow();
            }
        }
    }

    private void showSelectedStationOnMap(Station station, int position) {
        // Clear all markers, polylines and circle
        map.clear();

        // Instantiate HashMap use to show station in bottom sheet
        hashMapMarkers = new HashMap<>();

        Marker marker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(station.getPosition().getLat(), station.getPosition().getLng()))
                    .title(station.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        hashMapMarkers.put(marker.getId(), position);

        // Center map on selected station ans show title
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15.0f));
        marker.showInfoWindow();

        // Show Bottom Sheet
        openBottomSheet(marker);

        // Hide Menu FAB
        FloatingActionsMenu menuFab = (FloatingActionsMenu) findViewById(R.id.fab_menu);
        menuFab.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onBackPressed() {
        // Close app if bottom sheet already hidden
        if (!closeBottomSheet()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        showSnackbar("Déconnecté des services Google");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showSnackbar("Pas de connexion aux services Google");
    }

    protected void startLocationUpdates() {
        if (Build.VERSION.SDK_INT < 23 || ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Create the location request
            locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(10000)
                    .setFastestInterval(2000);

            // Request location updates
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid());
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                showExplainPermissionSnackbar();
            }
            else {
                askForLocationPermission();
            }
            return false;
        }
        else { return true; }
    }

    private void askForLocationPermission()
    {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_FINE_LOCATION);
    }

    private void showParamPermissionSnackbar()
    {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinate_layout);
        Snackbar.make(coordinatorLayout, "Vous avez désactivé la permission Localisation", Snackbar.LENGTH_LONG).setAction("Paramètres", new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                final Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }).show();
    }

    private void showExplainPermissionSnackbar()
    {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinate_layout);
        Snackbar.make(coordinatorLayout, "Permission Localisation requise", Snackbar.LENGTH_LONG).setAction("Activer", new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                askForLocationPermission();
            }
        }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                }
                else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]) == false)
                {
                    showParamPermissionSnackbar();
                }
                else
                {
                    showExplainPermissionSnackbar();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(false);
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
                String stationName = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
                for (int i=0; i < stationList.size(); i++) {
                    if (stationList.get(i).getName().equalsIgnoreCase(stationName)) {
                        // Show station on map and collapse searchview
                        showSelectedStationOnMap(stationList.get(i), i);
                        searchView.clearFocus();
                        searchView.setIconified(true);
                        menu.findItem(R.id.action_search).collapseActionView();
                        break;
                    }
                }
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Prevent search keyboard action
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                //onSearchRequested();
                return true;

            case R.id.action_refresh:
                if (Toolbox.isNetworkAvailable(this)) {
                    isModeOffline = false;

                    // Get stations from API
                    getStationsFromJCDecauxAPI();

                    // Refresh selected stations informations
                    if (isShowingStation()) {
                        refreshBottomSheetInformations();
                    }

                    // Not showing itinerary
                    isModeItinerary = false;

                    // Menu FAB
                    collapseMenuFab();
                }
                else {
                    // Hide BottomSheet and inform no internet connection
                    closeBottomSheet();
                    showSnackbar("Pas de connexion internet");
                }
                return true;

            default:
                // No user's action, invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private boolean closeBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return true;
        }
        return false;
    }

    private void openBottomSheet(Marker marker) {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Don't show bottom sheet on user's location marker
        if ((myMarker != null && myMarker.equals(marker)) || isModeItinerary) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }

        // Fill station informations
        selectedStation =  stationList.get(hashMapMarkers.get(marker.getId()));
        refreshBottomSheetInformations();

        // Show only important's informations : name, available bikes and stands
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void refreshBottomSheetInformations() {
        TextView name = (TextView) findViewById(R.id.station_name);
        assert name != null;
        name.setText(String.format("%s - %s",
                selectedStation.getNumber(),
                selectedStation.getName().substring(selectedStation.getName().indexOf("-")+1).trim()));

        TextView banking = (TextView) findViewById(R.id.station_banking);
        assert banking != null;
        banking.setText(selectedStation.getBanking() ? "Avec terminal de paiement" : "Sans terminal de paiement");


        TextView status = (TextView) findViewById(R.id.station_status);
        assert status != null;
        if (!isModeOffline) {
            status.setText(STATUS_OPEN.equalsIgnoreCase(selectedStation.getStatus()) ? "Station ouverte" : "Station fermée");
        }
        else { status.setText("-"); }

        TextView availableBikeStands = (TextView) findViewById(R.id.station_available_bike_stands);
        assert availableBikeStands != null;
        if (!isModeOffline) {
            availableBikeStands.setText(String.format("%s %s",
                    selectedStation.getAvailableBikeStands().toString(),
                    (selectedStation.getAvailableBikeStands() > 0 ? "places disponibles" : "place disponible")));
            availableBikeStands.setTextColor(getAvailabilityColor(selectedStation.getAvailableBikeStands(), selectedStation.getBikeStands()));
        }
        else {
            availableBikeStands.setText("-");
            availableBikeStands.setTextColor((Color.BLACK));
        }

        ImageView availableBikeStandsImg = (ImageView) findViewById(R.id.image_station_available_bike_stands);
        assert availableBikeStandsImg != null;
        availableBikeStandsImg.setColorFilter(availableBikeStands.getCurrentTextColor());

        TextView availableBikes = (TextView) findViewById(R.id.station_available_bikes);
        assert availableBikes != null;
        if (!isModeOffline) {
            availableBikes.setText(String.format("%s %s",
                    selectedStation.getAvailableBikes().toString(),
                    (selectedStation.getAvailableBikes() > 0 ? "vélos disponibles" : "vélo disponible")));
            availableBikes.setTextColor(getAvailabilityColor(selectedStation.getAvailableBikes(), selectedStation.getBikeStands()));
        }
        else {
            availableBikes.setText("-");
            availableBikes.setTextColor(Color.BLACK);
        }

        ImageView availableBikesImg = (ImageView) findViewById(R.id.image_station_available_bikes);
        assert availableBikesImg != null;
        availableBikesImg.setColorFilter(availableBikes.getCurrentTextColor());
    }

    private int getAvailabilityColor(int available, int max) {
        if (available <= 2) {
            // Dark Primary Color RED
            return Color.parseColor("#" + Integer.toHexString(ContextCompat.getColor(MapsActivity.this, R.color.colorRedDark)));
        }
        else if (available > 2 && available < (max/2)) {
            // Dark Primary Color ORANGE
            return Color.parseColor("#" + Integer.toHexString(ContextCompat.getColor(MapsActivity.this, R.color.colorOrangeDark)));
        }
        else {
            // Dark Primary Color GREEN
            return Color.parseColor("#" + Integer.toHexString(ContextCompat.getColor(MapsActivity.this, R.color.colorGreenDark)));
        }
    }

    private void showSnackbar(String text) {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinate_layout);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_LONG);
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void collapseMenuFab() {
        FloatingActionsMenu menuFab = (FloatingActionsMenu) findViewById(R.id.fab_menu);
        menuFab.setVisibility(View.VISIBLE);
        menuFab.collapse();
    }

    private boolean isShowingStation() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        return !(selectedStation == null || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN);
    }

    private void changeStateFabOpenStations(FloatingActionButton filterOpenStationsFab) {
        if (isFilterOpenStationEnabled) {
            filterOpenStationsFab.setIcon(R.drawable.ic_access_time_white_24dp);
            filterOpenStationsFab.setColorNormal(ContextCompat.getColor(MapsActivity.this, R.color.colorGreenDark));
            filterOpenStationsFab.setColorPressed(ContextCompat.getColor(MapsActivity.this, R.color.colorGreenDark));
        }
        else {
            filterOpenStationsFab.setIcon(R.drawable.ic_access_time_green_700_24dp);
            filterOpenStationsFab.setColorNormal(ContextCompat.getColor(MapsActivity.this, R.color.white));
            filterOpenStationsFab.setColorPressed(ContextCompat.getColor(MapsActivity.this, R.color.white));
        }
    }

    private void changeStateFabAvailableBikes(FloatingActionButton filterAvailibleBikesFab) {
        if (isFilterAvailableBikesEnabled) {
            filterAvailibleBikesFab.setIcon(R.drawable.ic_directions_bike_white_24dp);
            filterAvailibleBikesFab.setColorNormal(ContextCompat.getColor(MapsActivity.this, R.color.colorGreenDark));
            filterAvailibleBikesFab.setColorPressed(ContextCompat.getColor(MapsActivity.this, R.color.colorGreenDark));
        }
        else {
            filterAvailibleBikesFab.setIcon(R.drawable.ic_directions_bike_green_700_24dp);
            filterAvailibleBikesFab.setColorNormal(ContextCompat.getColor(MapsActivity.this, R.color.white));
            filterAvailibleBikesFab.setColorPressed(ContextCompat.getColor(MapsActivity.this, R.color.white));
        }
    }

    private void changeStateFabAvailableBikeStands(FloatingActionButton filterAvailibleBikeStandsFab) {
        if (isFilterAvailableBikeStandsEnabled) {
            filterAvailibleBikeStandsFab.setIcon(R.drawable.ic_local_parking_white_24dp);
            filterAvailibleBikeStandsFab.setColorNormal(ContextCompat.getColor(MapsActivity.this, R.color.colorGreenDark));
            filterAvailibleBikeStandsFab.setColorPressed(ContextCompat.getColor(MapsActivity.this, R.color.colorGreenDark));
        }
        else {
            filterAvailibleBikeStandsFab.setIcon(R.drawable.ic_local_parking_green_700_24dp);
            filterAvailibleBikeStandsFab.setColorNormal(ContextCompat.getColor(MapsActivity.this, R.color.white));
            filterAvailibleBikeStandsFab.setColorPressed(ContextCompat.getColor(MapsActivity.this, R.color.white));
        }
    }
}
