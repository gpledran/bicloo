package fr.gpledran.bicloo.model;

import java.util.LinkedList;
import java.util.List;

public class Stations {
    private List<Station> stations;

    public List<Station> getStations() {
        return stations;
    }

    public void add(Station stationItem){
        if(stations == null){
            stations = new LinkedList<>();
        }
        stations.add(stationItem);
    }
}
