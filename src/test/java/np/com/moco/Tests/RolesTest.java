package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.RoleClient;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RolesTest extends BaseClass {

    private final RoleClient client = new RoleClient();

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    // =============== GET /role ===============

    @Test(priority = 1)
    public void getRolesTest() {
        Response response = client.getRoles(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);

    }

    @Test(priority = 2)
    public void getRolesWithoutTokenTest() {
        Response response = client.getRolesWithoutToken();

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 3)
    public void getRolesWithInvalidTokenTest() {
        Response response = client.getRoles("this.is.not.a.valid.token");

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 4)
    public void getRolesWithEmptyTokenTest() {
        Response response = client.getRoles("");

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 5)
    public void getRolesWithTamperedTokenTest() {
        Response response = client.getRoles(tamper(getAuthToken()));

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 6)
    public void getRolesContentTypeTest() {
        Response response = client.getRoles(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(response.getContentType().contains("application/json"),
                "Unexpected Content-Type: " + response.getContentType());
    }

    @Test(priority = 7)
    public void getRolesResponseTimeTest() {
        Response response = client.getRoles(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(response.getTime() < 5000, "Response too slow: " + response.getTime() + " ms");
    }

    // =============== GET /permission ===============

    @Test(priority = 8)
    public void getPermissionsTest() {
        Response response = client.getPermissions(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertPermissionList(response);
    }

    @Test(priority = 9)
    public void getPermissionsWithoutTokenTest() {
        Response response = client.getPermissionsWithoutToken();

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 10)
    public void getPermissionsWithInvalidTokenTest() {
        Response response = client.getPermissions("this.is.not.a.valid.token");

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 11)
    public void getPermissionsWithEmptyTokenTest() {
        Response response = client.getPermissions("");

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 12)
    public void getPermissionsWithTamperedTokenTest() {
        Response response = client.getPermissions(tamper(getAuthToken()));

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 13)
    public void getPermissionsContentTypeTest() {
        Response response = client.getPermissions(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(response.getContentType().contains("application/json"),
                "Unexpected Content-Type: " + response.getContentType());
    }

    @Test(priority = 14)
    public void getPermissionsResponseTimeTest() {
        Response response = client.getPermissions(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(response.getTime() < 5000, "Response too slow: " + response.getTime() + " ms");
    }
}