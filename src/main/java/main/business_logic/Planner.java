package main.business_logic;
import main.service.PlannerService;

import java.time.LocalTime;
import java.util.*;

public class Planner {
    public static Date parseTimeString(String timeString, Date startTime) {
        int hours = Integer.parseInt(timeString.substring(0, 2));
        int minutes = Integer.parseInt(timeString.substring(2, 4));
        return new Date(startTime.getYear(), startTime.getMonth(), startTime.getDate(), hours, minutes);
    }

    public static List<List<Integer>> tsp_algorithm(double[][] distanceMatrix, double[][] timeMatrix,
                                              List<Map<String, Long>> openingHours, List<Integer> averageTimeSpent,
                                              List<List<String>> tags, Calendar startTime_c, Calendar endTime_c,
                                              List<String> preferences, List<Integer> mustVisit) {
        //convert startTime_c to Date object
        Date startTime = new Date(startTime_c.get(Calendar.YEAR), startTime_c.get(Calendar.MONTH), startTime_c.get(Calendar.DATE), startTime_c.get(Calendar.HOUR_OF_DAY), startTime_c.get(Calendar.MINUTE));
        Date endTime = new Date(endTime_c.get(Calendar.YEAR), endTime_c.get(Calendar.MONTH), endTime_c.get(Calendar.DATE), endTime_c.get(Calendar.HOUR_OF_DAY), endTime_c.get(Calendar.MINUTE));
        int numNodes = distanceMatrix.length;
        List<Integer> bestPath = new ArrayList<>();
        List<Integer> bestPath2 = new ArrayList<>();
        int bestVisitedNodes = -1;
        int bestPreferenceScore = -1;
        double bestDistance = Double.POSITIVE_INFINITY;

        // Calculate the preference score for each tourist spot
        if (preferences == null) preferences = new ArrayList<>();
        int[] preferenceScore = new int[numNodes];
        for (int i = 0; i < numNodes; i++) {
            int score = 0;
            for (int j = 0; j < tags.get(i).size(); j++) {
                String tag = tags.get(i).get(j);
                if (preferences.contains(tag)) {
                    score++;
                }
            }
            preferenceScore[i] = score;
        }

        do {
            List<Integer> blackList = new ArrayList<>();
            for (int startingNode = 0; startingNode < numNodes; startingNode++) {
                Set<Integer> unvisitedNodes = new HashSet<>();
                for (int i = 0; i < numNodes; i++) {
                    unvisitedNodes.add(i);
                }

                List<Integer> path = new ArrayList<>();
                int currentNode = startingNode;
                long currentTime = startTime.getTime();

                path.add(currentNode);
                currentTime += averageTimeSpent.get(currentNode);
                unvisitedNodes.remove(currentNode);
                boolean isLunchBreakAdded = false;

                if (blackList.size() == numNodes - mustVisit.size()) break;
                while (!unvisitedNodes.isEmpty() && currentTime <= endTime.getTime()) {
                    int nextNode = -1;
                    double minScore = Double.POSITIVE_INFINITY;
                    long remainingTime = endTime.getTime() - currentTime;

                    for (int node : unvisitedNodes) {
                        if (blackList.contains(node)) continue;
                        double travelTime = timeMatrix[currentNode][node];
                        long arrivalTime = currentTime + (long) travelTime;

                        if (arrivalTime <= endTime.getTime() &&
                                openingHours.get(node).get("open") <= arrivalTime &&
                                arrivalTime + averageTimeSpent.get(node) <= openingHours.get(node).get("close")) {
                            double distanceScore = distanceMatrix[currentNode][node] * averageTimeSpent.get(node);
                            double timeScore = (openingHours.get(node).get("close") - (arrivalTime + averageTimeSpent.get(node))) * remainingTime;
                            double score = distanceScore * timeScore * (preferenceScore[node] + 1);

                            if (score < minScore) {
                                nextNode = node;
                                minScore = score;
                            }
                        }
                    }

                    if (nextNode == -1) {
                        break; // No feasible node within remaining time and constraints
                    }
                    path.add(nextNode);
                    unvisitedNodes.remove(nextNode);

                    double travelTime = timeMatrix[currentNode][nextNode];
                    currentTime += (long) (travelTime + averageTimeSpent.get(nextNode));
                    currentNode = nextNode;

                    // If current time is past 1:00 pm, then add lunch break and -1 to the path
                    if (!isLunchBreakAdded && currentTime > startTime.getTime() + 3 * 60 * 60 * 1000) {
                        long lunchBreak = 60 * 60 * 1000;
                        currentTime += lunchBreak;
                        path.add(-1);
                        isLunchBreakAdded = true;
                    }
                }

                // Check if all must-visit nodes are in the path
                Set<Integer> mustVisitSet = new HashSet<>(mustVisit);
                List<Integer> missingMustVisit = new ArrayList<>();
                for (int node : mustVisit) {
                    if (!path.contains(node)) {
                        missingMustVisit.add(node);
                    }
                }

                if (missingMustVisit.isEmpty() || mustVisit.isEmpty()) {
                    int numVisitedNodes = path.size();
                    double totalDistance = 0;

                    for (int i = 1; i < path.size(); i++) {
                        if (path.get(i) == -1 || path.get(i - 1) == -1) continue;
                        totalDistance += distanceMatrix[path.get(i - 1)][path.get(i)];
                    }

                    if (!preferences.isEmpty()) {
                        int totalPreferenceScore = 0;
                        for (int spot : path) {
                            if (spot == -1) continue;
                            totalPreferenceScore += preferenceScore[spot];
                        }
                        if (totalPreferenceScore > bestPreferenceScore ||
                                (totalPreferenceScore == bestPreferenceScore && numVisitedNodes > bestVisitedNodes) ||
                                (totalPreferenceScore == bestPreferenceScore && numVisitedNodes == bestVisitedNodes && totalDistance < bestDistance)) {
                            bestPreferenceScore = totalPreferenceScore;
                            bestPath = new ArrayList<>(path);
                            bestVisitedNodes = numVisitedNodes;
                            bestDistance = totalDistance;
                        }
                    } else if (numVisitedNodes > bestVisitedNodes || (numVisitedNodes == bestVisitedNodes && totalDistance < bestDistance)) {
                        bestPath2 = new ArrayList<>(bestPath);
                        bestPath = new ArrayList<>(path);
                        bestVisitedNodes = numVisitedNodes;
                        bestDistance = totalDistance;
                    }
                    // Clear the black list
                    blackList.clear();
                } else {
                    int node = (int) (Math.random() * numNodes);
                    while (mustVisitSet.contains(node) || blackList.contains(node)) {
                        node = (int) (Math.random() * numNodes);
                    }
                    blackList.add(node);
                    if (node == startingNode) continue;
                    startingNode--;
                }
            }

            // If still no path is found, then remove one must-visit node and try again
            if (bestPath.isEmpty()) {
                System.out.println("No path found, removing one must-visit node");
                mustVisit.remove(mustVisit.size() - 1);
            }
        } while (bestPath.isEmpty());

        if (bestPath2.isEmpty()) bestPath2 = new ArrayList<>(bestPath);

        List<List<Integer>> result = new ArrayList<>();
        result.add(bestPath);
        result.add(bestPath2);
        return result;
    }

