package np.com.moco.Client;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;

public class OperatorLoginClient {

    public static final String AUTH_ENDPOINT = "/operator/authentication"; // keep your own value

    public Response login(String id, int pin) {
        return given()
                .contentType(ContentType.URLENC)
                .formParam("id", id)
                .formParam("pin", pin)
                .log().all()                          // logs the request
                .when()
                .post(BaseClass.BASE_URL + AUTH_ENDPOINT)
                .then()                       // logs the response
                .extract().response();
    }

    public Response loginWithInvalidValues(String id, String pin) {
        return given().contentType(ContentType.URLENC)
                .formParam("id", id)
                .formParam("pin", pin)
                .when().post(BaseClass.BASE_URL + AUTH_ENDPOINT);
    }

    public Response loginWithoutPin(String id) {
        return given().contentType(ContentType.URLENC)
                .formParam("id", id)
                .when().post(BaseClass.BASE_URL + AUTH_ENDPOINT);
    }

    public Response loginWithoutId(int pin) {
        return given().contentType(ContentType.URLENC)
                .formParam("pin", pin)
                .when().post(BaseClass.BASE_URL + AUTH_ENDPOINT);
    }

    public Response loginWithoutCredentials() {
        return given().contentType(ContentType.URLENC)
                .when().post(BaseClass.BASE_URL + AUTH_ENDPOINT);
    }

    public Response getDashboard(String token) {
        return given().header("X-MOCO-MPS-TOKEN", token)
                .when().get(BaseClass.BASE_URL + "/merchant/dashboard");
    }

    public Response getDashboardWithoutToken() {
        return given().when().get(BaseClass.BASE_URL + "/merchant/dashboard");
    }

    public Response logout(String token) {
        return given().header("X-MOCO-MPS-TOKEN", token)
                .when().delete(BaseClass.BASE_URL + AUTH_ENDPOINT);
    }
    public Response loginToMerchant(String merchantId, String token) {
        return given().header("X-MOCO-MPS-TOKEN", token)
                .when().post(BaseClass.BASE_URL + "/merchant/" + merchantId + "/operator/login");
    }
}