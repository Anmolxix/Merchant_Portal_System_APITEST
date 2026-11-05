package np.com.moco.Client;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import np.com.moco.base.BaseClass;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class MerchantClient {

    private static final String TOKEN_HEADER = "X-MOCO-MPS-TOKEN";

    public static final String MERCHANT_ENDPOINT = "/merchant";
    public static final String INTERCHANGE_ENDPOINT = "/merchant/acquirer/interchange";
    public static final String OPERATOR_ENDPOINT = "/merchant/operator";
    public static final String OUTLET_ENDPOINT = "/merchant/outlet";

    // ---------------- helpers ----------------

    private RequestSpecification spec(String token) {
        RequestSpecification spec = given();
        if (token != null) {
            spec = spec.header(TOKEN_HEADER, token);
        }
        return spec;
    }

    private RequestSpecification jsonSpec(String token, Object body) {
        return spec(token).contentType(ContentType.JSON).body(body);
    }

    private Response get(RequestSpecification spec, String path) {
        return spec.log().all()
                .when().get(BaseClass.BASE_URL + path)
                .then().log().all()
                .extract().response();
    }

    private Response post(RequestSpecification spec, String path) {
        return spec.log().all()
                .when().post(BaseClass.BASE_URL + path)
                .then().log().all()
                .extract().response();
    }

    private Response put(RequestSpecification spec, String path) {
        return spec.log().all()
                .when().put(BaseClass.BASE_URL + path)
                .then().log().all()
                .extract().response();
    }

    private Response delete(RequestSpecification spec, String path) {
        return spec.log().all()
                .when().delete(BaseClass.BASE_URL + path)
                .then().log().all()
                .extract().response();
    }

    // ---------------- GET /merchant, PUT /merchant ----------------

    public Response getMerchant(String token) {
        return get(spec(token), MERCHANT_ENDPOINT);
    }

    /** body: a JSON String, or a Map/object that RestAssured serialises to JSON. */
    public Response updateMerchant(String token, Object body) {
        return put(jsonSpec(token, body), MERCHANT_ENDPOINT);
    }

    // ---------------- GET /merchant/acquirer/interchange ----------------

    public Response getInterchanges(String token) {
        return get(spec(token), INTERCHANGE_ENDPOINT);
    }

    // ---------------- GET /merchant/outlet (helper: outlet ids for operator tests) ----------------

    public Response getOutlets(String token) {
        return get(spec(token), OUTLET_ENDPOINT);
    }

    // ---------------- /merchant/operator ----------------

    /** List all operators. */
    public Response getOperators(String token) {
        return get(spec(token), OPERATOR_ENDPOINT);
    }

    /** Search operators: outletId, roleId, status, name. */
    public Response getOperators(String token, Map<String, String> filters) {
        return get(spec(token).queryParams(filters), OPERATOR_ENDPOINT);
    }

    public Response getOperator(String token, String operatorId) {
        return get(spec(token), OPERATOR_ENDPOINT + "/" + operatorId);
    }

    public Response addOperator(String token, String jsonBody) {
        return post(jsonSpec(token, jsonBody), OPERATOR_ENDPOINT);
    }

    public Response updateOperator(String token, String operatorId, String jsonBody) {
        return put(jsonSpec(token, jsonBody), OPERATOR_ENDPOINT + "/" + operatorId);
    }

    /** status: "enabled" or "disabled". reasonJson: {"reason":"..."} */
    public Response updateOperatorStatus(String token, String operatorId, String status, String reasonJson) {
        return put(jsonSpec(token, reasonJson), OPERATOR_ENDPOINT + "/" + operatorId + "/" + status);
    }

    /** reasonJson: {"reason":"..."} */
    public Response deleteOperator(String token, String operatorId, String reasonJson) {
        return delete(jsonSpec(token, reasonJson), OPERATOR_ENDPOINT + "/" + operatorId);
    }
}