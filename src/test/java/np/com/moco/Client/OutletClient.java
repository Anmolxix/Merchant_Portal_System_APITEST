package np.com.moco.Client;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;


public class OutletClient {

    private static final String TOKEN_HEADER = "X-MOCO-MPS-TOKEN";

    // ---------------- helper ----------------

    private RequestSpecification spec(String token) {
        RequestSpecification spec = given();
        if (token != null) {
            spec = spec.header(TOKEN_HEADER, token);
        }
        return spec;
    }

    // ---------------- GET /merchant/outlet ----------------

    public Response getOutlets(String token) {
        return spec(token)
                .log().all()
                .when().get(BaseClass.BASE_URL + "/merchant/outlet")
                .then().log().all()
                .extract().response();
    }

    // ---------------- GET /outlet/{outletId} ----------------

    public Response getOutlet(String token, String outletId) {
        return spec(token)
                .log().all()
                .when().get(BaseClass.BASE_URL + "/outlet/" + outletId)
                .then().log().all()
                .extract().response();
    }

    // ---------------- PUT /outlet/{outletId} (sends a change REQUEST) ----------------

    /** body: a JSON String, or a Map that RestAssured turns into JSON. */
    public Response updateOutlet(String token, String outletId, Object body) {
        return spec(token).contentType(ContentType.JSON).body(body)
                .log().all()
                .when().put(BaseClass.BASE_URL + "/outlet/" + outletId)
                .then().log().all()
                .extract().response();
    }
}