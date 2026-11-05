package np.com.moco.Client;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import np.com.moco.base.BaseClass;

import java.util.Map;

import static io.restassured.RestAssured.given;


public class TransactionClient {

    private static final String TOKEN_HEADER = "X-MOCO-MPS-TOKEN";

    public static final String TRANSACTION_ENDPOINT = "/merchant/transaction";

    // ---------------- helper ----------------

    private RequestSpecification spec(String token) {
        RequestSpecification spec = given();
        if (token != null) {
            spec = spec.header(TOKEN_HEADER, token);
        }
        return spec;
    }

    // ---------------- GET /merchant/transaction ----------------

    public Response getTransactions(String token, Map<String, String> params) {
        return spec(token).queryParams(params)
                .log().all()
                .when().get(BaseClass.BASE_URL + TRANSACTION_ENDPOINT)
                .then().log().all()
                .extract().response();
    }

    // ---------------- GET /merchant/transaction/download (CSV) ----------------

    public Response downloadTransactions(String token, Map<String, String> params) {
        return spec(token).queryParams(params)
                .log().all()
                .when().get(BaseClass.BASE_URL + TRANSACTION_ENDPOINT + "/download")
                .then().log().all()
                .extract().response();
    }

    // ---------------- GET /merchant/transaction/refund/{id} ----------------

    public Response getRefunds(String token, String purchaseTxnId) {
        return spec(token)
                .log().all()
                .when().get(BaseClass.BASE_URL + TRANSACTION_ENDPOINT + "/refund/" + purchaseTxnId)
                .then().log().all()
                .extract().response();
    }

    // ---------------- POST /merchant/transaction/refund/{id} ----------------

    public Response refund(String token, String purchaseTxnId, String amount, String reason, String pin) {
        RequestSpecification spec = spec(token).contentType(ContentType.URLENC);
        if (amount != null) {
            spec = spec.formParam("amount", amount);
        }
        if (reason != null) {
            spec = spec.formParam("reason", reason);
        }
        if (pin != null) {
            spec = spec.formParam("pin", pin);
        }
        return spec
                .log().all()
                .when().post(BaseClass.BASE_URL + TRANSACTION_ENDPOINT + "/refund/" + purchaseTxnId)
                .then().log().all()
                .extract().response();
    }

    // ---------------- GET /merchant/transaction/status ----------------

    public Response getTransactionStatus(String token, Map<String, String> params) {
        return spec(token).queryParams(params)
                .log().all()
                .when().get(BaseClass.BASE_URL + TRANSACTION_ENDPOINT + "/status")
                .then().log().all()
                .extract().response();
    }

}