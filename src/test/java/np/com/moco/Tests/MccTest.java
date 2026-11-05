package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.MccClient;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MccTest extends BaseClass {

    private final MccClient client = new MccClient();

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    // =============== GET /mcc ===============

    @Test(priority = 1)
    public void getMccTest() {
        Response response = client.getMcc(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "id", "name");
    }

    @Test(priority = 2)
    public void getMccWithoutTokenTest() {
        Response response = client.getMccWithoutToken();

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 3)
    public void getMccWithInvalidTokenTest() {
        Response response = client.getMcc("this.is.not.a.valid.token");

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 4)
    public void getMccWithEmptyTokenTest() {
        Response response = client.getMcc("");

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 5)
    public void getMccWithTamperedTokenTest() {
        Response response = client.getMcc(tamper(getAuthToken()));

        ResponseAssertion.assertStatusCode(response, 401);
    }

    @Test(priority = 6)
    public void getMccContentTypeTest() {
        Response response = client.getMcc(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(response.getContentType().contains("application/json"),
                "Unexpected Content-Type: " + response.getContentType());
    }

    @Test(priority = 7)
    public void getMccResponseTimeTest() {
        Response response = client.getMcc(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(response.getTime() < 5000, "Response too slow: " + response.getTime() + " ms");
    }
}