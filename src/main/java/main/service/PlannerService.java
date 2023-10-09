package main.service;
import main.entity.AllPlans;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PlannerService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> findAllLocationInfo() {
        String sql = "SELECT * FROM \"Location\"";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findAllDistAndTime(String location) {
        String sql =
                """
                SELECT * FROM "Spot_Distance_Time" 
                WHERE "id1" IN 
                (SELECT "Tourist_Spot_ID" FROM "Tourist_Spot" 
                WHERE "Location_ID" = (SELECT "Location_ID" FROM "Location" WHERE "Location_Name" = ?))
                """;
        return jdbcTemplate.queryForList(sql, location);
    }

    public List<Map<String, Object>> findSpotsOpenOnDay(String location, int day) {
        String sql =
                """
                SELECT *
                FROM "Tourist_Spot" ts
                JOIN spot_opening_hours soh ON ts."Tourist_Spot_ID" = soh.tourist_spot_id
                WHERE ts."Location_ID" = (SELECT "Location_ID" FROM "Location" WHERE "Location_Name" = ?) AND soh.day_id = ?
                """;
        return jdbcTemplate.queryForList(sql, location, day);
    }

    public List<Map<String, Object>> findAllDistAndTimeLocation() {
        String sql =
                """
                SELECT l1."Location_Name" AS name1, l2."Location_Name" AS name2, ldt."time", ldt.distance
                FROM "Location_Distance_Time" ldt
                INNER JOIN "Location" l1 ON ldt.id1 = l1."Location_ID"
                INNER JOIN "Location" l2 ON ldt.id2 = l2."Location_ID";
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<AllPlans> findAllPlans(String email) {
        String sql =
                "select p.email, p.plan_id, p.planname, dbd.\"date\", " +
                "(select c.image_url from cluster c where c.plan_id = p.plan_id limit 1 offset (select floor(random() *(select count(*) from cluster cl where cl.plan_id = p.plan_id)))) " +
                "from plan p join day_by_day dbd on p.plan_id = dbd.plan_id " +
                "where p.email = ? and dbd.day_order = 0 " +
                "order by dbd.\"date\" asc ";

        return jdbcTemplate.query(sql, new Object[]{email}, (rs, rowNum) -> {
            AllPlans entity = new AllPlans();
            entity.email = rs.getString("email");
            entity.plan_id = rs.getInt("plan_id");
            entity.planname = rs.getString("planname");
            entity.date = rs.getString("date");
            entity.image_url = rs.getString("image_url");
            return entity;
        });
    }

    public void checklist(int id) {
        String sql = "select update_checklist(?)";
        jdbcTemplate.query(sql, new Object[]{id}, (rs, rowNum) -> {
            return null;
        });
    }

    public List<Map<String, Object>> findPlan(int id) {
        String sql = "select * from plan p join plan_destination pd on p.plan_id = pd.plan_id where p.plan_id = ? order by pd.destination_order";
        return jdbcTemplate.queryForList(sql, id);
    }

    public List<Map<String, Object>> findDayByDay(int id) {
        String sql =
        """
        select *
        from day_by_day dbd
        join cluster c on dbd.day_order = c.day_order
        join hotel h on dbd.day_order = h.day_order
        where dbd.plan_id = ? and c.plan_id = ? and h.plan_id = ?
        order by dbd.day_order, c.cluster_order
        """;
        return jdbcTemplate.queryForList(sql, id, id, id);
    }

    public List<Map<String, Object>> findTravel(int id, String date) {
        String sql =
        """
        select *
        from travel t
        where t.plan_id = ? and t.date = ?;
        """;
        return jdbcTemplate.queryForList(sql, id, date);
    }

    public void deletePlan(int id) {
        String sql = "select delete_records_by_id(?);";
        jdbcTemplate.query(sql, new Object[]{id}, (rs, rowNum) -> null);
    }

    public int getPlanId(String email, String planName, String currentCity, String type, int totalCost) {
        String sql = "insert into plan (email, planname, currentcity, type, totalcost) values(?, ?, ?, ?, ?) returning plan_id";
        return jdbcTemplate.queryForObject(sql, new Object[]{email, planName, currentCity, type, totalCost}, Integer.class);
    }

    public void storePlanDestination(int plan_id, int destination_order, String name, int days, double lat, double lng, double distance, double time) {
        String sql = "insert into plan_destination (plan_id, destination_order, name, days, lat, lng, distance, time) values(?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, plan_id, destination_order, name, days, lat, lng, distance, time);
    }

    public void storeDayByDay(int plan_id, int day_order, String date, String location) {
        String sql = "insert into day_by_day (plan_id, day_order, date, location) values(?, ?, ?, ?)";
        jdbcTemplate.update(sql, plan_id, day_order, date, location);
    }

    public void storeCluster(int plan_id, int day_order, int cluster_order, String name, int id, String start_time, String end_time, double lat, double lng, int rating, String description, String image_url, int average_time_spent, String open, String close, int cost) {
        String sql = "insert into cluster (plan_id, day_order, cluster_order, name, id, starttime, endtime, lat, lng, rating, description, image_url, average_time_spent, open, close, cost) values(? ,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, plan_id, day_order, cluster_order, name, id, start_time, end_time, lat, lng, rating, description, image_url, average_time_spent, open, close, cost);
    }

    public void storeHotel(int plan_id, int day_order, String place, double lat, double lng, String startDate, String endDate) {
        String sql = "insert into hotel (plan_id, day_order, hotel_place, hotel_lat, hotel_lng, startdate, enddate) values(?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, plan_id, day_order, place, lat, lng, startDate, endDate);
    }

    public void storeTravel(int plan_id, int day_order, String previous_location, String next_location, double distance, double time, String date_travel) {
        String sql = "insert into travel (plan_id, day_order, previous_location, next_location, distance, time, date) values(?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, plan_id, day_order, previous_location, next_location, distance, time, date_travel);
    }

    public List<Map<String, Object>> findAllSpotsWithTime(String place, int day) {
        String sql =
        """
        SELECT * FROM "Tourist_Spot" ts 
        JOIN spot_opening_hours soh ON ts."Tourist_Spot_ID" = soh.tourist_spot_id 
        WHERE ts."Location_ID" = (SELECT "Location_ID" FROM "Location" WHERE "Location_Name" = ?) AND soh.day_id = ?
        """;
        return jdbcTemplate.queryForList(sql, place, day);
    }

    public List<Map<String, Object>> findAllPublicPlans() {
        String sql =
        """
        select p.email, p.plan_id, p.planname, dbd."date",
        (select c.image_url from cluster c where c.plan_id = p.plan_id limit 1 offset (select floor(random() *(select count(*) from cluster cl where cl.plan_id = p.plan_id))))
        from plan p
        join day_by_day dbd on p.plan_id = dbd.plan_id
        where p.type = 'public' and dbd.day_order = 0
        """;
        return jdbcTemplate.queryForList(sql);
    }

    public void changeType(int id) {
        String sql = "update plan set type = 'public' where plan_id = ?;";
        jdbcTemplate.update(sql, id);
    }
}
