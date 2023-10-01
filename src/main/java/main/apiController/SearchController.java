package main.apiController;
import main.service.PlannerService;
import main.service.SearchService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class SearchController {
    private static final Set<String> stopWords = new HashSet<>(Arrays.asList("the", "and", "in", "from", "or", "to" /* Add more stop words here */));
    private static final Set<String> specificWordsToCheck = new HashSet<>(Arrays.asList("historic", "gardens", "liberation", "museum", "river", "histroy", "rich", "fort", "palace",
            "temple", "mosque", "church", "pagoda", "monastery", "beach", "hill", "mountain", "lake", "waterfall",
            "national", "park", "wildlife", "sanctuary", "zoo", "safari", "cave", "canyon", "volcano", "geyser", "desert"));

    public double calculateJaccardSimilarity(String description1, String description2) {
        String[] words1 = description1.toLowerCase().split(" ");
        String[] words2 = description2.toLowerCase().split(" ");

        Set<String> intersection = new HashSet<>(Arrays.asList(words1));
        intersection.retainAll(Arrays.asList(words2));

        Set<String> union = new HashSet<>(Arrays.asList(words1));
        union.addAll(Arrays.asList(words2));

        double jaccardSimilarity = (double) intersection.size() / union.size();
        return jaccardSimilarity;
    }

    public boolean descriptionsContainSpecificWords(String description1, String description2, Set<String> specificWords) {
        String[] words1 = description1.toLowerCase().split(" ");
        String[] words2 = description2.toLowerCase().split(" ");

        for (String word : specificWords) {
            if (!(Arrays.asList(words1).contains(word) && Arrays.asList(words2).contains(word))) {
                return false;
            }
        }
        return true;
    }

    public double calculateSimilarityScore(double jaccardSimilarity, boolean containSpecificWords) {
        double jaccardWeight = 0.6;
        double specificWordsWeight = 0.4;

        return jaccardWeight * jaccardSimilarity + specificWordsWeight * (containSpecificWords ? 1 : 0);
    }

    @Autowired
    private SearchService searchService;

    @GetMapping("/api/search")
    public ResponseEntity<Object> performSearch(@RequestParam String query) {
        List<Map<String, Object>> results = new ArrayList<>();

        //split the query words into an array of strings, filter out the stop words, and convert all the words to lowercase
        String[] queryWords = query.split(" ");
        queryWords = Arrays.stream(queryWords).filter(word -> !stopWords.contains(word)).map(word -> word.toLowerCase()).toArray(String[]::new);

        List<Map<String, Object>> touristSpots = searchService.getAllTouristSpots();


        for (Map<String, Object> spot : touristSpots) {
            String spotName = ((String) spot.get("Name")).toLowerCase();
            String spotDescription = ((String) spot.get("Description")).toLowerCase();
            String spotLocation = ((String) spot.get("Location_Name")).toLowerCase();

            // Check if each query word is present in spot name, description, or location words
            boolean isMatch = true;

            for (String queryWord : queryWords) {
                // Check if the query word is present in at least one of the spot's name, description, or location
                if (!spotName.contains(queryWord) && !spotDescription.contains(queryWord) && !spotLocation.contains(queryWord)) {
                    //now trim the query word from front and back one character and check again
                    if (queryWord.length() > 2) {
                        String trimmedQueryWord = queryWord.substring(1, queryWord.length() - 1);
                        if (!spotName.contains(trimmedQueryWord) && !spotDescription.contains(trimmedQueryWord) && !spotLocation.contains(trimmedQueryWord)) {
                            isMatch = false;
                            break;
                        } else {
                            continue;
                        }
                    }
                    isMatch = false;
                    break;
                }
            }

            if (isMatch)
                results.add((spot));

        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/api/search/tourist_spot")
    public ResponseEntity<Object> getTouristSpot(@RequestParam int tourist_spot_id) {
        int spotId = tourist_spot_id;
        Map<String, Object> tourist_spot = searchService.getTouristSpot(spotId);
        List<Map<String, Object>> touristSpots = searchService.getAllTouristSpots();
        List<Map<String, Object>> similar_tourist_spots = new ArrayList<>();
        //now i have to compare the description of the tourist spot with all the other tourist spots
        for (Map<String, Object> spot : touristSpots) {
            if ((int) spot.get("Tourist_Spot_ID") != spotId) {
                double jaccardSimilarity = calculateJaccardSimilarity((String) tourist_spot.get("Description"), (String) spot.get("Description"));
                boolean containSpecificWords = descriptionsContainSpecificWords((String) tourist_spot.get("Description"), (String) spot.get("Description"), specificWordsToCheck);
                double similarityScore = calculateSimilarityScore(jaccardSimilarity, containSpecificWords);
                if (similarityScore > 0.08) {
                    similar_tourist_spots.add(spot);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tourist_spot", tourist_spot);
        result.put("similar_tourist_spots", similar_tourist_spots);

        return ResponseEntity.ok(result);
    }
}
