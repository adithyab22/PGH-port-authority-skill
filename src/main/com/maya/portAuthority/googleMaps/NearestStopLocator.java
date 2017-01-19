/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.maya.portAuthority.googleMaps;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maya.portAuthority.InvalidInputException;
import com.maya.portAuthority.util.Location;
import com.maya.portAuthority.util.JsonUtils;
import com.maya.portAuthority.util.Stop;
import com.maya.portAuthority.api.TrueTimeAPI;

/**
 */
public class NearestStopLocator {

	private static Logger log = LoggerFactory.getLogger(NearestStopLocator.class);

	private static final String TEXT_SEARCH_URL="https://maps.googleapis.com/maps/api/place/textsearch/json";
	private static final String DISTANCE_MATRIX_URL="https://maps.googleapis.com/maps/api/distancematrix/json";
	private static final String GOOGLE_MAPS_KEY="AIzaSyBzW19DGDOi_20t46SazRquCLw9UNp_C8s";


	public static Stop process(Location source, String routeID, String direction) throws IOException, JSONException, InvalidInputException{
		log.trace("Process: source={}, routeId={}, direction={}", source, routeID, direction);
		if ((source==null)||(routeID==null)||(direction==null)){
			log.error("Bad Input");
			throw new InvalidInputException("Null input: source="+source+",routeID="+routeID+",direction="+direction,"I forgot something");
		}

		//Get list of stops of the route# returned by truetime:
		List<Stop> listOfStops = TrueTimeAPI.getStopsAsJson(routeID, direction);

		//find nearest stop from the source location:
		Stop nearestStop = findNearestStop(source.getLat(), source.getLng(), listOfStops);

		return nearestStop;

	}

	public static Location getSourceLocation(String source)throws IOException, JSONException, InvalidInputException {
		log.trace("getSourceLocation: source={}", source);
		List<Location> sourceLocation = getSourceLocationCoordinates(source, 1);

		//get the first location returned:
		return(sourceLocation.get(0));

	}

	/**
	 * Pads "Pittsburgh" to input and requests the Google location API which returns list of location details.
	 * 
	 * @param source name / Landmark / Any keyword/s to identify source location
	 * @return List of top 5 location details. (1st being the most relevant)
	 * @throws IOException
	 * @throws JSONException 
	 */
	public static List<Location> getSourceLocationCoordinates(String source, int numResults) throws IOException, JSONException, InvalidInputException {
		log.info("getSourceLocationCoordinates: source={}", source);
		JSONObject currentLocationDetails = null;
		List<Location> listOfLocationCoordinates = null;
		String currLocation = (source + " Pittsburgh").replaceAll("\\s", "+");
		String currLocationURL = TEXT_SEARCH_URL+"?query=" + currLocation + "&sensor=false&key="+GOOGLE_MAPS_KEY;
		currentLocationDetails = JsonUtils.readJsonFromUrl(currLocationURL);
		if (currentLocationDetails != null) {
			listOfLocationCoordinates = LocationTracker.getLatLngDetails(currentLocationDetails, numResults);
		}
		return listOfLocationCoordinates;
	}

	/**
	 * @deprecated
	 * Gets list of stops for a route#
	 * @param routeID
	 * @param direction
	 * @return
	 * @throws IOException
	 * @throws JSONException 
	 * 
	 */
	//    public static List<Stop> getStops(String routeID, String direction) throws IOException, JSONException{
	//    	log.info("getStops: routeId={}, direction={}", routeID, direction);
	//    	//TODO: Move to TrueTimeMessageParser
	//    	String url =  "http://truetime.portauthority.org/bustime/api/v2/getstops?key=929FvbAPSEeyexCex5a7aDuus&rt="+routeID+"&dir="+direction.toUpperCase()+"&format=json";
	//       JSONObject stopsJSON = null;
	//       List<Stop> listOfStops = null;
	//       stopsJSON = JsonUtils.readJsonFromUrl(url);
	//       listOfStops = LocationTracker.getStopDetails(stopsJSON);
	//       return listOfStops;
	//    }

