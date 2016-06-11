package id.kawalharga.facebook;

import facebook4j.*;
import id.kawalharga.database.Service;
import id.kawalharga.model.*;
import org.apache.log4j.Logger;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class Main {

    final static Logger logger = Logger.getLogger(Main.class);
    private static final String GOOGLE_MAP_URL = "http://maps.google.com/maps?q=%f,%f";

    private Facebook facebook;
    private Service service;

    public Main(String dbConfig) throws Exception {
        facebook = new FacebookFactory().getInstance();
        service = Service.getInstance(dbConfig);
        this.createTableIfNotExist();
    }

    public Service getService() {
        return service;
    }

    String getGoogleMapUrlString(Geolocation geolocation) {
        return String.format(GOOGLE_MAP_URL, geolocation.getLat(), geolocation.getLng());
    }

    public CommodityInput getInputToBePosted() throws Exception {
        CommodityInput res = null;
        Calendar beginningOfDay = new GregorianCalendar();
        beginningOfDay.set(Calendar.HOUR, 0);
        beginningOfDay.set(Calendar.MINUTE, 0);
        beginningOfDay.set(Calendar.SECOND, 0);
        List<CommodityInput> commodityInputList = this.getInputsToBePosted(beginningOfDay.getTime(), 1);
        res = (commodityInputList.size() > 0) ? commodityInputList.get(0) : res;
        return res;
    }

    public List<CommodityInput> getInputsToBePosted(Date date, int limit) throws Exception {
        List<CommodityInput> commodityInputList = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Connection connection = this.getService().connectToDatabase();
        try {
            Calendar nextDate = Calendar.getInstance();
            nextDate.setTime(date);
            nextDate.add(Calendar.DATE, 1);
            pstmt = connection.prepareStatement("select " +
                    "i.id, " +
                    "c.name, " +
                    "r.id as location_id, " +
                    "r.name as location, " +
                    "i.price, " +
                    "i.amount, " +
                    "i.lat, " +
                    "i.lng, " +
                    "i.description, " +
                    "i.date_created, " +
                    "u.id as user_id," +
                    "u.nama as user_name," +
                    "u.username as user_username," +
                    "u.alamat as user_address," +
                    "u.nohp as user_phone," +
                    "u.kodepos as user_postal_code," +
                    "u.email as user_email " +
                    "from comodity_input i join auth_user u on i.user_id = u.id " +
                    "join comodity c on i.comodity_name_id = c.id " +
                    "join region r on i.region_id = r.id " +
                    "where i.date_created >= ? " +
                    "and i.id not in ( select comodity_input_id from post_fb ) " +
                    "order by id asc limit ?");
            pstmt.setDate(1, new java.sql.Date(date.getTime()));
            pstmt.setInt(2, limit);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                id.kawalharga.model.User user = new id.kawalharga.model.User(
                        rs.getLong("user_id"),
                        rs.getString("user_username"),
                        rs.getString("user_name"),
                        rs.getString("user_address"),
                        rs.getString("user_phone"),
                        rs.getString("user_postal_code"),
                        rs.getString("user_email"));
                CommodityInput commodityInput = new CommodityInput(
                        rs.getLong("id"),
                        user,
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getDouble("price"),
                        rs.getDouble("lat"),
                        rs.getDouble("lng"),
                        rs.getLong("location_id"),
                        rs.getString("description"),
                        rs.getDate("date_created")
                );
                commodityInputList.add(commodityInput);
                logger.debug("retrieved: " + commodityInput);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (rs != null)
                rs.close();
            if (pstmt != null)
                pstmt.close();
        }
        this.getService().closeDatabaseConnection();
        return commodityInputList;
    }

    public String post(CommodityInput commodityInput) throws Exception {
        String id = null;
        URL googleMapUrl = new URL(this.getGoogleMapUrlString(commodityInput.getGeo()));
        try {
            String message = commodityInput.toString();
            id = facebook.postLink(googleMapUrl, message);
            logger.info(id);
        } catch (Exception e) {
            throw e;
        }
        return id;
    }

    public Post getPost(String id) throws Exception {
        return facebook.getPost(id);
    }

    void createTableIfNotExist() throws Exception {
        Connection connection = this.getService().connectToDatabase();
        Statement statement = connection.createStatement();
        String createTableSQL = "CREATE TABLE IF NOT EXISTS post_fb("
                + "id VARCHAR(50) NOT NULL, "
                + "comodity_input_id BIGINT NOT NULL, "
                + "text VARCHAR(500) NOT NULL, "
                + "likes INT DEFAULT 0, "
                + "dislikes INT DEFAULT 0, "
                + "neutral INT DEFAULT 0, "
                + "created_date DATE NOT NULL, " + "PRIMARY KEY (id) "
                + ")";
        boolean result = statement.execute(createTableSQL);
        if (result) logger.info(createTableSQL);
        logger.info("Database creation: " + result);
        this.getService().closeDatabaseConnection();
    }

    boolean insertPost(Post post, CommodityInput commodityInput) throws Exception {
        Connection dbConnection = this.getService().connectToDatabase();
        String insertSQL = "INSERT INTO post_fb (id, comodity_input_id, text, created_date) VALUES (?, ?, ?, ?)";
        PreparedStatement statement = dbConnection.prepareStatement(insertSQL);
        statement.setString(1, post.getId());
        statement.setLong(2, commodityInput.getId());
        statement.setString(3, post.getMessage());
        statement.setDate(4, new java.sql.Date(post.getCreatedTime().getTime()));
        logger.debug(statement);
        boolean success = statement.execute();
        if (statement != null) statement.close();
        this.getService().closeDatabaseConnection();
        return success;
    }

    Date getLastPostedInputCreatedDate() throws Exception {
        Date lastId = null;
        Connection dbConnection = this.getService().connectToDatabase();
        String selectSQL = "SELECT created_date FROM post_fb ORDER BY created_date DESC LIMIT 1";
        Statement statement = dbConnection.createStatement();
        logger.debug(statement);
        ResultSet rs = statement.executeQuery(selectSQL);
        if (rs.next()) {
            lastId = rs.getDate("created_date");
            logger.info("Last post id: " + lastId);
        }
        if (statement != null) statement.close();
        this.getService().closeDatabaseConnection();
        return lastId;
    }


    public static void main(String args[]) {
        try {
            Main main = new Main(args[0]);
            CommodityInput input = main.getInputToBePosted();
            if (input != null) {
                String postId = main.post(input);
                Post post = main.getPost(postId);
                main.insertPost(post, input);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}

