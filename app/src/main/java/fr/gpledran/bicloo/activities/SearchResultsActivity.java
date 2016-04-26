package fr.gpledran.bicloo.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import fr.gpledran.bicloo.provider.StationContentProvider;

/**
 * The Search results activity.
 */
public class SearchResultsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_SEARCH:
                String query = intent.getStringExtra(SearchManager.QUERY);
                doSearch(query);
                break;
        }
    }

    private void doSearch(String query){
        Bundle data = new Bundle();
        data.putString("query", query);

        // Invoking onCreateLoader() in non-ui thread
        getSupportLoaderManager().initLoader(1, data, this);
    }

    /** This method is invoked by initLoader() */
    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle data) {
        Uri uri = StationContentProvider.CONTENT_URI;
        return new CursorLoader(getBaseContext(), uri, null, null , new String[]{data.getString("query")}, null);
    }

    /** This method is executed in ui thread, after onCreateLoader() */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {}


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}
}
