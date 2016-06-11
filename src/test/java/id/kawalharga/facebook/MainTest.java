package id.kawalharga.facebook;

import facebook4j.Post;
import id.kawalharga.model.CommodityInput;
import id.kawalharga.model.Geolocation;
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

    Main main;

    @Before
    public void setup() throws Exception {
        main = new Main("src/test/resources/config.properties");
    }

    @Test
    public void urlTest() {

        try {
            String actual = main.getGoogleMapUrlString(new Geolocation(-6.239879, 106.8623443, 0));
            String expected = "http://maps.google.com/maps?q=-6.239879,106.862344";
            assertEquals(expected, actual);
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }

    }

    @Test
    public void getInputToBePostedTest() {
        try {
            CommodityInput input = main.getInputToBePosted();
            assert true;
        } catch (Exception e) {
            assert false;
        }
    }

//    @Test
    public void postTest() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
            String dateInString = "03-06-2016";
            List<CommodityInput> list = main.getService().getLatestCommodityInputs(sdf.parse(dateInString), 1);
            CommodityInput input = list.get(0);
            String id = main.post(input);
            assert id != null;
            if (id != null) {
                Post post = main.getPost(id);
                main.insertPost(post, input);
            }
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

//    @Test
    public void getLastPostedInputCreatedDateTest() {
        try {
            Date date = main.getLastPostedInputCreatedDate();
            assert date != null;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }
}
