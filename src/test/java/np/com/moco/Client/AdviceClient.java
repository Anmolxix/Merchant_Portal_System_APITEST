package np.com.moco.Client;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;


public class AdviceClient {

    private static final String TOKEN_HEADER = "X-MOCO-MPS-TOKEN";

    public static final String ADVICE_ENDPOINT = "/merchant/advice";
    public static final String ADVICE_DOWNLOAD_ENDPOINT = "/merchant/advice/download";
    public static final String ADVICE_ZIP_ENDPOINT = "/merchant/advice/download/zip";

    // ---------------- helpers ----------------

    private RequestSpecification spec(String token) {
        RequestSpecification spec = given();
        if (token != null) {
            spec = spec.header(TOKEN_HEADER, token);
        }
        return spec;
    }

    private Response getJson(RequestSpecification spec, String path) {
        return spec.log().all()
                .when().get(BaseClass.BASE_URL + path)
                .then().log().all()
                .extract().response();
    }

    private Response getBinary(RequestSpecification spec, String path) {
        return spec.log().all()
                .when().get(BaseClass.BASE_URL + path)
                .then().log().status().log().headers()
                .extract().response();
    }

    // ---------------- GET /merchant/advice ----------------

    public Response getAdviceList(String token, String startDate, String endDate) {
        RequestSpecification spec = spec(token);
        if (startDate != null) {
            spec = spec.queryParam("startDate", startDate);
        }
        if (endDate != null) {
            spec = spec.queryParam("endDate", endDate);
        }
        return getJson(spec, ADVICE_ENDPOINT);
    }

    // ---------------- GET /merchant/advice/download (PDF) ----------------

    public Response downloadAdvice(String token, String reportName, String date) {
        RequestSpecification spec = spec(token);
        if (reportName != null) {
            spec = spec.queryParam("reportName", reportName);
        }
        if (date != null) {
            spec = spec.queryParam("date", date);
        }
        return getBinary(spec, ADVICE_DOWNLOAD_ENDPOINT);
    }

    // ---------------- GET /merchant/advice/download/zip ----------------

    public Response getAdviceZipLink(String token, String startDate, String endDate) {
        RequestSpecification spec = spec(token);
        if (startDate != null) {
            spec = spec.queryParam("startDate", startDate);
        }
        if (endDate != null) {
            spec = spec.queryParam("endDate", endDate);
        }
        return getJson(spec, ADVICE_ZIP_ENDPOINT);
    }
}