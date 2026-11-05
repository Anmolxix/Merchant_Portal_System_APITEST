package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.OperatorLoginClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class OperatorLogin extends BaseClass {

    private final OperatorLoginClient client = new OperatorLoginClient();


    private final String id = ConfigReader.get("id");
    private final int pin = Integer.parseInt(ConfigReader.get("pin"));
    private static final String UNREGISTERED_ID = "9800000000";

    /** A PIN with the same number of digits as the real one, but different. */
    private int wrongPin() {
        int length = String.valueOf(pin).length();
        int candidate = Integer.parseInt("1".repeat(length));
        if (candidate == pin) {
            candidate = Integer.parseInt("2".repeat(length));
        }
        return candidate;
    }

    /** Full login (operator, then merchant). Returns a fresh merchant-level token. */
    private String freshMerchantToken() {
        Response login = client.login(id, pin);
        ResponseAssertion.assertStatusCode(login, 200);
        String operatorToken = login.getHeader(ResponseAssertion.TOKEN_HEADER);
        String merchantId = login.jsonPath().getString("[0].id");

        Response merchantLogin = client.loginToMerchant(merchantId, operatorToken);
        ResponseAssertion.assertStatusCode(merchantLogin, 200);
        return merchantLogin.getHeader(ResponseAssertion.TOKEN_HEADER);
    }

    // =============== POST /operator/authentication ===============

    @Test(priority = 1)
    public void validOperatorLoginTest() {
        Response response = client.login(id, pin);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);


    }

    // =============== POST /merchant/{merchantId}/operator/login ===============

    @Test(priority = 2)
    public void merchantLoginReturnsOperatorTest() {
        Response login = client.login(id, pin);
        ResponseAssertion.assertStatusCode(login, 200);
        String operatorToken = login.getHeader(ResponseAssertion.TOKEN_HEADER);
        String merchantId = login.jsonPath().getString("[0].id");

        Response response = client.loginToMerchant(merchantId, operatorToken);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
        // Operator model
        ResponseAssertion.assertFieldsNotEmpty(response,
                "id", "name", "status", "role.id", "role.name", "merchant.id", "merchant.name");
    }

    @Test(priority = 3)
    public void loginWithUnregisteredOperatorTest() {
        Response response = client.login(UNREGISTERED_ID, 1234);

        ResponseAssertion.assertStatusCode(response, 422);
        ResponseAssertion.assertTokenAbsent(response);
    }

    @Test(priority = 4)
    public void loginWithWrongPinTest() {
        Response response = client.login(id, wrongPin());

        ResponseAssertion.assertStatusCode(response, 401);
        ResponseAssertion.assertTokenAbsent(response);

        // Successful login resets the failed-attempt counter
        ResponseAssertion.assertStatusCode(client.login(id, pin), 200);
    }

    @Test(priority = 5)
    public void loginWithoutPinTest() {
        Response response = client.loginWithoutPin(id);

        ResponseAssertion.assertStatusCode(response, 400);
    }

    @Test(priority = 6)
    public void loginWithoutIdTest() {
        Response response = client.loginWithoutId(pin);

        ResponseAssertion.assertStatusCode(response, 400);
    }

    @Test(priority = 7)
    public void loginWithoutCredentialsTest() {
        Response response = client.loginWithoutCredentials();

        ResponseAssertion.assertStatusCode(response, 400);
    }

    @Test(priority = 8)
    public void loginWithLettersInsteadOfNumbersTest() {
        Response response = client.loginWithInvalidValues("abc", "xyz");

        ResponseAssertion.assertStatusCode(response, 422);
        ResponseAssertion.assertTokenAbsent(response);
    }

    // =============== dashboard (used here to prove the token works) ===============

    @Test(priority = 9)
    public void dashboardWithTokenTest() {
        Response response = client.getDashboard(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertFieldsNotEmpty(response, "totalTxnCount", "totalTxnAmount");
    }

    @Test(priority = 10)
    public void dashboardWithoutTokenTest() {
        Response response = client.getDashboardWithoutToken();

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 11)
    public void dashboardWithMalformedTokenTest() {
        Response response = client.getDashboard("abc.def.ghi");

        ResponseAssertion.assertStatusCode(response, 401);
    }

    // =============== DELETE /operator/authentication (logout) ===============

    @Test(priority = 12)
    public void logoutTest() {
        String token = freshMerchantToken();

        // Token works before logout
        ResponseAssertion.assertStatusCode(client.getDashboard(token), 200);

        ResponseAssertion.assertStatusCode(client.logout(token), 200);

        // ...and is rejected after logout
        ResponseAssertion.assertStatusCode(client.getDashboard(token), 401);
    }

    @Test(priority = 13)
    public void logoutTwiceTest() {
        String token = freshMerchantToken();

        ResponseAssertion.assertStatusCode(client.logout(token), 200);
        ResponseAssertion.assertStatusCode(client.logout(token), 401);
    }

    // =============== invalid ids ===============

    @DataProvider(name = "invalidIds")
    public Object[][] invalidIds() {
        return new Object[][] {
                {"0"}, {"-1"}, {"12.5"}, {"99999999999999999999"},
                {"@#$%"}, {"' OR '1'='1"}
        };
    }

    @Test(dataProvider = "invalidIds", priority = 14)
    public void loginWithInvalidIdTest(String badId) {
        Response response = client.loginWithInvalidValues(badId, String.valueOf(pin));

        int status = response.getStatusCode();
        Assert.assertTrue(
                status == 400 || status == 404 || status == 422,
                "Unexpected status for id=" + badId + ": " + status);
        ResponseAssertion.assertTokenAbsent(response);
    }
}