package fr.gpledran.bicloo.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class StationsDeserializerJson implements JsonDeserializer<Stations> {

    @Override
    public Stations deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Stations stations = new Stations();
        JsonArray jsonArray = json.getAsJsonArray();
        Gson gson = new Gson();
        for(JsonElement element : jsonArray){
            JsonObject jsonObject = element.getAsJsonObject();
            Station station = gson.fromJson(jsonObject, Station.class);
            stations.add(station);
        }
        return stations;
    }
}
