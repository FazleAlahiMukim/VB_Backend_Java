package main.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SearchService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    public List<Map<String, Object>> getAllTouristSpots() {
        String sql =
                """
                select ts."Tourist_Spot_ID", ts."Name", ts."Image_Url", ts."Description", ts."Rating", ts."Longitude", ts."Latitude", ts."Average_Time_Spent", l."Location_Name"\s
                from "Tourist_Spot" ts join "Location" l on l."Location_ID" = ts."Location_ID"
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public Map<String, Object> getTouristSpot(int id) {
        String sql =
                """
                select ts."Tourist_Spot_ID", ts."Name", ts."Image_Url", ts."Description", ts."Rating", ts."Longitude", ts."Latitude", ts."Average_Time_Spent", l."Location_Name"
                from "Tourist_Spot" ts\s
                join "Location" l on l."Location_ID" = ts."Location_ID" where "Tourist_Spot_ID" = ?;
                """;
        return jdbcTemplate.queryForMap(sql, id);
    }
}
