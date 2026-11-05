package np.com.moco.Client;

import io.restassured.response.Response;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;

public class MccClient {

    public static final String MCC_ENDPOINT = "/mcc";

    public Response getMcc(String token) {
        return given().header("X-MOCO-MPS-TOKEN", token)
                .when().get(BaseClass.BASE_URL + MCC_ENDPOINT);
    }

    public Response getMccWithoutToken() {
        return given()
                .when().get(BaseClass.BASE_URL + MCC_ENDPOINT);
    }
}