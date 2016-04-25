package fr.gpledran.bicloo.common;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import fr.gpledran.bicloo.database.DatabaseHelper;
import fr.gpledran.bicloo.model.Station;

public class DatabaseTask extends AsyncTask<Void, Integer, Boolean> {

    private Context context;
    private List<Station> stationList = new ArrayList<>();

    public DatabaseTask(final Context context, final List<Station> stationList) {
        this.context = context;
        this.stationList = stationList;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            // SQL Lite helper
            DatabaseHelper dbHelper = new DatabaseHelper(context);

            // Insert stations
            dbHelper.insertStations(stationList);
            return true;
        }
        catch(final Exception e) {
            return false;
        }
    }
}
