package fr.gpledran.bicloo.database;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.gpledran.bicloo.model.Position;
import fr.gpledran.bicloo.model.Station;

/**
 * The type Database helper.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    /**
     * The constant DATABASE_NAME.
     */
    public static final String DATABASE_NAME = "Bicloo.db";
    private static final int DATABASE_VERSION = 1;

    /**
     * The constant STATION_TABLE_NAME.
     */
    public static final String STATION_TABLE_NAME = "station";
    /**
     * The constant STATION_COLUMN_ID.
     */
    public static final String STATION_COLUMN_ID = "_id";
    /**
     * The constant STATION_COLUMN_NUMBER.
     */
    public static final String STATION_COLUMN_NUMBER = "number";
    /**
     * The constant STATION_COLUMN_NAME.
     */
    public static final String STATION_COLUMN_NAME = "name";
    /**
     * The constant STATION_COLUMN_ADDRESS.
     */
    public static final String STATION_COLUMN_ADDRESS = "address";
    /**
     * The constant STATION_COLUMN_LAT.
     */
    public static final String STATION_COLUMN_LAT = "lat";
    /**
     * The constant STATION_COLUMN_LNG.
     */
    public static final String STATION_COLUMN_LNG = "lng";
    /**
     * The constant STATION_COLUMN_BANKING.
     */
    public static final String STATION_COLUMN_BANKING = "banking";
    /**
     * The constant STATION_COLUMN_BONUS.
     */
    public static final String STATION_COLUMN_BONUS = "bonus";
    /**
     * The constant STATION_COLUMN_STATUS.
     */
    public static final String STATION_COLUMN_STATUS = "status";
    /**
     * The constant STATION_COLUMN_CONTRACT_NAME.
     */
    public static final String STATION_COLUMN_CONTRACT_NAME = "contract_name";
    /**
     * The constant STATION_COLUMN_BIKE_STANDS.
     */
    public static final String STATION_COLUMN_BIKE_STANDS = "bike_stands";
    /**
     * The constant STATION_COLUMN_AVAILABLE_BIKE_STANDS.
     */
    public static final String STATION_COLUMN_AVAILABLE_BIKE_STANDS = "available_bike_stands";
    /**
     * The constant STATION_COLUMN_AVAILABLE_BIKES.
     */
    public static final String STATION_COLUMN_AVAILABLE_BIKES = "available_bikes";
    /**
     * The constant STATION_COLUMN_LAST_UPDATE.
     */
    public static final String STATION_COLUMN_LAST_UPDATE = "last_update";

    private HashMap<String, String> aliasMap;

    /**
     * Instantiates a new Database helper.
     *
     * @param context the context
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME , null, DATABASE_VERSION);

        // This HashMap is used to map table fields to Custom Suggestion fields
        aliasMap = new HashMap<String, String>();

        // Unique id for the each Suggestions
        aliasMap.put("_ID", STATION_COLUMN_ID + " as " + "_id" );

        // Text for Suggestions
        aliasMap.put(SearchManager.SUGGEST_COLUMN_TEXT_1, STATION_COLUMN_NAME + " as " + SearchManager.SUGGEST_COLUMN_TEXT_1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder query = new StringBuilder("CREATE TABLE ");
        query.append(STATION_TABLE_NAME).append("(");
        query.append(STATION_COLUMN_ID).append(" INTEGER PRIMARY KEY, ");
        query.append(STATION_COLUMN_NUMBER).append(" INTEGER UNIQUE,");
        query.append(STATION_COLUMN_NAME).append(" TEXT, ");
        query.append(STATION_COLUMN_ADDRESS).append(" TEXT, ");
        query.append(STATION_COLUMN_LAT).append(" REAL,");
        query.append(STATION_COLUMN_LNG).append(" REAL,");
        query.append(STATION_COLUMN_BANKING).append(" INTEGER,");
        query.append(STATION_COLUMN_BONUS).append(" INTEGER,");
        query.append(STATION_COLUMN_STATUS).append(" TEXT,");
        query.append(STATION_COLUMN_CONTRACT_NAME).append(" TEXT,");
        query.append(STATION_COLUMN_BIKE_STANDS).append(" INTEGER,");
        query.append(STATION_COLUMN_AVAILABLE_BIKE_STANDS).append(" INTEGER,");
        query.append(STATION_COLUMN_AVAILABLE_BIKES).append(" INTEGER,");
        query.append(STATION_COLUMN_LAST_UPDATE).append(" INTEGER)");

        db.execSQL(query.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + STATION_TABLE_NAME);
        onCreate(db);
    }

    /**
     * Insert stations boolean.
     *
     * @param stationList the station list
     * @return the boolean
     */
    public boolean insertStations(List<Station> stationList) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Delete old datas
        db.delete(STATION_TABLE_NAME, null, null);

        // Insert new
        Station station;
        for (int i=0; i<stationList.size(); i++) {
            // Current station to insert
            station = stationList.get(i);

            // Prepare content values to insert
            ContentValues contentValues = new ContentValues();
            contentValues.put(STATION_COLUMN_NUMBER, station.getNumber());
            contentValues.put(STATION_COLUMN_NAME, station.getName().substring(station.getName().indexOf("-")+1).trim());
            contentValues.put(STATION_COLUMN_ADDRESS, station.getAddress());
            contentValues.put(STATION_COLUMN_LAT, station.getPosition().getLat());
            contentValues.put(STATION_COLUMN_LNG, station.getPosition().getLng());
            contentValues.put(STATION_COLUMN_BANKING, station.getBanking() ? 1 : 0);
            contentValues.put(STATION_COLUMN_BONUS, station.getBonus() ? 1 : 0);
            contentValues.put(STATION_COLUMN_STATUS, station.getBonus());
            contentValues.put(STATION_COLUMN_CONTRACT_NAME, station.getContractName());
            contentValues.put(STATION_COLUMN_BIKE_STANDS, station.getBikeStands());
            contentValues.put(STATION_COLUMN_AVAILABLE_BIKE_STANDS, station.getAvailableBikeStands());
            contentValues.put(STATION_COLUMN_AVAILABLE_BIKES, station.getAvailableBikes());
            contentValues.put(STATION_COLUMN_LAST_UPDATE, station.getLastUpdate());

            // Insert row
            db.insert(STATION_TABLE_NAME, null, contentValues);
        }

        db.close();
        return true;
    }

    /**
     * Number of rows int.
     *
     * @return the int
     */
    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, STATION_TABLE_NAME);
        db.close();
        return numRows;
    }

    /**
     * Gets station.
     *
     * @param id the id
     * @return the station
     */
    public Cursor getStation(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("SELECT * FROM " + STATION_TABLE_NAME + " WHERE " +
                STATION_COLUMN_ID + "=?", new String[]{Integer.toString(id)});
        db.close();
        return res;
    }

    /**
     * Gets all stations.
     *
     * @return the all stations
     */
    public List<Station> getAllStations() {
        List<Station> stationList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor =  db.rawQuery( "SELECT * FROM " + STATION_TABLE_NAME, null );

        if (cursor.moveToFirst()) {
            do {
                Station station = new Station(
                    cursor.getInt(cursor.getColumnIndex(STATION_COLUMN_NUMBER)),
                    cursor.getString(cursor.getColumnIndex(STATION_COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndex(STATION_COLUMN_ADDRESS)),
                    new Position(
                            cursor.getDouble(cursor.getColumnIndex(STATION_COLUMN_LAT)),
                            cursor.getDouble(cursor.getColumnIndex(STATION_COLUMN_LNG))
                    ),
                    cursor.getInt(cursor.getColumnIndex(STATION_COLUMN_BANKING)) == 1,
                    cursor.getInt(cursor.getColumnIndex(STATION_COLUMN_BONUS)) == 1,
                    cursor.getString(cursor.getColumnIndex(STATION_COLUMN_STATUS)),
                    cursor.getString(cursor.getColumnIndex(STATION_COLUMN_CONTRACT_NAME)),
                    cursor.getInt(cursor.getColumnIndex(STATION_COLUMN_BIKE_STANDS)),
                    cursor.getInt(cursor.getColumnIndex(STATION_COLUMN_AVAILABLE_BIKE_STANDS)),
                    cursor.getInt(cursor.getColumnIndex(STATION_COLUMN_AVAILABLE_BIKES)),
                    cursor.getInt(cursor.getColumnIndex(STATION_COLUMN_LAST_UPDATE))
                );

                stationList.add(station);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return stationList;
    }

    /**
     * Get stations cursor.
     *
     * @param selectionArgs the selection args
     * @return the cursor
     */
    public Cursor getStations(String[] selectionArgs){

        String selection = STATION_COLUMN_NAME + " like ? ";

        if(selectionArgs!=null){
            selectionArgs[0] = "%" + selectionArgs[0] + "%";
        }

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setProjectionMap(aliasMap);

        queryBuilder.setTables(STATION_TABLE_NAME);

        Cursor cursor = queryBuilder.query(
            this.getReadableDatabase(),
            new String[] {
                "_ID",
                SearchManager.SUGGEST_COLUMN_TEXT_1
            },
            selection,
            selectionArgs,
            null,
            null,
            STATION_COLUMN_NAME + " ASC ",
            "10"
        );

        return cursor;
    }
}
