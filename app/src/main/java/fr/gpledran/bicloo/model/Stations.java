package fr.gpledran.bicloo.model;

import java.util.LinkedList;
import java.util.List;

/**
 * The class Stations.
 */
public class Stations {
    private List<Station> stations;

    /**
     * Gets stations.
     *
     * @return the stations
     */
    public List<Station> getStations() {
        return stations;
    }

    /**
     * Add.
     *
     * @param stationItem the station item
     */
    public void add(Station stationItem){
        if(stations == null){
            stations = new LinkedList<>();
        }
        stations.add(stationItem);
    }
}
