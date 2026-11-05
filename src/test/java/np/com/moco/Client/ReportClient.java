package np.com.moco.Client;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;


public class ReportClient {

    private static final String TOKEN_HEADER = "X-MOCO-MPS-TOKEN";

    public static final String SUMMARY = "summary";
    public static final String CUSTOMER = "customer";
    public static final String DAYWISE = "daywise";
    public static final String HOURWISE = "hourwise";

    /** type: "summary", "customer", "daywise" or "hourwise" -> GET /merchant/report/{type} */
    public Response getReport(String type, String token, String yearMonth, String outletId) {
        RequestSpecification spec = given();
        if (token != null) {
            spec = spec.header(TOKEN_HEADER, token);
        }
        if (yearMonth != null) {
            spec = spec.queryParam("yearMonth", yearMonth);
        }
        if (outletId != null) {
            spec = spec.queryParam("outletId", outletId);
        }
        return spec.log().all()
                .when().get(BaseClass.BASE_URL + "/merchant/report/" + type)
                .then().log().all()
                .extract().response();
    }

    public Response getSummary(String token, String yearMonth, String outletId) {
        return getReport(SUMMARY, token, yearMonth, outletId);
    }

    public Response getCustomer(String token, String yearMonth, String outletId) {
        return getReport(CUSTOMER, token, yearMonth, outletId);
    }

    public Response getDaywise(String token, String yearMonth, String outletId) {
        return getReport(DAYWISE, token, yearMonth, outletId);
    }

    public Response getHourwise(String token, String yearMonth, String outletId) {
        return getReport(HOURWISE, token, yearMonth, outletId);
    }
}