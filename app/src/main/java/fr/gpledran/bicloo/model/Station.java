package fr.gpledran.bicloo.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The class Station.
 */
public class Station {

    /**
     * Instantiates a new Station.
     *
     * @param number              the number
     * @param name                the name
     * @param address             the address
     * @param position            the position
     * @param banking             the banking
     * @param bonus               the bonus
     * @param status              the status
     * @param contractName        the contract name
     * @param bikeStands          the bike stands
     * @param availableBikeStands the available bike stands
     * @param availableBikes      the available bikes
     * @param lastUpdate          the last update
     */
    public Station(Integer number, String name, String address, Position position, Boolean banking,
                   Boolean bonus, String status, String contractName, Integer bikeStands,
                   Integer availableBikeStands, Integer availableBikes, Integer lastUpdate) {
        this.number = number;
        this.name = name;
        this.address = address;
        this.position = position;
        this.banking = banking;
        this.bonus = bonus;
        this.status = status;
        this.contractName = contractName;
        this.bikeStands = bikeStands;
        this.availableBikeStands = availableBikeStands;
        this.availableBikes = availableBikes;
        this.lastUpdate = lastUpdate;
    }

    @SerializedName("number")
    @Expose
    private Integer number;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("address")
    @Expose
    private String address;

    @SerializedName("position")
    @Expose
    private Position position;

    @SerializedName("banking")
    @Expose
    private Boolean banking;

    @SerializedName("bonus")
    @Expose
    private Boolean bonus;

    @SerializedName("status")
    @Expose
    private String status;

    @SerializedName("contract_name")
    @Expose
    private String contractName;

    @SerializedName("bike_stands")
    @Expose
    private Integer bikeStands;

    @SerializedName("available_bike_stands")
    @Expose
    private Integer availableBikeStands;

    @SerializedName("available_bikes")
    @Expose
    private Integer availableBikes;

    @SerializedName("last_update")
    @Expose
    private Integer lastUpdate;

    /**
     *
     * @return
     * The number
     */
    public Integer getNumber() {
        return number;
    }

    /**
     *
     * @param number
     * The number
     */
    public void setNumber(Integer number) {
        this.number = number;
    }

    /**
     *
     * @return
     * The name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     * The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     * The address
     */
    public String getAddress() {
        return address;
    }

    /**
     *
     * @param address
     * The address
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     *
     * @return
     * The position
     */
    public Position getPosition() {
        return position;
    }

    /**
     *
     * @param position
     * The position
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     *
     * @return
     * The banking
     */
    public Boolean getBanking() {
        return banking;
    }

    /**
     *
     * @param banking
     * The banking
     */
    public void setBanking(Boolean banking) {
        this.banking = banking;
    }

    /**
     *
     * @return
     * The bonus
     */
    public Boolean getBonus() {
        return bonus;
    }

    /**
     *
     * @param bonus
     * The bonus
     */
    public void setBonus(Boolean bonus) {
        this.bonus = bonus;
    }

    /**
     *
     * @return
     * The status
     */
    public String getStatus() {
        return status;
    }

    /**
     *
     * @param status
     * The status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     *
     * @return
     * The contractName
     */
    public String getContractName() {
        return contractName;
    }

    /**
     *
     * @param contractName
     * The contract_name
     */
    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    /**
     *
     * @return
     * The bikeStands
     */
    public Integer getBikeStands() {
        return bikeStands;
    }

    /**
     *
     * @param bikeStands
     * The bike_stands
     */
    public void setBikeStands(Integer bikeStands) {
        this.bikeStands = bikeStands;
    }

    /**
     *
     * @return
     * The availableBikeStands
     */
    public Integer getAvailableBikeStands() {
        return availableBikeStands;
    }

    /**
     *
     * @param availableBikeStands
     * The available_bike_stands
     */
    public void setAvailableBikeStands(Integer availableBikeStands) {
        this.availableBikeStands = availableBikeStands;
    }

    /**
     *
     * @return
     * The availableBikes
     */
    public Integer getAvailableBikes() {
        return availableBikes;
    }

    /**
     *
     * @param availableBikes
     * The available_bikes
     */
    public void setAvailableBikes(Integer availableBikes) {
        this.availableBikes = availableBikes;
    }

    /**
     *
     * @return
     * The lastUpdate
     */
    public Integer getLastUpdate() {
        return lastUpdate;
    }

    /**
     *
     * @param lastUpdate
     * The last_update
     */
    public void setLastUpdate(Integer lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

}