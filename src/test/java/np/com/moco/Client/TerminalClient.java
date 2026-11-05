package np.com.moco.Client;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;

public class TerminalClient {

    private static final String TOKEN_HEADER = "X-MOCO-MPS-TOKEN";

    // ---------------- helper ----------------

    private RequestSpecification spec(String token) {
        RequestSpecification spec = given();
        if (token != null) {
            spec = spec.header(TOKEN_HEADER, token);
        }
        return spec;
    }

    // ---------------- GET /outlet/{outletId}/terminal ----------------

    public Response getTerminals(String token, String outletId) {
        return spec(token)
                .log().all()
                .when().get(BaseClass.BASE_URL + "/outlet/" + outletId + "/terminal")
                .then().log().all()
                .extract().response();
    }

    // ---------------- GET /terminal (all terminals of the merchant, optional outlet filter) ----------------

    /** outletId may be null to list every terminal of the merchant. */
    public Response getAllTerminals(String token, String outletId) {
        RequestSpecification spec = spec(token);
        if (outletId != null) {
            spec = spec.queryParam("outletId", outletId);
        }
        return spec
                .log().all()
                .when().get(BaseClass.BASE_URL + "/terminal")
                .then().log().all()
                .extract().response();
    }

    // ---------------- POST /outlet/{outletId}/terminal ----------------

    public Response addTerminal(String token, String outletId, String jsonBody) {
        return spec(token).contentType(ContentType.JSON).body(jsonBody)
                .log().all()
                .when().post(BaseClass.BASE_URL + "/outlet/" + outletId + "/terminal")
                .then().log().all()
                .extract().response();
    }

    // ---------------- GET /outlet/{outletId}/terminal/{terminalId} ----------------

    public Response getTerminal(String token, String outletId, String terminalId) {
        return spec(token)
                .log().all()
                .when().get(BaseClass.BASE_URL + "/outlet/" + outletId + "/terminal/" + terminalId)
                .then().log().all()
                .extract().response();
    }

    // ---------------- PUT /outlet/{outletId}/terminal/{terminalId} ----------------

    public Response updateTerminal(String token, String outletId, String terminalId, String jsonBody) {
        return spec(token).contentType(ContentType.JSON).body(jsonBody)
                .log().all()
                .when().put(BaseClass.BASE_URL + "/outlet/" + outletId + "/terminal/" + terminalId)
                .then().log().all()
                .extract().response();
    }

    // ---------------- DELETE /outlet/{outletId}/terminal/{terminalId} ----------------

    /** reasonJson: {"reason":"..."} */
    public Response deleteTerminal(String token, String outletId, String terminalId, String reasonJson) {
        return spec(token).contentType(ContentType.JSON).body(reasonJson)
                .log().all()
                .when().delete(BaseClass.BASE_URL + "/outlet/" + outletId + "/terminal/" + terminalId)
                .then().log().all()
                .extract().response();
    }
}