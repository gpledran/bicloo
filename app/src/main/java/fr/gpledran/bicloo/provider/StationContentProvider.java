package fr.gpledran.bicloo.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import fr.gpledran.bicloo.database.DatabaseHelper;

public class StationContentProvider extends ContentProvider {

    public static final String AUTHORITY = "fr.gpledran.bicloo.provider.StationContentProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/stations" );

    private static final int SUGGESTIONS_STATION = 1;
    private static final int SEARCH_STATION = 2;
    private static final int GET_STATION = 3;

    private DatabaseHelper dbHelper;
    private UriMatcher uriMatcher = buildUriMatcher();

    private UriMatcher buildUriMatcher(){
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Suggestion items of Search Dialog is provided by this uri
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SUGGESTIONS_STATION);

        // This URI is invoked, when user presses "Go" in the Keyboard of Search Dialog
        // Listview items of SearchableActivity is provided by this uri
        uriMatcher.addURI(AUTHORITY, "stations", SEARCH_STATION);

        // This URI is invoked, when user selects a suggestion from search dialog or an item from the listview
        // Country details for CountryActivity is provided by this uri
        // See, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID in CountryDB.java
        uriMatcher.addURI(AUTHORITY, "stations/#", GET_STATION);

        return uriMatcher;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;

        switch(uriMatcher.match(uri)){
            case SUGGESTIONS_STATION :
                cursor = dbHelper.getStations(selectionArgs);
                break;
            case SEARCH_STATION :
                cursor = dbHelper.getStations(selectionArgs);
                break;
            case GET_STATION :
                int id = Integer.parseInt(uri.getLastPathSegment());
                cursor = dbHelper.getStation(id);
        }

        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        throw new UnsupportedOperationException();
    }
}
