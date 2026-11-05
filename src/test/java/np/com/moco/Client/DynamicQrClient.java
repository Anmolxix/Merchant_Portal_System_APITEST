package np.com.moco.Client;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;


public class DynamicQrClient {

    private static final String TOKEN_NAME = "X-MOCO-MPS-TOKEN";

    /** Path of the dynamic QR endpoint. If the portal uses a different address, change only this line. */
    public static final String DYNAMIC_QR_PATH = "/merchant/transaction/qr";

    public Response getDynamicQr(String token, String outletId, String amount, String referenceNumber) {
        // timeouts so a request that never finishes can't hang the whole run
        RestAssuredConfig timeouts = RestAssured.config().httpClient(
                HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 15000)
                        .setParam("http.socket.timeout", 15000));

        RequestSpecification spec = given().config(timeouts);

        if (token != null) {
            // the swagger puts the token in the query string; it is also sent as a header
            spec = spec.header(TOKEN_NAME, token).queryParam(TOKEN_NAME, token);
        }
        if (outletId != null) {
            spec = spec.queryParam("outletId", outletId);
        }
        if (amount != null) {
            spec = spec.queryParam("amount", amount);
        }
        if (referenceNumber != null) {
            spec = spec.queryParam("referenceNumber", referenceNumber);
        }

        return spec.log().all()
                .when().get(BaseClass.BASE_URL + DYNAMIC_QR_PATH)
                .then().log().status().log().headers()
                .extract().response();
    }
}