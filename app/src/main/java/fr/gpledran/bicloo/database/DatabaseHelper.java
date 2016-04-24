package fr.gpledran.bicloo.database;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.gpledran.bicloo.model.Position;
import fr.gpledran.bicloo.model.Station;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Bicloo.db";
    private static final int DATABASE_VERSION = 1;

    public static final String STATION_TABLE_NAME = "station";
    public static final String STATION_COLUMN_ID = "_id";
    public static final String STATION_COLUMN_NUMBER = "number";
    public static final String STATION_COLUMN_NAME = "name";
    public static final String STATION_COLUMN_ADDRESS = "address";
    public static final String STATION_COLUMN_LAT = "lat";
    public static final String STATION_COLUMN_LNG = "lng";
    public static final String STATION_COLUMN_BANKING = "banking";
    public static final String STATION_COLUMN_BONUS = "bonus";
    public static final String STATION_COLUMN_STATUS = "status";
    public static final String STATION_COLUMN_CONTRACT_NAME = "contract_name";
    public static final String STATION_COLUMN_BIKE_STANDS = "bike_stands";
    public static final String STATION_COLUMN_AVAILABLE_BIKE_STANDS = "available_bike_stands";
    public static final String STATION_COLUMN_AVAILABLE_BIKES = "available_bikes";
    public static final String STATION_COLUMN_LAST_UPDATE = "last_update";

    private HashMap<String, String> aliasMap;

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
        db.execSQL(
                "CREATE TABLE " + STATION_TABLE_NAME +
                        "(" + STATION_COLUMN_ID + " INTEGER PRIMARY KEY, " +
                        STATION_COLUMN_NUMBER + " INTEGER UNIQUE," +
                        STATION_COLUMN_NAME + " TEXT, " +
                        STATION_COLUMN_ADDRESS + " TEXT," +
                        STATION_COLUMN_LAT + " REAL," +
                        STATION_COLUMN_LNG + " REAL," +
                        STATION_COLUMN_BANKING + " INTEGER," +
                        STATION_COLUMN_BONUS + " INTEGER," +
                        STATION_COLUMN_STATUS + " TEXT," +
                        STATION_COLUMN_CONTRACT_NAME + " TEXT," +
                        STATION_COLUMN_BIKE_STANDS + " INTEGER," +
                        STATION_COLUMN_AVAILABLE_BIKE_STANDS + " INTEGER," +
                        STATION_COLUMN_AVAILABLE_BIKES + " INTEGER," +
                        STATION_COLUMN_LAST_UPDATE + " INTEGER)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + STATION_TABLE_NAME);
        onCreate(db);
    }

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

    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, STATION_TABLE_NAME);
        db.close();
        return numRows;
    }

    public Cursor getStation(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("SELECT * FROM " + STATION_TABLE_NAME + " WHERE " +
                STATION_COLUMN_ID + "=?", new String[]{Integer.toString(id)});
        db.close();
        return res;
    }

    public Cursor getAllStations() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "SELECT * FROM " + STATION_TABLE_NAME, null );
        db.close();
        return res;
    }

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

    public Cursor getStation(String id){

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        queryBuilder.setTables(STATION_TABLE_NAME);

        Cursor cursor = queryBuilder.query(this.getReadableDatabase(),
            new String[] { "_id", "name", "flag", "currency" } ,
            "_id = ?", new String[] { id } , null, null, null ,"1"
        );

        return cursor;
    }

    public List<Station> read(String searchText) {
        List<Station> stationList = new ArrayList<>();

        String query = "SELECT * FROM " + STATION_TABLE_NAME +
                       "WHERE " + STATION_TABLE_NAME + " LIKE '%" + searchText + "%'" +
                       "ORDER BY " + STATION_TABLE_NAME + " DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String objectName = cursor.getString(cursor.getColumnIndex(STATION_COLUMN_NAME));
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
}
