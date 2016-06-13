package id.kawalharga.facebook;

import facebook4j.Post;
import id.kawalharga.model.CommodityInput;
import id.kawalharga.model.Geolocation;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by yohanesgultom on 11/06/16.
 */
public class MainTest {

    final static Logger logger = Logger.getLogger(MainTest.class);

    Main main;

    @Before
    public void setup() throws Exception {
        main = new Main("src/test/resources/config.properties");
    }

    @Test
    public void renewAccessTokenTest() {
        try {
            main.renewAccessToken();
            assert true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }

    @Test
    public void urlTest() {

        try {
            String actual = main.getGoogleMapUrlString(new Geolocation(-6.239879, 106.8623443, 0));
            String expected = "http://maps.google.com/maps?q=-6.239879,106.862344";
            assertEquals(expected, actual);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }

    }

    @Test
    public void getInputsToBePostedTest() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
            Date date = sdf.parse("03-06-2016");
            List<CommodityInput> list = main.getInputsToBePosted(date, 3);
            assert list.size() == 3;
            for (CommodityInput input:list) {
                assert !input.getCreatedAt().before(date);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }


    @Test
    public void getInputToBePostedTest() {
        try {
            CommodityInput input = main.getInputToBePosted();
            assert true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }


//    @Test
    public void postTest() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
            String dateInString = "02-06-2016";
            List<CommodityInput> list = main.getInputsToBePosted(sdf.parse(dateInString), 1);
            CommodityInput input = list.get(0);
            String id = main.post(input);
            logger.info("Posted to page: " + id);
            assert id != null;
            if (id != null) {
                Post post = main.getPost(id);
                main.insertPost(post, input);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }

    @Test
    public void getLastPostedInputCreatedDateTest() {
        try {
            Date date = main.getLastPostedInputCreatedDate();
            assert date != null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }

    @Test
    public void getLastPostIdsTest() {
        try {
            List<String> ids = main.getLastPostIds(10);
            assert ids != null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }

    @Test
    public void updatePostsStatusTest() {
        try {
            List<String> ids = main.getLastPostIds(10);
            main.updatePostsStatus(ids);
            assert true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }
}
