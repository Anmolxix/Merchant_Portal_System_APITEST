package np.com.moco.Client;

import io.restassured.response.Response;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;

import static io.restassured.RestAssured.given;

public class RoleClient {

    public static final String ROLE_ENDPOINT = "/role";
    public static final String PERMISSION_ENDPOINT = "/permission";

    // One request method does the work; the others just pick the path / token.
    private Response get(String endpoint, String token) {
        return given()
                .header(ResponseAssertion.TOKEN_HEADER, token)
                .when()
                .get(BaseClass.BASE_URL + endpoint);
    }

    private Response getWithoutToken(String endpoint) {
        return given()
                .when()
                .get(BaseClass.BASE_URL + endpoint);
    }

    // =============== Roles (JSON) ===============

    public Response getRoles(String token) {
        return get(ROLE_ENDPOINT, token);
    }

    public Response getRolesWithoutToken() {
        return getWithoutToken(ROLE_ENDPOINT);
    }

    // =============== Permissions (JSON) ===============

    public Response getPermissions(String token) {
        return get(PERMISSION_ENDPOINT, token);
    }

    public Response getPermissionsWithoutToken() {
        return getWithoutToken(PERMISSION_ENDPOINT);
    }
}