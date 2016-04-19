package fr.gpledran.bicloo.api;

import fr.gpledran.bicloo.model.Stations;
import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

public interface JCDecauxService {

    public static final String BASE_URL = "https://api.jcdecaux.com";
    public static final String API_KEY = "YOUR_API_KEY";

    @GET("/vls/v1/stations")
    public void listStations(@Query("contract") String contract, @Query("apiKey") String apiKey, Callback<Stations> response);
}
