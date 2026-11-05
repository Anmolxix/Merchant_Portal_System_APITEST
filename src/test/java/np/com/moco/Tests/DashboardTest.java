package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.OperatorLoginClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DashboardTest extends BaseClass {

    private final OperatorLoginClient client = new OperatorLoginClient();

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    // =============== GET /merchant/dashboard ===============

    @Test(priority = 1)
    public void dashboardWithValidTokenTest() {
        Response response = client.getDashboard(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertFieldsNotEmpty(response, "totalTxnCount", "totalTxnAmount");
    }

    @Test(priority = 2)
    public void dashboardWithoutTokenTest() {
        Response response = client.getDashboardWithoutToken();

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 3)
    public void dashboardWithInvalidTokenTest() {
        Response response = client.getDashboard("this.is.not.a.valid.token");

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 4)
    public void dashboardWithEmptyTokenTest() {
        Response response = client.getDashboard("");

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 5)
    public void dashboardWithTamperedTokenTest() {
        Response response = client.getDashboard(tamper(getAuthToken()));

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 6)
    public void dashboardContentTypeTest() {
        Response response = client.getDashboard(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(response.getContentType().contains("application/json"),
                "Unexpected Content-Type: " + response.getContentType());
    }

    @Test(priority = 7)
    public void dashboardResponseTimeTest() {
        Response response = client.getDashboard(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(response.getTime() < 5000, "Response too slow: " + response.getTime() + " ms");
    }

    @Test(priority = 8)
    public void dashboardWithOperatorOnlyTokenIsRejectedTest() {
        // The token from step 1 (operator authentication) has no merchant selected yet,
        // so it should not be accepted by merchant-level endpoints.
        // Note: this makes an extra login with the same operator, so it runs last.
        Response login = client.login(ConfigReader.get("id"), Integer.parseInt(ConfigReader.get("pin")));
        ResponseAssertion.assertStatusCode(login, 200);
        String operatorOnlyToken = login.getHeader(ResponseAssertion.TOKEN_HEADER);

        Response response = client.getDashboard(operatorOnlyToken);

        ResponseAssertion.assertStatusCode(response, 401);
    }
}