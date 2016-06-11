package id.kawalharga.facebook;

import facebook4j.*;
import id.kawalharga.database.Service;
import id.kawalharga.model.CommodityInput;
import id.kawalharga.model.Geolocation;
import org.apache.log4j.Logger;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

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
        beginningOfDay.set(Calendar.HOUR_OF_DAY, 0);
        beginningOfDay.set(Calendar.MINUTE, 0);
        beginningOfDay.set(Calendar.SECOND, 0);
        beginningOfDay.set(Calendar.MILLISECOND, 0);
        Date lastInputCreatedDate = this.getLastPostedInputCreatedDate();
        lastInputCreatedDate = (lastInputCreatedDate == null) ? beginningOfDay.getTime() : lastInputCreatedDate;
        List<CommodityInput> commodityInputList = this.service.getLatestCommodityInputs(lastInputCreatedDate, 5);
        for (CommodityInput input : commodityInputList) {
            if (input.getCreatedAt().after(lastInputCreatedDate)) {
                res = input;
                break;
            }
        }
        return res;
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

