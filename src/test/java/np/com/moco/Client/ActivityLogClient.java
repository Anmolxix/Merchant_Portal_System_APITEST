package np.com.moco.Client;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;


public class ActivityLogClient {

    private static final String TOKEN_HEADER = "X-MOCO-MPS-TOKEN";

    public static final String ACTIVITY_ENDPOINT = "/merchant/operator/activity";

    public Response getActivityLog(String token, String startDate, String endDate,
                                   String operatorId, String sectionId,
                                   String startRecord, String count) {
        RequestSpecification spec = given();

        if (token != null) {
            spec = spec.header(TOKEN_HEADER, token);
        }
        if (startDate != null) {
            spec = spec.queryParam("startDate", startDate);
        }
        if (endDate != null) {
            spec = spec.queryParam("endDate", endDate);
        }
        if (operatorId != null) {
            spec = spec.queryParam("id", operatorId);
        }
        if (sectionId != null) {
            spec = spec.queryParam("sectionId", sectionId);
        }
        if (startRecord != null) {
            spec = spec.queryParam("startRecord", startRecord);
        }
        if (count != null) {
            spec = spec.queryParam("count", count);
        }

        return spec.log().all()
                .when().get(BaseClass.BASE_URL + ACTIVITY_ENDPOINT)
                .then().log().all()
                .extract().response();
    }
}