package np.com.moco.Client;

import io.restassured.response.Response;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;

public class AddressClient {

    public static final String MUNICIPALITY_ENDPOINT = "/municipality";

    public Response getMunicipalities(String token) {
        return given().header("X-MOCO-MPS-TOKEN", token)
                .log().all()                       // logs the request
                .when().get(BaseClass.BASE_URL + MUNICIPALITY_ENDPOINT)
                .then().log().all()                // logs the response
                .extract().response();
    }

    public Response getMunicipalitiesWithoutToken() {
        return given()
                .when().get(BaseClass.BASE_URL + MUNICIPALITY_ENDPOINT);
    }
}