package edu.cmu.hw6byadav;

/**
 * Created by yadav on 6/27/2017.
 */

public class AddressTweet {
    private String streetNumber;
    private String streetName;
    private String cityName;
    private String stateName;
    private String postalCode;
    private static AddressTweet instance = null;
    private AddressTweet(){};

    public static AddressTweet getInstance(){
        if(instance == null){
            instance = new AddressTweet();
        }
        return instance;
    }
//    public void setStreetNumber(String streetNumber){
//        this.streetNumber = streetNumber;
//    }
//    public void setStreetName(String streetName){
//        this.streetName = streetName;
//    }
//    public void setCityName(String cityName){
//        this.cityName = cityName;
//    }
//    public void setStateName()
    public void setAddressToTweet(String streetNumber, String streetName,
                                  String cityName, String stateName, String postalCode){
        this.streetNumber = streetNumber;
        this.streetName = streetName;
        this.cityName = cityName;
        this.stateName = stateName;
        this.postalCode = postalCode;
    }

    public String getAddressToTweet(){
        if(this == null)
            return "";
        return this.streetNumber + " " +
                this.streetName + " " +
                this.cityName + " " +
                this.stateName + " " +
                this.postalCode + " ";
    }
}
