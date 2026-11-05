package np.com.moco.base;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import np.com.moco.Client.OperatorLoginClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import org.testng.annotations.BeforeClass;

public class BaseClass {
    public static final String BASE_URL = ConfigReader.get("baseUrl");

    // Merchant-level token (result of both login steps). Use getAuthToken() to read it.
    public static volatile String AuthToken = null;

    @BeforeClass(alwaysRun = true)
    public void baseSetup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Logs in once (operator auth, then merchant login) and caches the token.
     * Login happens lazily, so tests that don't need a token (e.g. wrong-PIN tests)
     * still run when credentials are bad.
     */
    public static synchronized String getAuthToken() {
        if (AuthToken != null) return AuthToken;

        OperatorLoginClient client = new OperatorLoginClient();
        String id = ConfigReader.get("id");
        int pin = Integer.parseInt(ConfigReader.get("pin"));

        // Step 1: operator authentication -> operator token + merchant list
        Response login = client.login(id, pin);
        if (login.statusCode() != 200) {
            throw new IllegalStateException("Operator login failed: " + login.statusCode() + " " + login.asString());
        }
        String operatorToken = login.getHeader(ResponseAssertion.TOKEN_HEADER);
        if (operatorToken == null || operatorToken.isBlank()) {
            throw new IllegalStateException("Operator login returned no " + ResponseAssertion.TOKEN_HEADER + " header");
        }
        String merchantId = login.jsonPath().getString("[0].id");
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalStateException("Operator login returned no merchant: " + login.asString());
        }

        // Step 2: login to the merchant -> merchant-level token
        Response merchantLogin = client.loginToMerchant(merchantId, operatorToken);
        if (merchantLogin.statusCode() != 200) {
            throw new IllegalStateException("Merchant login failed: " + merchantLogin.statusCode() + " " + merchantLogin.asString());
        }
        String merchantToken = merchantLogin.getHeader(ResponseAssertion.TOKEN_HEADER);
        if (merchantToken == null || merchantToken.isBlank()) {
            throw new IllegalStateException("Merchant login returned no " + ResponseAssertion.TOKEN_HEADER + " header");
        }

        AuthToken = merchantToken;
        return AuthToken;
    }
}