    public static Map<String, Object> planTour(Map<String, Object> Data, Calendar startTime, Calendar endTime, List<Map<String, Object>> distandTimeresult, List<String> preferences, List<Integer> mustVisit) {
        List<Map<String, Object>> tourist_spots = (List<Map<String, Object>>) Data.get("tourist_spots");
        double[][] distanceMatrix = new double[tourist_spots.size()][tourist_spots.size()];
        double[][] timeMatrix = new double[tourist_spots.size()][tourist_spots.size()];
        List<Map<String, Long>> spotOpeningHours = new ArrayList<>();
        for (int i = 0; i < tourist_spots.size(); i++) {
            distanceMatrix[i] = new double[tourist_spots.size()];
            timeMatrix[i] = new double[tourist_spots.size()];
            for (int j = 0; j < tourist_spots.size(); j++) {
                distanceMatrix[i][j] = 0;
                timeMatrix[i][j] = 0;
            }
        }

        //take distance and time from distandTimeresult, if not exist then call google map api and store the result in database
        int n = tourist_spots.size();
        for (int j = 0; j < n; j++) {
            for (int l = 0; l < n; l++) {
                if (j == l) continue;
                int id1 = (int) tourist_spots.get(j).get("id");
                int id2 = (int) tourist_spots.get(l).get("id");

                Map<String, Object> distAndTime = null;
                for (Map<String, Object> item : distandTimeresult) {
                    if ((int) item.get("id1") == id1 && (int) item.get("id2") == id2) {
                        distAndTime = item;
                        break;
                    }
                    if ((int) item.get("id1") == id2 && (int) item.get("id2") == id1) {
                        distAndTime = item;
                        break;
                    }
                }
//                if (distAndTime == null) {
//                    System.out.println("calling google map api");
//                    distAndTime = getDirectionDistanceUsingGoogleMap(
//                            tourist_spots[j].longitude,
//                            tourist_spots[j].latitude,
//                            tourist_spots[l].longitude,
//                            tourist_spots[l].latitude
//                    );
//                    plannerService.insertDistandTime(tourist_spots[j].id, tourist_spots[l].id, distAndTime.distance, distAndTime.time);
//                }
                distanceMatrix[j][l] = (float) distAndTime.get("distance");
                distanceMatrix[l][j] = (float) distAndTime.get("distance");
                //time is stored as hours in database, converting it to milliesecond
                timeMatrix[j][l] = (float) distAndTime.get("time") * 60 * 60 * 1000;
                timeMatrix[l][j] = (float) distAndTime.get("time") * 60 * 60 * 1000;
            }
        }

        //convert startTime to date
        Date startTime_d = new Date(startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH), startTime.get(Calendar.DATE), startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE));
        Date endTime_d = new Date(endTime.get(Calendar.YEAR), endTime.get(Calendar.MONTH), endTime.get(Calendar.DATE), endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE));
        for (int i = 0; i < n; i++) {
            Map<String, Object> spot = tourist_spots.get(i);
            Date open = parseTimeString((String) spot.get("open"), startTime_d);       //start time is passed just to get the Date object
            Date close = parseTimeString((String) spot.get("close"), startTime_d);
            long openTime = open.getTime();
            long closeTime = close.getTime();
            Map<String, Long> map = new HashMap<>();
            map.put("open", openTime);
            map.put("close", closeTime);
            spotOpeningHours.add(map);
        }

        //create a array that stores the average time spent in every spots
        List<Integer> averageTimeSpent = new ArrayList<>();
        for (Map<String, Object> spot : tourist_spots) {
            averageTimeSpent.add((int) spot.get("average_time_spent") * 60 * 60 * 1000);
        }

        //create an array of objects that stores the tags of every tourist spots
        List<List<String>> tags = new ArrayList<>();
        for (Map<String, Object> spot : tourist_spots) {
            if (spot.get("tags") != null)
                tags.add(Arrays.asList(((String) spot.get("tags")).split(",")));
            else
                tags.add(new ArrayList<>());
        }

        //find the indices of the must visit spots
        List<Integer> mustVisitIndices = new ArrayList<>();
        for (Integer integer : mustVisit) {
            int index = -1;
            for (int j = 0; j < tourist_spots.size(); j++) {
                if ((int) tourist_spots.get(j).get("id") == integer) {
                    index = j;
                    break;
                }
            }
            mustVisitIndices.add(index);
        }

        //run the algorithm
        System.out.println("Before TSP: " + LocalTime.now().getMinute() + ":" + LocalTime.now().getSecond());
        List<List<Integer>> tsp_result = tsp_algorithm(distanceMatrix, timeMatrix, spotOpeningHours, averageTimeSpent, tags, startTime, endTime, preferences, mustVisitIndices);
        System.out.println("After TSP: " + LocalTime.now().getMinute() + ":" + LocalTime.now().getSecond());

        Map<String, Object> result = new HashMap<>();
        result.put("tsp_result", tsp_result);
        result.put("distanceMatrix", distanceMatrix);
        result.put("timeMatrix", timeMatrix);
        return result;
    }
}
