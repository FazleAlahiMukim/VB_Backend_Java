package main.apiController;
import main.entity.*;
import main.service.PlannerService;
import main.business_logic.Planner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class PlannerController {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
//    SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");
    int costPerKM = 20;
    int costPerKMLongDistance = 5;
    int costResidence = 1500;
    List<Double> totalDistanceArrayForEachCluster = new ArrayList<>();

    double budget;
    double totalCost = 0;
    double totalCost2 = 0;
    int days_allowed_Plan1;

    int getNumberOfDaysBetweenDates(String startDate, String endDate) {
        // Parse the date strings to LocalDate
        LocalDate startDate_t = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endDate_t = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);

        // Calculate the difference in days
        return (int) Math.abs(endDate_t.toEpochDay() - startDate_t.toEpochDay()) + 1;
    }

    String dateFormatter(Calendar date) {
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DATE);
        // Format the month to ensure it has two digits
        String formattedMonth = String.format("%02d", month);
        return year + "-" + formattedMonth + "-" + day;
    }

    List<LinkedHashMap<String, Object>> getClusterValueList(Map<String, Object> plan, Calendar startTime, List<Integer> tsp_result, Map<String, Object> Data) {
        double [][] timeMatrix = (double[][]) plan.get("timeMatrix");
        double [][] distanceMatrix = (double[][]) plan.get("distanceMatrix");
        LocalTime lastEndTime = LocalTime.of(10, 0);List<LinkedHashMap<String, Object>> cluster_value_list = new ArrayList<>();
        double totalDistance = 0;

        for (int i = 0; i < tsp_result.size(); i++) {
            //if tsp_result[i] is -1, then its the restaurant.....................................................
            if (tsp_result.get(i) == -1) {
                String name = "Restaurant";
                int id = -1;
                String startTime_t = lastEndTime.format(formatter).toUpperCase();
                int avg_time_spent = 1;
                lastEndTime = lastEndTime.plusHours(avg_time_spent);
                String endTime_t = lastEndTime.format(formatter).toUpperCase();
                //lat will be previous spots lat
                double lat = (double) cluster_value_list.get(cluster_value_list.size() - 1).get("lat");
                //lng will be previous spots lng
                double lng = (double) cluster_value_list.get(cluster_value_list.size() - 1).get("lng");
                double rating = 0;
                String description = "Restaurant";
                String imageURL = (String) cluster_value_list.get(cluster_value_list.size() - 1).get("imageURL");
                String open = "0000";
                String close = "2359";
                LinkedHashMap<String, Object> spot_obj = new LinkedHashMap<>();
                spot_obj.put("name", name);
                spot_obj.put("id", id);
                spot_obj.put("startTime", startTime_t);
                spot_obj.put("endTime", endTime_t);
                spot_obj.put("lat", lat);
                spot_obj.put("lng", lng);
                spot_obj.put("rating", rating);
                spot_obj.put("description", description);
                spot_obj.put("imageURL", imageURL);
                spot_obj.put("average_time_spent", avg_time_spent);
                spot_obj.put("cost", 0);
                spot_obj.put("open", open);
                spot_obj.put("close", close);
                cluster_value_list.add(spot_obj);
                continue;
            }
            //its the restaurant..........................................................................

            var tourist_spots = (List<Map<String, Object>>) Data.get("tourist_spots");
            String name = (String) tourist_spots.get(tsp_result.get(i)).get("place");
            int id = (int) tourist_spots.get(tsp_result.get(i)).get("id");
            String startTime_t = lastEndTime.format(formatter).toUpperCase();

            int avg_time_spent = (int) (double) tourist_spots.get(tsp_result.get(i)).get("average_time_spent");
            lastEndTime = lastEndTime.plusHours(avg_time_spent);
            String endTime_t = lastEndTime.format(formatter).toUpperCase();
            double lat = (double) tourist_spots.get(tsp_result.get(i)).get("latitude");
            double lng = (double) tourist_spots.get(tsp_result.get(i)).get("longitude");
            double rating = (double) tourist_spots.get(tsp_result.get(i)).get("rating");
            String description = (String) tourist_spots.get(tsp_result.get(i)).get("description");
            String imageURL = (String) tourist_spots.get(tsp_result.get(i)).get("image_url");
            int cost = (int) tourist_spots.get(tsp_result.get(i)).get("cost");
            String open = (String) tourist_spots.get(tsp_result.get(i)).get("open");
            String close = (String) tourist_spots.get(tsp_result.get(i)).get("close");

            LinkedHashMap<String, Object> spot_obj = new LinkedHashMap<>();
            spot_obj.put("name", name);
            spot_obj.put("id", id);
            spot_obj.put("startTime", startTime_t);
            spot_obj.put("endTime", endTime_t);
            spot_obj.put("lat", lat);
            spot_obj.put("lng", lng);
            spot_obj.put("rating", rating);
            spot_obj.put("description", description);
            spot_obj.put("imageURL", imageURL);
            spot_obj.put("cost", cost);
            spot_obj.put("average_time_spent", avg_time_spent);
            spot_obj.put("open", open);
            spot_obj.put("close", close);

            cluster_value_list.add(spot_obj);
            //timeMatrix is in milliseconds
            if (i < tsp_result.size() - 1) {
                if (tsp_result.get(i + 1) == -1 && i + 2 < tsp_result.size()) {
                    lastEndTime = lastEndTime.plusSeconds((int) timeMatrix[tsp_result.get(i)][tsp_result.get(i + 2)]/1000);
                    totalDistance += distanceMatrix[tsp_result.get(i)][tsp_result.get(i + 2)];
                }
                else if (!(tsp_result.get(i + 1) == -1)) {
                    lastEndTime = lastEndTime.plusSeconds((int) timeMatrix[tsp_result.get(i)][tsp_result.get(i + 1)]/1000);
                    totalDistance += distanceMatrix[tsp_result.get(i)][tsp_result.get(i + 1)];
                }
            }
        }
        totalDistanceArrayForEachCluster.add(totalDistance);

        return cluster_value_list;
    }

    List<LinkedHashMap<String, Object>> getDaybyDay(Calendar finalStartTime, List<List<LinkedHashMap<String, Object>>> cluster_value_list_list, List<Integer> daysAllocated, List<String> destinations, List<Map<String, Object>> location_distance_time_data, int type) {
        List<LinkedHashMap<String, Object>> daybyday = new ArrayList<>();
        Calendar plan_StartTime = Calendar.getInstance();
        plan_StartTime.set(finalStartTime.get(Calendar.YEAR), finalStartTime.get(Calendar.MONTH), finalStartTime.get(Calendar.DATE), 10, 0);
        for (int i = 0; i < cluster_value_list_list.size(); i++) {
            //calculate the cost from the cluster_value_list
            for (int j = 0; j < cluster_value_list_list.get(i).size(); j++) {
                if (type == 1) totalCost += (int) cluster_value_list_list.get(i).get(j).get("cost");
                else totalCost2 += (int) cluster_value_list_list.get(i).get(j).get("cost");
            }
            if (type == 1) {
                totalCost += totalDistanceArrayForEachCluster.get(i) * costPerKM;
                totalCost += 3 * 200;
            } else {
                totalCost2 += totalDistanceArrayForEachCluster.get(i) * costPerKM;
                totalCost2 += 3 * 200;
            }

            //i need the dat_value as 'yyyy-mm-dd' format
            String date_value = dateFormatter(plan_StartTime);

            //get the current location name from days allocated
            String currentLocationName = null;
            int count = 0;
            for (int j = 0; j < daysAllocated.size(); j++) {
                count += daysAllocated.get(j);
                if (count >= i + 1) {
                    currentLocationName = destinations.get(j);
                    break;
                }
            }

            var cluster_value_list = cluster_value_list_list.get(i);
            Calendar endDate_t = Calendar.getInstance();
            endDate_t.set(plan_StartTime.get(Calendar.YEAR), plan_StartTime.get(Calendar.MONTH), plan_StartTime.get(Calendar.DATE) + 1, 10, 0);
            LinkedHashMap<String, Object> hotel_value = new LinkedHashMap<>();
            hotel_value.put("place", cluster_value_list.get(cluster_value_list.size() - 1).get("name"));
            hotel_value.put("lat", cluster_value_list.get(cluster_value_list.size() - 1).get("lat"));
            hotel_value.put("lng", cluster_value_list.get(cluster_value_list.size() - 1).get("lng"));
            hotel_value.put("startDate", dateFormatter(plan_StartTime));
            hotel_value.put("endDate", dateFormatter(endDate_t));

            LinkedHashMap<String, Object> daybyday_obj = new LinkedHashMap<>();
            daybyday_obj.put("date", date_value);
            daybyday_obj.put("location", currentLocationName);
            daybyday_obj.put("cluster", cluster_value_list);
            daybyday_obj.put("hotel", hotel_value);

            //i have to find out if the next day is on another location or not, if it is another location i have to add a travel card
            String nextDayLocationName = null;
            int count2 = 0;
            for (int j = 0; j < daysAllocated.size(); j++) {
                count2 += daysAllocated.get(j);
                if (count2 >= i + 2) {
                    nextDayLocationName = destinations.get(j);
                    break;
                }
            }

            LinkedHashMap<String, Object> travel = new LinkedHashMap<>();
            if (!(nextDayLocationName == null) && !(nextDayLocationName.equals(currentLocationName))) {
                //find distance and time from location_distance_time_data
                double distance = 0;
                double time = 0;
                for (int j = 0; j < location_distance_time_data.size(); j++) {
                    if (currentLocationName.equals(location_distance_time_data.get(j).get("name1")) && nextDayLocationName.equals(location_distance_time_data.get(j).get("name2"))
                            || currentLocationName.equals(location_distance_time_data.get(j).get("name2")) && nextDayLocationName.equals(location_distance_time_data.get(j).get("name1"))) {
                        distance = (float) location_distance_time_data.get(j).get("distance");
                        time = (float) location_distance_time_data.get(j).get("time");
                        break;
                    }
                }
                travel.put("previousLocation", currentLocationName);
                travel.put("nextLocation", nextDayLocationName);
                travel.put("distance", distance);
                travel.put("time", time);

                daybyday_obj.put("travel", travel);
            }

            //add residence cost for this day if there is no travel and if it is not the last day
            if (daybyday_obj.get("travel") == null) {
                if (i != cluster_value_list_list.size() - 1) {
                    if (type == 1) totalCost += costResidence;
                    else totalCost2 += costResidence;
                }
            } else {
                if (type == 1) totalCost += (double) travel.get("distance") * costPerKMLongDistance;
                else totalCost2 += (double) travel.get("distance") * costPerKMLongDistance;
            }

            daybyday.add(daybyday_obj);

            if (type == 1) {
                if (totalCost >= (budget - 1500)) {
                    days_allowed_Plan1 = i + 1;
                    //if there is travel in this day, then i have to remove the travel
                    if(daybyday_obj.get("travel") != null) {
                        daybyday_obj.remove("travel");
                    }
                    break;
                }
            }

            plan_StartTime.add(Calendar.DATE, 1);
        }
        return daybyday;
    }

    @Autowired
    private PlannerService plannerService;

    @PostMapping("api/planner/initialPlan")
    public ResponseEntity<Object> initialPlan(@RequestBody Map<String, Object> requestBody) {
        List<String> destinations = (List<String>) requestBody.get("destinations");
        String startDate = (String) requestBody.get("startDate");
        String endDate = (String) requestBody.get("endDate");
        String currentCity = "Dhaka";
        List<String> preferences = (List<String>) requestBody.get("preferences");
        budget = (int) requestBody.get("budget");

        if (budget == 0) {
            budget = 10000000;
        }

        totalCost = 0;
        totalCost2 = 0;

        int numberOfDaysFromUser = getNumberOfDaysBetweenDates(startDate, endDate);

        List<Integer> daysRequired = new ArrayList<>();
        List<Map<String, Object>> location_data = plannerService.findAllLocationInfo();

        for (String destination : destinations) {
            // Find the days_required for each location from location_data
            for (Map<String, Object> locationDatum : location_data) {
                if (destination.equals(locationDatum.get("Location_Name"))) {
                    daysRequired.add((int) locationDatum.get("days_required"));
                    break;
                }
            }
        }

        int totalDaysRequired = daysRequired.stream().mapToInt(Integer::intValue).sum();
        List<Integer> daysAllocated = new ArrayList<>();
        int totalDaysAllocated = 0;

        if (totalDaysRequired > numberOfDaysFromUser) {
            for (int i = 0; i < daysRequired.size(); i++) {
                int allocatedDays = (int) Math.round((double) daysRequired.get(i) / totalDaysRequired * numberOfDaysFromUser);
                daysAllocated.add(allocatedDays);
                totalDaysAllocated += allocatedDays;

                if (totalDaysAllocated > numberOfDaysFromUser) {
                    daysAllocated.set(i, daysAllocated.get(i) - 1);
                    totalDaysAllocated -= 1;
                }
            }
        } else {
            daysAllocated.addAll(daysRequired);
            totalDaysAllocated = daysRequired.stream().mapToInt(Integer::intValue).sum();
        }
        //.............................................................................................

        // Extract year, month, and day from start date and end date
        int startYear = Integer.parseInt(startDate.substring(0, 4));
        int startMonth = Integer.parseInt(startDate.substring(5, 7));
        int startDay = Integer.parseInt(startDate.substring(8, 10));

        int endYear = Integer.parseInt(endDate.substring(0, 4));
        int endMonth = Integer.parseInt(endDate.substring(5, 7));
        int endDay = Integer.parseInt(endDate.substring(8, 10));

        // Create Calendar instances for start and end date
        Calendar finalStartTime = Calendar.getInstance();
        finalStartTime.set(startYear, startMonth - 1, startDay, 10, 0);

        Calendar finalEndTime = Calendar.getInstance();
        finalEndTime.set(endYear, endMonth - 1, endDay, 23, 59);

        Calendar startTime = Calendar.getInstance();
        startTime.set(startYear, startMonth - 1, startDay, 10, 0);

        List<List<LinkedHashMap<String, Object>>> cluster_value_list_list = new ArrayList<>();
        List<List<LinkedHashMap<String, Object>>> cluster_value_list_list2 = new ArrayList<>();

        List<Integer> tsp_result = new ArrayList<>();
        List<Integer> tsp_result2 = new ArrayList<>();

        for (int i = 0; i < destinations.size(); i++) {
            System.out.println("Taking distance and time matrix: " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());
            List<Map<String, Object>> distandTimeresult = plannerService.findAllDistAndTime(destinations.get(i));
            System.out.println("Distance and time matrix taken: " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());

            List<Integer> alreadyVisitedSpots = new ArrayList<>();
            List<Integer> alreadyVisitedSpots2 = new ArrayList<>();
            int daysCount = 0;
            while (true) {
                if (daysCount == daysAllocated.get(i)) break;
                if (startTime.compareTo(finalEndTime) > 0) break;

                int weekDay = startTime.get(Calendar.DAY_OF_WEEK) - 1;

                boolean newSpotFound = false;
                System.out.println("Finding Tourist Spots Open on Day: " + weekDay + ". " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());
                List<Map<String, Object>> ts_data =  plannerService.findSpotsOpenOnDay(destinations.get(i), weekDay);
                System.out.println("Tourist Spots found. " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());

                List<Map<String, Object>> tourist_spots = new ArrayList<>();
                List<Map<String, Object>> tourist_spots2 = new ArrayList<>();

                for (int j = 0; j < ts_data.size(); j++) {
                    Map<String, Object> tourist_spot = ts_data.get(j);
                    int tourist_spot_id = (int) tourist_spot.get("Tourist_Spot_ID");
                    String tourist_spot_name = (String) tourist_spot.get("Name");
                    double longitude = (double) tourist_spot.get("Longitude");
                    double latitude = (double) tourist_spot.get("Latitude");
                    double average_time_spent = (double) tourist_spot.get("Average_Time_Spent");
                    double rating = (double) tourist_spot.get("Rating");
                    String description = (String) tourist_spot.get("Description");
                    String image_url = (String) tourist_spot.get("Image_Url");
                    String tags = (String) tourist_spot.get("Tags");
                    int cost = (int) tourist_spot.get("Cost");
                    String open = (String) tourist_spot.get("open");
                    String close = (String) tourist_spot.get("close");

                    Map<String, Object> tourist_spot_obj = new LinkedHashMap<>();
                    tourist_spot_obj.put("id", tourist_spot_id);
                    tourist_spot_obj.put("place", tourist_spot_name);
                    tourist_spot_obj.put("average_time_spent", average_time_spent);
                    tourist_spot_obj.put("longitude", longitude);
                    tourist_spot_obj.put("latitude", latitude);
                    tourist_spot_obj.put("rating", rating);
                    tourist_spot_obj.put("description", description);
                    tourist_spot_obj.put("image_url", image_url);
                    tourist_spot_obj.put("tags", tags);
                    tourist_spot_obj.put("cost", cost);
                    tourist_spot_obj.put("open", open);
                    tourist_spot_obj.put("close", close);


                    if (!alreadyVisitedSpots.contains(tourist_spot_id)) {
                        tourist_spots.add(tourist_spot_obj);
                        newSpotFound = true;
                    }
                    if (!alreadyVisitedSpots2.contains(tourist_spot_id)) {
                        tourist_spots2.add(tourist_spot_obj);
                        newSpotFound = true;
                    }
                }
                if (!newSpotFound) {  //if no new spot is found, then break the while loop and go to next location
                    break;
                }

                Map<String, Object> Data = new HashMap<>();
                Data.put("place", destinations.get(i));
                Data.put("tourist_spots", tourist_spots);

                Map<String, Object> Data2 = new HashMap<>();
                Data2.put("place", destinations.get(i));
                Data2.put("tourist_spots", tourist_spots2);

                Calendar endTime = Calendar.getInstance();
                endTime.set(startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH), startTime.get(Calendar.DATE), 23, 59);

                Map<String, Object> plan = Planner.planTour(Data, startTime, endTime, distandTimeresult, preferences, List.of());
                Map<String, Object> plan2 = Planner.planTour(Data2, startTime, endTime, distandTimeresult, preferences, List.of());
                var arr = (List<List<Integer>>) plan.get("tsp_result");
                tsp_result = arr.get(0);
                var arr2 = (List<List<Integer>>) plan2.get("tsp_result");
                tsp_result2 = arr2.get(0);

                //need to add the spots from tsp_result from alreadyVisitedSpots
                for (int j = 0; j < tsp_result.size(); j++) {
                    if (tsp_result.get(j) == -1) continue;                 //its the restaurant
                    int spot_id = (int) ((List<Map<String, Object>>) Data.get("tourist_spots")).get(tsp_result.get(j)).get("id");
                    alreadyVisitedSpots.add(spot_id);
                }

                //need to add the spots from tsp_result2 from alreadyVisitedSpots2
                for (int j = 0; j < tsp_result2.size(); j++) {
                    if (tsp_result2.get(j) == -1) continue;                 //its the restaurant
                    int spot_id = (int) ((List<Map<String, Object>>) Data2.get("tourist_spots")).get(tsp_result2.get(j)).get("id");
                    alreadyVisitedSpots2.add(spot_id);
                }

                //get cluster_value_list
                var cluster_value_list = getClusterValueList(plan, startTime, tsp_result, Data);
                var cluster_value_list2 = getClusterValueList(plan2, startTime, tsp_result2, Data2);

                cluster_value_list_list.add(cluster_value_list);
                cluster_value_list_list2.add(cluster_value_list2);

                //advance startTime by 1 day
                startTime.add(Calendar.DATE, 1);
                daysCount += 1;
            };
        }
        //next database info is needed in travel in daybyday
        //i need to get info about the distance and time between locations from the database
        List<Map<String, Object>> location_distance_time_data = plannerService.findAllDistAndTimeLocation();

        //first calculate the cost to travel to the first allocated city from current city
        { //first find the index for which the first destination has days allocated
            int index = 0;
            for (int i = 0; i < daysAllocated.size(); i++) {
                if (daysAllocated.get(i) != 0) {
                    index = i;
                    break;
                }
            }
            double distance = 0;
            double time = 0;
            for (int j = 0; j < location_distance_time_data.size(); j++) {
                if(currentCity.equals(location_distance_time_data.get(j).get("name1")) && destinations.get(index).equals(location_distance_time_data.get(j).get("name2"))
                        || currentCity.equals(location_distance_time_data.get(j).get("name2")) && destinations.get(index).equals(location_distance_time_data.get(j).get("name1"))) {
                    distance = (double) location_distance_time_data.get(j).get("distance");
                    time = (double) location_distance_time_data.get(j).get("time");
                    break;
                }
            }
            totalCost += distance * costPerKMLongDistance;
            totalCost2 += distance * costPerKMLongDistance;
        }


        var daybyday = getDaybyDay(finalStartTime, cluster_value_list_list, daysAllocated, destinations, location_distance_time_data, 1);
        if (days_allowed_Plan1 == 0) days_allowed_Plan1 = daybyday.size();
        var daybyday2 = getDaybyDay(finalStartTime, cluster_value_list_list2, daysAllocated, destinations, location_distance_time_data, 2);

        for(int i = 0; i < daybyday.size(); i++) {
            var cluster = (List<LinkedHashMap<String, Object>>) daybyday.get(i).get("cluster");
            System.out.println("Day " + (i + 1) + " :");
            for (int j = 0; j < cluster.size(); j++) {
                System.out.println(cluster.get(j).get("name") + " " + cluster.get(j).get("startTime") + " " + cluster.get(j).get("endTime"));
            }
        }

        for(int i = 0; i < daybyday2.size(); i++) {
            var cluster = (List<LinkedHashMap<String, Object>>) daybyday2.get(i).get("cluster");
            System.out.println("Day " + (i + 1) + " :");
            for (int j = 0; j < cluster.size(); j++) {
                System.out.println(cluster.get(j).get("name") + " " + cluster.get(j).get("startTime") + " " + cluster.get(j).get("endTime"));
            }
        }

        //create the plan name for plan 1.............................................
        int daysCountForPlan1 = 0;
        int lastDestinationIndex = 0;
        StringBuilder planName = new StringBuilder(days_allowed_Plan1 + " days in ");
        for (int i = 0; i < destinations.size(); i++) {
            if (daysAllocated.get(i) == 0)
                continue;
            else {
                planName.append(destinations.get(i)).append(", ");
                daysCountForPlan1 += daysAllocated.get(i);
                if (daysCountForPlan1 >= days_allowed_Plan1) {
                    lastDestinationIndex = i;
                    break;
                }
            }
        }
        planName = new StringBuilder(planName.substring(0, planName.length() - 2));
        //plan name created successfully....................................

        //create the plan name for plan 2.............................................
        StringBuilder planName2 = new StringBuilder(totalDaysAllocated + " days in ");
        for (int i = 0; i < destinations.size(); i++) {
            if (daysAllocated.get(i) == 0)
                continue;
            else {
                planName2.append(destinations.get(i)).append(", ");
            }
        }
        planName2 = new StringBuilder(planName2.substring(0, planName2.toString().length() - 2));
        //plan name created successfully....................................

        //i have to return days_allocated with name of destination, lat, lng and final_plan_list.................

        //location_distance_time_data is the data from location_distance_time table, it has been fetched earlier in the code
        List<Map<String, Double>> location_distance_time = new ArrayList<>();
        boolean currentCityFound = false;
        for (int i = 0; i < destinations.size(); i++) {
            if (i == 0 && currentCity.equalsIgnoreCase(destinations.get(i)))  {
                currentCityFound = true;
            }
            double distance = -1;
            double time = -1;
            for (int j = 0; j < location_distance_time_data.size(); j++) {
                if (i == 0) {
                    if (currentCity.equalsIgnoreCase(location_distance_time_data.get(j).get("name1").toString()) && destinations.get(i).equalsIgnoreCase(location_distance_time_data.get(j).get("name2").toString())
                            || currentCity.equalsIgnoreCase(location_distance_time_data.get(j).get("name2").toString()) && destinations.get(i).equalsIgnoreCase(location_distance_time_data.get(j).get("name1").toString())) {
                        distance = (double) location_distance_time_data.get(j).get("distance");
                        time = (double) location_distance_time_data.get(j).get("time");
                        currentCityFound = true;
                        break;
                    }
                } else if (destinations.get(i).equalsIgnoreCase(location_distance_time_data.get(j).get("name1").toString()) && destinations.get(i - 1).equalsIgnoreCase(location_distance_time_data.get(j).get("name2").toString())
                        || destinations.get(i).equalsIgnoreCase(location_distance_time_data.get(j).get("name2").toString()) && destinations.get(i - 1).equalsIgnoreCase(location_distance_time_data.get(j).get("name1").toString())) {
                    distance = (float) location_distance_time_data.get(j).get("distance");
                    time = (float) location_distance_time_data.get(j).get("time");
                    break;
                }
            }
            if (!currentCityFound) {
                System.out.println("Current City can not be found. Default Current City is Dhaka.");
                currentCity = "Dhaka";
                i--;
                continue;
            }
            if (!(distance == -1)) {
                location_distance_time.add(Map.of("distance", distance, "time", time));
            } else {
                location_distance_time.add(Map.of("distance", 0.0, "time", 0.0));
            }
        }

        //create destination_value for plan 1
        List<LinkedHashMap<String, Object>> destination_value = new ArrayList<>();
        for (int i = 0; i < destinations.size(); i++) {

            //get latitude and longitude from location_data, location_data has been called earlier in the code
            double lat = 0;
            double lng = 0;
            for (int j = 0; j < location_data.size(); j++) {
                if (destinations.get(i).equals(location_data.get(j).get("Location_Name"))) {
                    lat = (double) location_data.get(j).get("Latitude");
                    lng = (double) location_data.get(j).get("Longitude");
                    break;
                }
            }

            if (daysAllocated.get(i) == 0)
                continue;
            else {
                String name = destinations.get(i);
                int days;
                if (i == lastDestinationIndex) days = daysAllocated.get(i) - (daysCountForPlan1 - days_allowed_Plan1);
                else days = daysAllocated.get(i);
                LinkedHashMap<String, Object> obj = new LinkedHashMap<>();
                obj.put("name", name);
                obj.put("days", days);
                obj.put("lat", lat);
                obj.put("lng", lng);
                obj.put("distance", location_distance_time.get(i).get("distance"));
                obj.put("time", location_distance_time.get(i).get("time"));
                destination_value.add(obj);
            }
            //if it is the last destination, then i have to add a endCity object, which will go from last destination to current city
            if (i == lastDestinationIndex) {
                double distance = -1;
                double time = -1;
                for (int j = 0; j < location_distance_time_data.size(); j++) {
                    if (destinations.get(i).equals(location_distance_time_data.get(j).get("name1")) && currentCity.equals(location_distance_time_data.get(j).get("name2"))
                            || destinations.get(i).equals(location_distance_time_data.get(j).get("name2")) && currentCity.equals(location_distance_time_data.get(j).get("name1"))) {
                        distance = (float) location_distance_time_data.get(j).get("distance");
                        time = (float) location_distance_time_data.get(j).get("time");
                        break;
                    }
                }
                if (distance == -1) {
                    distance = 0;
                    time = 0;
                }
                LinkedHashMap<String, Object> obj = new LinkedHashMap<>();
                obj.put("name", currentCity);
                obj.put("days", 0);
                obj.put("lat", 0);
                obj.put("lng", 0);
                obj.put("distance", distance);
                obj.put("time", time);
                destination_value.add(obj);
                break;
            }
            //......................................end city added.....................................................
        }
        //destination_value created for plan 1..........................................................................

        //create destination_value for plan 2
        List<LinkedHashMap<String, Object>> destination_value2 = new ArrayList<>();
        for (int i = 0; i < destinations.size(); i++) {

            //get latitude and longitude from location_data, location_data has been called earlier in the code
            double lat = 0;
            double lng = 0;
            for (int j = 0; j < location_data.size(); j++) {
                if (destinations.get(i).equals(location_data.get(j).get("Location_Name"))) {
                    lat = (double) location_data.get(j).get("Latitude");
                    lng = (double) location_data.get(j).get("Longitude");
                    break;
                }
            }

            if (daysAllocated.get(i) == 0)
                continue;
            else {
                String name = destinations.get(i);
                int days = daysAllocated.get(i);
                LinkedHashMap<String, Object> obj = new LinkedHashMap<>();
                obj.put("name", name);
                obj.put("days", days);
                obj.put("lat", lat);
                obj.put("lng", lng);
                obj.put("distance", location_distance_time.get(i).get("distance"));
                obj.put("time", location_distance_time.get(i).get("time"));
                destination_value2.add(obj);
            }
            //if it is the last destination, then i have to add a endCity object, which will go from last destination to current city
            if (i == destinations.size() - 1) {
                double distance = -1;
                double time = -1;
                for (int j = 0; j < location_distance_time_data.size(); j++) {
                    if (destinations.get(i).equals(location_distance_time_data.get(j).get("name1")) && currentCity.equals(location_distance_time_data.get(j).get("name2"))
                            || destinations.get(i).equals(location_distance_time_data.get(j).get("name2")) && currentCity.equals(location_distance_time_data.get(j).get("name1"))) {
                        distance = (float) location_distance_time_data.get(j).get("distance");
                        time = (float) location_distance_time_data.get(j).get("time");
                        break;
                    }
                }
                if (distance == -1) {
                    distance = 0;
                    time = 0;
                }
                LinkedHashMap<String, Object> obj = new LinkedHashMap<>();
                obj.put("name", currentCity);
                obj.put("days", 0);
                obj.put("lat", 0);
                obj.put("lng", 0);
                obj.put("distance", distance);
                obj.put("time", time);
                destination_value2.add(obj);
            }
            //......................................end city added.....................................................
        }
        //destination_value created for plan 2..........................................................................

        String type = "private";
        LinkedHashMap<String, Object> final_return_obj = new LinkedHashMap<>();
        LinkedHashMap<String, Object> final_return_obj2 = new LinkedHashMap<>();
        final_return_obj.put("planID", -1);
        final_return_obj.put("type", type);
        final_return_obj.put("totalCost", totalCost);
        final_return_obj.put("planName", planName);
        final_return_obj.put("currentCity", currentCity);
        final_return_obj.put("destinations", destination_value);
        final_return_obj.put("daybyday", daybyday);

        final_return_obj2.put("planID", -2);
        final_return_obj2.put("type", type);
        final_return_obj2.put("totalCost", totalCost2);
        final_return_obj2.put("planName", planName2);
        final_return_obj2.put("currentCity", currentCity);
        final_return_obj2.put("destinations", destination_value2);
        final_return_obj2.put("daybyday", daybyday2);

        if(totalCost2 <= budget) final_return_obj = final_return_obj2;

        totalCost = 0;
        totalCost2 = 0;
        days_allowed_Plan1 = 0;
        return ResponseEntity.status(HttpStatus.OK).body(List.of(final_return_obj, final_return_obj2));
    }

    @PostMapping("api/planner/update")
    public ResponseEntity<Object> update(@RequestBody Map<String, Object> requestBody) throws ParseException {
        System.out.println("Update called");
        LinkedHashMap<String, Object> initialPlan = (LinkedHashMap<String, Object>) requestBody.get("plan");
        String date = (String) requestBody.get("date");
        List<Integer> tourist_spot_add = (List<Integer>) requestBody.get("tourist_spot_add");
        List<Integer> tourist_spot_remove = (List<Integer>) requestBody.get("tourist_spot_remove");

        //now i have to search the plan for the specific date and update tourist_spots only on that date
        List<LinkedHashMap<String, Object>> daybyday = (List<LinkedHashMap<String, Object>>) initialPlan.get("daybyday");
        int daybyday_index = 0;
        for (int i = 0; i < daybyday.size(); i++) {
            if (daybyday.get(i).get("date").equals(date)) {
                daybyday_index = i;
                break;
            }
        }

        //first create the Data to send to planTour
        Map<String, Object> Data = new HashMap<>();
        Data.put("place", daybyday.get(daybyday_index).get("location"));

        //now i have to take tourist spots from the cluster of that day, update it with add and remove and call planTour to get new spots for that day
        List<Map<String, Object>> tourist_spots = new ArrayList<>();
        List<LinkedHashMap<String, Object>> cluster = (List<LinkedHashMap<String, Object>>) daybyday.get(daybyday_index).get("cluster");
        for (int i = 0; i < cluster.size(); i++) {
            LinkedHashMap<String, Object> spot_obj = new LinkedHashMap<>();
            spot_obj.put("id", cluster.get(i).get("id"));
            if (tourist_spot_remove.contains(cluster.get(i).get("id")) || cluster.get(i).get("id").equals(-1))
                continue;
            spot_obj.put("place", cluster.get(i).get("name"));
            double average_time_spent = (double) (int) cluster.get(i).get("average_time_spent");
            spot_obj.put("average_time_spent", average_time_spent);
            spot_obj.put("longitude", cluster.get(i).get("lng"));
            spot_obj.put("latitude", cluster.get(i).get("lat"));
            spot_obj.put("rating", cluster.get(i).get("rating"));
            spot_obj.put("description", cluster.get(i).get("description"));
            spot_obj.put("image_url", cluster.get(i).get("imageURL"));
            spot_obj.put("cost", cluster.get(i).get("cost"));
            spot_obj.put("open", cluster.get(i).get("open"));
            spot_obj.put("close", cluster.get(i).get("close"));
            tourist_spots.add(spot_obj);
        }

        //i have to get information of tourist_spot_add from database
        //date = 'yyyy-mm-dd', create a date object and get the day of the week
        Date date_temp = (new SimpleDateFormat("yyyy-MM-dd").parse(date));
        int day = date_temp.getDay();

        List<Map<String, Object>> tourist_spots_result = plannerService.findAllSpotsWithTime(Data.get("place").toString(), day);
        for (int i = 0; i < tourist_spot_add.size(); i++) {
            int index = 0;
            for (int j = 0; j < tourist_spots_result.size(); j++) {
                if (tourist_spot_add.get(i) == (int) tourist_spots_result.get(j).get("Tourist_Spot_ID")) {
                    index = j;
                    break;
                }
            }
            LinkedHashMap<String, Object> spot_obj = new LinkedHashMap<>();
            spot_obj.put("id", tourist_spots_result.get(index).get("Tourist_Spot_ID"));
            spot_obj.put("place", tourist_spots_result.get(index).get("Name"));
            double average_time_spent = (double) tourist_spots_result.get(index).get("Average_Time_Spent");
            spot_obj.put("average_time_spent", average_time_spent);
            spot_obj.put("longitude", tourist_spots_result.get(index).get("Longitude"));
            spot_obj.put("latitude", tourist_spots_result.get(index).get("Latitude"));
            spot_obj.put("rating", tourist_spots_result.get(index).get("Rating"));
            spot_obj.put("description", tourist_spots_result.get(index).get("Description"));
            spot_obj.put("image_url", tourist_spots_result.get(index).get("Image_Url"));
            spot_obj.put("cost", tourist_spots_result.get(index).get("Cost"));
            spot_obj.put("open", tourist_spots_result.get(index).get("open"));
            spot_obj.put("close", tourist_spots_result.get(index).get("close"));
            tourist_spots.add(spot_obj);
        }
        Data.put("tourist_spots", tourist_spots);

        //crete startTime and endTime as a calendar instance to send to planTour
        int startYear = Integer.parseInt(date.substring(0, 4));
        int startMonth = Integer.parseInt(date.substring(5, 7));
        int startDay = Integer.parseInt(date.substring(8, 10));
        Calendar startTime = Calendar.getInstance();
        startTime.set(startYear, startMonth - 1, startDay, 10, 0);
        Calendar endTime = Calendar.getInstance();
        endTime.set(startYear, startMonth - 1, startDay, 23, 59);
//        Date startTime = new Date(date);
//        startTime.setHours(10);
//        startTime.setMinutes(0);
//        Date endTime = new Date(date);
//        endTime.setHours(23);
//        endTime.setMinutes(59);

        System.out.println("Taking distance and time matrix: " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());
        List<Map<String, Object>> distandTimeresult = plannerService.findAllDistAndTime(Data.get("place").toString());
        System.out.println("Data fetch done: " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());

        //now call planTour to get new spots for that day
        Map<String, Object> plan = Planner.planTour(Data, startTime, endTime, distandTimeresult, new ArrayList<>(), tourist_spot_add);
        List<List<Integer>> tsp_result_list = (List<List<Integer>>) plan.get("tsp_result");
        List<Integer> tsp_result = tsp_result_list.get(0);

        //get cluster_value_list
        var cluster_value_list = getClusterValueList(plan, startTime, tsp_result, Data);

        //now i have to update the cluster of that day in daybyday
        daybyday.get(daybyday_index).put("cluster", cluster_value_list);

        //now i have to update the hotel of that day in daybyday
        Map<String, Object> hotel = (Map<String, Object>) daybyday.get(daybyday_index).get("hotel");
        hotel.put("place", cluster_value_list.get(cluster_value_list.size() - 1).get("name"));
        hotel.put("lat", cluster_value_list.get(cluster_value_list.size() - 1).get("lat"));
        hotel.put("lng", cluster_value_list.get(cluster_value_list.size() - 1).get("lng"));
        daybyday.get(daybyday_index).put("hotel", hotel);

        //now i have to update the daybyday in initialPlan
        initialPlan.put("daybyday", daybyday);

        return ResponseEntity.status(HttpStatus.OK).body(initialPlan);
    }

    @PostMapping("api/planner/getsuggestion")
    public ResponseEntity<Object> getSuggestion(@RequestBody LinkedHashMap<String, Object> requestBody) {
        Map<String, Object> initialPlan = (Map<String, Object>) requestBody.get("plan");
        String date = (String) requestBody.get("date");
        List<Map<String, Object>> dayByDay = (List<Map<String, Object>>) initialPlan.get("daybyday");

        int dayByDayIndex = -1;
        for (int i = 0; i < dayByDay.size(); i++) {
            if (dayByDay.get(i).get("date").equals(date)) {
                dayByDayIndex = i;
                break;
            }
        }

        String place = null;
        int count = 0;
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) initialPlan.get("destinations");
        for (int i = 0; i < destinations.size(); i++) {
            count += (int) destinations.get(i).get("days");
            if (count >= dayByDayIndex + 1) {
                place = (String) destinations.get(i).get("name");
                break;
            }
        }

        //now i have to search the plan and get all the tourist spot that have been added
        List<Integer> tourist_spots_included = new ArrayList<>();
        for (int i = 0; i < dayByDay.size(); i++) {
            List<Map<String, Object>> cluster = (List<Map<String, Object>>) dayByDay.get(i).get("cluster");
            for (int j = 0; j < cluster.size(); j++) {
                if ((int) cluster.get(j).get("id") != -1) {
                    tourist_spots_included.add((int) cluster.get(j).get("id"));
                }
            }
        }

        //now i have to get all the tourist spots of "place" from the database that are not included in the plan
        //get the day in integer value: sunday = 0, monday = 1, tuesday = 2, wednesday = 3, thursday = 4, friday = 5, saturday = 6
        //date = 'yyyy-mm-dd'
        int day = LocalDateTime.parse(date + "T00:00:00").getDayOfWeek().getValue() % 7;
        System.out.println("Day: " + day);
        System.out.println("Getting suggestions from database: " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());
        List<Map<String, Object>> touristSpotsResult = plannerService.findAllSpotsWithTime(place, day);
        System.out.println("Suggestions fetched from database: " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());

        List<Map<String, Object>> touristSpots = new ArrayList<>();
        for (int i = 0; i < touristSpotsResult.size(); i++) {
            if (!tourist_spots_included.contains(touristSpotsResult.get(i).get("Tourist_Spot_ID"))) {
                Map<String, Object> obj = new LinkedHashMap<>();
                obj.put("id", touristSpotsResult.get(i).get("Tourist_Spot_ID"));
                obj.put("place", touristSpotsResult.get(i).get("Name"));
                obj.put("average_time_spent", touristSpotsResult.get(i).get("Average_Time_Spent"));
                obj.put("longitude", touristSpotsResult.get(i).get("Longitude"));
                obj.put("latitude", touristSpotsResult.get(i).get("Latitude"));
                obj.put("rating", touristSpotsResult.get(i).get("Rating"));
                obj.put("description", touristSpotsResult.get(i).get("Description"));
                obj.put("image_url", touristSpotsResult.get(i).get("Image_Url"));
                obj.put("open", touristSpotsResult.get(i).get("open"));
                obj.put("close", touristSpotsResult.get(i).get("close"));
                touristSpots.add(obj);
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(touristSpots);
    }

    @PostMapping("/api/planner/save")
    public ResponseEntity<LinkedHashMap<String, Object>> savePlan(@RequestBody LinkedHashMap<String, Object> request) {
        System.out.println("Save called");

        String email = (String) request.get("email");
        LinkedHashMap<String, Object> final_return_obj = (LinkedHashMap<String, Object>) request.get("plan");
        int previous_plan_ID = (int) final_return_obj.get("planID");
        String type = (String) final_return_obj.get("type");
        int totalCost = (int) final_return_obj.get("totalCost");
        String planName = (String) final_return_obj.get("planName");
        String currentCity = (String) final_return_obj.get("currentCity");
        List<LinkedHashMap<String, Object>> destination_value = (List<LinkedHashMap<String, Object>>) final_return_obj.get("destinations");
        List<LinkedHashMap<String, Object>> daybyday = (List<LinkedHashMap<String, Object>>) final_return_obj.get("daybyday");

        //if previous_plan_ID is -1 or -2, then it is a new plan, else it is an update
        if (previous_plan_ID != -1 && previous_plan_ID != -2) {
            //delete the previous plan
            System.out.println("Deleting previous plan");
            plannerService.deletePlan(previous_plan_ID);
        }
        //store the plan in the database ........................................................
        //first get the plan id, it is auto incremented in the database
        int plan_id = plannerService.getPlanId(email, planName, currentCity, type, totalCost);
        for (int i = 0; i < destination_value.size(); i++) {
            //now store plan_id, destination_order, name, days in the plan_destination table
            int destination_order = i;
            String name = (String) destination_value.get(i).get("name");
            int days = (int) destination_value.get(i).get("days");
            double lat = (double) destination_value.get(i).get("lat");
            double lng = (double) destination_value.get(i).get("lng");
            double distance = (double) destination_value.get(i).get("distance");
            double time = (double) destination_value.get(i).get("time");
            plannerService.storePlanDestination(plan_id, destination_order, name, days, lat, lng, distance, time);
        }
        System.out.println("Destination Stored");

        for (int i = 0; i < daybyday.size(); i++) {
            //now store plan_id, day_order, date in the day_by_day table
            int day_order = i;
            String date = (String) daybyday.get(i).get("date");
            String location = (String) daybyday.get(i).get("location");
            plannerService.storeDayByDay(plan_id, day_order, date, location);
            List<LinkedHashMap<String, Object>> cluster = (List<LinkedHashMap<String, Object>>) daybyday.get(i).get("cluster");
            for (int j = 0; j < cluster.size(); j++) {
                //now store plan_id, day_order, cluster_order, name, id, start_time, end_time, lat, lng, rating, description, image_url, average_time_spent, open, close in the cluster table
                int cluster_order = j;
                String name = (String) cluster.get(j).get("name");
                int id = (int) cluster.get(j).get("id");
                String start_time = (String) cluster.get(j).get("startTime");
                String end_time = (String) cluster.get(j).get("endTime");
                double lat = (double) cluster.get(j).get("lat");
                double lng = (double) cluster.get(j).get("lng");
                double rating = (double) cluster.get(j).get("rating");
                String description = (String) cluster.get(j).get("description");
                String image_url = (String) cluster.get(j).get("imageURL");
                int average_time_spent = (int) cluster.get(j).get("average_time_spent");
                int cost = (int) cluster.get(j).get("cost");
                String open = (String) cluster.get(j).get("open");
                String close = (String) cluster.get(j).get("close");
                plannerService.storeCluster(plan_id, day_order, cluster_order, name, id, start_time, end_time, lat, lng, rating, description, image_url, average_time_spent, open, close, cost);
            }
            //now store plan_id, day_order, place, lat, lng, startDate, endDate in the hotel table
            LinkedHashMap<String, Object> hotel = (LinkedHashMap<String, Object>) daybyday.get(i).get("hotel");
            String place = (String) hotel.get("place");
            double lat = (double) hotel.get("lat");
            double lng = (double) hotel.get("lng");
            String startDate = (String) hotel.get("startDate");
            String endDate = (String) hotel.get("endDate");
            plannerService.storeHotel(plan_id, day_order, place, lat, lng, startDate, endDate);

            //if there is a travel card, then store plan_id, day_order, previous_location, next_location, distance, time in the travel table
            if (!(daybyday.get(i).get("travel") == null)) {
                LinkedHashMap<String, Object> travel = (LinkedHashMap<String, Object>) daybyday.get(i).get("travel");
                String previous_location = (String) travel.get("previousLocation");
                String next_location = (String) travel.get("nextLocation");
                double distance = (double) travel.get("distance");
                double time = (double) travel.get("time");
                String date_travel = (String) travel.get("date");
                plannerService.storeTravel(plan_id, day_order, previous_location, next_location, distance, time, date_travel);
            }
            System.out.println("Day " + i + " stored");
        }
        //Plan stored in database successfully.......................................................................................
        System.out.println("Plan stored in database successfully");

        //now get the plan from database and return it, this is needed to get the updated checklist
        System.out.println("Getting plan from database: " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());
        LinkedHashMap<String, Object> return_obj = (LinkedHashMap<String, Object>) getPlan(plan_id).getBody();
        System.out.println("Plan fetched from database: " + LocalDateTime.now().getMinute() +":" + LocalDateTime.now().getSecond());

        return ResponseEntity.status(HttpStatus.OK).body(return_obj);
    }

    @GetMapping("/api/planner/delete")
    public ResponseEntity<Object> delete(@RequestParam(value = "id") int id) {
        System.out.println("delete is called for id: " + id);
        plannerService.deletePlan(id);
        return ResponseEntity.status(HttpStatus.OK).body("Plan successfully deleted from database");
    }

    @GetMapping("/api/planner/getplans")
    public List<AllPlans> getPlans(@RequestParam(value = "email") String email) {
        System.out.println("getPlans is called for email: " + email);
        return plannerService.findAllPlans(email);
    }

    @GetMapping("/api/planner/getplan")
    public ResponseEntity<Object> getPlan(@RequestParam(value = "id") int id) {
        System.out.println("getPlan is called for id: " + id);

        // First update the checklist of this plan id
        plannerService.checklist(id);

        Map<String, Object> final_return_obj = new LinkedHashMap<>();

        // Get the plan from the database with this id
        List<Map<String, Object>> plan = plannerService.findPlan(id); //this will return rows from plan table joining on plan_destination table

        // Prepare the "final_return_obj" with plan details
        final_return_obj.put("planID", plan.get(0).get("plan_id"));
        final_return_obj.put("email", plan.get(0).get("email"));
        final_return_obj.put("type", plan.get(0).get("type"));
        final_return_obj.put("totalCost", plan.get(0).get("totalcost"));
        final_return_obj.put("planName", plan.get(0).get("planname"));
        final_return_obj.put("currentCity", plan.get(0).get("currentcity"));

        List<Map<String, Object>> destinations = new ArrayList<>();
        for (Map<String, Object> stringObjectMap : plan) {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("name", stringObjectMap.get("name"));
            obj.put("days", stringObjectMap.get("days"));
            obj.put("lat", stringObjectMap.get("lat"));
            obj.put("lng", stringObjectMap.get("lng"));
            obj.put("distance", stringObjectMap.get("distance"));
            obj.put("time", stringObjectMap.get("time"));
            destinations.add(obj);
        }
        final_return_obj.put("destinations", destinations);

        //now get the rows from day_by_day table joining on cluster table joining on hotel table
        List<Map<String, Object>> dayByDayResult = plannerService.findDayByDay(id);

        List<Map<String, Object>> daybyday = new ArrayList<>();
        for (int i = 0; i < dayByDayResult.size(); i++) {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("date", dayByDayResult.get(i).get("date"));
            obj.put("location", dayByDayResult.get(i).get("location"));
            List<Map<String, Object>> cluster = new ArrayList<>();
            {
                int count = 0;
                for (int j = i; j < dayByDayResult.size(); j++) {
                    if (dayByDayResult.get(i).get("date").equals(dayByDayResult.get(j).get("date"))) {
                        count++;
                    } else {
                        break;
                    }
                }

                for (int j = 0; j < count; j++, i++) {
                    Map<String, Object> obj2 = new LinkedHashMap<>();
                    //get name, id, startTime, endTime, lat, lng, rating, description, image_url, average_time_spent, open, close, checklist
                    obj2.put("name", dayByDayResult.get(i).get("name"));
                    obj2.put("id", dayByDayResult.get(i).get("id"));
                    obj2.put("startTime", dayByDayResult.get(i).get("starttime"));
                    obj2.put("endTime", dayByDayResult.get(i).get("endtime"));
                    obj2.put("lat", dayByDayResult.get(i).get("lat"));
                    obj2.put("lng", dayByDayResult.get(i).get("lng"));
                    obj2.put("rating", dayByDayResult.get(i).get("rating"));
                    obj2.put("description", dayByDayResult.get(i).get("description"));
                    obj2.put("imageURL", dayByDayResult.get(i).get("image_url"));
                    obj2.put("average_time_spent", dayByDayResult.get(i).get("average_time_spent"));
                    obj2.put("cost", dayByDayResult.get(i).get("cost"));
                    obj2.put("open", dayByDayResult.get(i).get("open"));
                    obj2.put("close", dayByDayResult.get(i).get("close"));
                    obj2.put("checklist", dayByDayResult.get(i).get("checklist"));
                    cluster.add(obj2);
                }
                i--;
                obj.put("cluster", cluster);
            }
            Map<String, Object> hotel = new LinkedHashMap<>();
            hotel.put("place", dayByDayResult.get(i).get("hotel_place"));
            hotel.put("lat", dayByDayResult.get(i).get("hotel_lat"));
            hotel.put("lng", dayByDayResult.get(i).get("hotel_lng"));
            hotel.put("startDate", dayByDayResult.get(i).get("startdate"));
            hotel.put("endDate", dayByDayResult.get(i).get("enddate"));
            obj.put("hotel", hotel);

            //now get rows from travel table
            List<Map<String, Object>> travel = plannerService.findTravel(id, obj.get("date").toString());
            if (travel.size() != 0) {
                Map<String, Object> travelObj = new LinkedHashMap<>();
                //get previous_location, next_location, distance, time, date
                travelObj.put("previousLocation", travel.get(0).get("previous_location"));
                travelObj.put("nextLocation", travel.get(0).get("next_location"));
                travelObj.put("distance", travel.get(0).get("distance"));
                travelObj.put("time", travel.get(0).get("time"));
                travelObj.put("date", travel.get(0).get("date"));
                obj.put("travel", travelObj);
            }
            daybyday.add(obj);
        }
        final_return_obj.put("daybyday", daybyday);
        System.out.println("Plan successfully fetched from database");
        return ResponseEntity.status(HttpStatus.OK).body(final_return_obj);
    }

    @GetMapping("/api/planner/findtours")
    public ResponseEntity<Object> findTours() {
        System.out.println("findTours is called");
        List<Map<String, Object>> plans = plannerService.findAllPublicPlans();
        return ResponseEntity.status(HttpStatus.OK).body(plans);
    }

    @GetMapping("/api/planner/changetype")
    public ResponseEntity<Object> changeType(@RequestParam(value = "id") int id) {
        System.out.println("changeType is called for id: " + id);
        plannerService.changeType(id);
        return ResponseEntity.status(HttpStatus.OK).body("Type successfully changed");
    }

}