	/**
	 * Finds nearest bus stop by determining the shortest distance between source location and 
	 * list of bus stop locations returned by truetime API.
	 * @param sourceLat
	 * @param sourceLon
	 * @param stops
	 * @return
	 * @throws JSONException
	 * @throws IOException 
	 */
	public static Stop findNearestStop(double sourceLat, double sourceLon, List<Stop> stops) throws JSONException, IOException{
		//TODO Improve efficiency of findNearestStop
		//log.info("findNearestStop: sourceLat={}, sourceLon={}, stops={}", sourceLat, sourceLon, stops);
		//    	Stop nearestStop = null;
		//        double shortestDistance = Double.MAX_VALUE;
		//        double distance;
		//        for(Stop s : stops){
		//            distance = calculateDistance(sourceLat, sourceLon, s.getLatitude(), s.getLongitude());
		//            if( distance < shortestDistance){
		//                shortestDistance = distance;
		//                nearestStop = s;
		//            }
		//        }
		//        return nearestStop;
		String destinationString="&destinations="+stops.get(0).getLatitude()+","+stops.get(0).getLongitude();
		for (int i=1; i<stops.size(); i++){
			destinationString+="|"+stops.get(i).getLatitude()+","+stops.get(i).getLongitude();
		}
		String url =  DISTANCE_MATRIX_URL+"?origins="+sourceLat+","+sourceLon+destinationString+"&key="+GOOGLE_MAPS_KEY+"&units=imperial&mode=walk&transit_mode=walking";
		//log.info("DISTANCE_MATRIX_URL:"+url);
		JSONArray distanceJSON = null;
		distanceJSON = JsonUtils.readJsonFromUrl(url)
				.getJSONArray("rows")
				.getJSONObject(0)
				.getJSONArray("elements");

		for (int j=0;j<distanceJSON.length();j++){
			//log.info("JSON Elements at "+j+":"+distanceJSON.getJSONObject(j));
			Stop stop=stops.get(j);
			stop.setDistance(distanceJSON.getJSONObject(j).getJSONObject("distance").getDouble("value"));
			stop.setWalkTime(distanceJSON.getJSONObject(j).getJSONObject("duration").getString("value"));
			log.info("{},{},{}:{},{}:{},{}", stop.getStopName(), stop.getDistance(),sourceLat,sourceLon,stop.getLatitude(), stop.getLongitude());
		}

		Collections.sort(stops);
		//TODO: Make sure there is one stop that's close enough.
		return stops.get(0);


		//        "rows": [ {
		//            "elements": [ {
		//              "status": "OK",
		//              "duration": {
		//                "value": 340110,
		//                "text": "3 jours 22 heures"
		//              },
		//              "distance": {
		//                "value": 1734542,
		//                "text": "1 735 km"
		//              }
		//            }, 
		//        
	}
	/**
	 * @deprecated
	 * Calculates walking distance (in ft) between source and destination locations using Google distancematrix API
	 * @param sourceLat
	 * @param sourceLon
	 * @param destLat
	 * @param destLon
	 * @return 
	 */
//	public static double calculateDistance(double sourceLat, double sourceLon, double destLat, double destLon) throws JSONException, IOException{
//		//log.info("calculateDistance: sourceLat={}, sourceLon={}, destLat={}, destLon={}", sourceLat, sourceLon, destLat,destLon );
//		String url =  DISTANCE_MATRIX_URL+"?origins="+sourceLat+","+sourceLon+"&destinations="+destLat+","+destLon+"&key="+GOOGLE_MAPS_KEY+"&units=imperial&mode=walk&transit_mode=walking";
//		JSONObject distanceJSON = null;
//		String distance = "";
//		distanceJSON = JsonUtils.readJsonFromUrl(url);
//		distance = distanceJSON.getJSONArray("rows").getJSONObject(0).getJSONArray("elements").getJSONObject(0).getJSONObject("distance").getString("text");
//		return convertMileToFeet(distance);
//	}

	/***************************************Util methods***********************************************/

	public static double convertMileToFeet(String distance){
		log.info("convertMileToFeet: distance={}", distance);

		double result = 0.0;
		if(distance.contains("mi")){
			result = Double.parseDouble(distance.substring(0, distance.length() - 2))*5280.0;
		}else if(distance.contains("ft")){
			result = Double.parseDouble(distance.substring(0, distance.length() - 2));
		}else{
			result = 0.0;
		}
		return result;
	}




}