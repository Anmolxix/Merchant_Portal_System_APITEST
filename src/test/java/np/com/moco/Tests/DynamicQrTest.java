package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.DynamicQrClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Dynamic QR: checks that the API returns a PNG image.
 * The outlet id comes from config.properties key "outlet.id".
 *
 * Every QR needs a reference number that has not been used before, so the tests add the
 * current time to it. That is the only generated value.
 */
public class DynamicQrTest extends BaseClass {

    private static final String NO_TOKEN = null;
    private static final String AMOUNT = "10";

    private final DynamicQrClient client = new DynamicQrClient();

    /** An outlet of the merchant you log in to, e.g. O000006165. */
    private String outletId() {
        return ConfigReader.get("outlet.id");
    }

    /** A reference number that has never been used. */
    private String newReference(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    // =====================================================================

    @Test(priority = 1)
    public void dynamicQrReturnsPngTest() {
        Response response = client.getDynamicQr(getAuthToken(), outletId(), AMOUNT, newReference("ApiTestQr"));

        ResponseAssertion.assertDynamicQr(response);   // 200, image/png, bytes start with the PNG signature
    }

    @Test(priority = 2)
    public void dynamicQrImageIsNotEmptyTest() {
        Response response = client.getDynamicQr(getAuthToken(), outletId(), AMOUNT, newReference("ApiTestQrSize"));

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(response.asByteArray().length > 500,
                "The QR image looks too small: " + response.asByteArray().length + " bytes");
    }

    @Test(priority = 3)
    public void dynamicQrWithoutTokenTest() {
        Response response = client.getDynamicQr(NO_TOKEN, outletId(), AMOUNT, newReference("ApiTestQrNoToken"));

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 4)
    public void dynamicQrWithInvalidTokenTest() {
        Response response = client.getDynamicQr("this.is.not.a.valid.token",
                outletId(), AMOUNT, newReference("ApiTestQrBadToken"));

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 5)
    public void dynamicQrWithSameReferenceTwiceTest() {
        String reference = newReference("ApiTestQrDup");

        Response first = client.getDynamicQr(getAuthToken(), outletId(), AMOUNT, reference);
        ResponseAssertion.assertStatusCode(first, 200);

        Response second = client.getDynamicQr(getAuthToken(), outletId(), AMOUNT, reference);
        ResponseAssertion.assertConflict(second);   // 409: QR already generated for this reference
    }
}