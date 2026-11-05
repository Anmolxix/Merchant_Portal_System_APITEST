package np.com.moco.Tests;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import np.com.moco.Client.OutletClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outlet APIs: list outlets, view outlet, update outlet (change request).
 * The outlet id comes from config.properties key "outlet.id".
 */
public class OutletTest extends BaseClass {

    private static final String NO_TOKEN = null;
    private static final String UNKNOWN_OUTLET_ID = "O999999999";

    private final OutletClient client = new OutletClient();

    /** An outlet of the merchant you log in to, e.g. O000006165. */
    private String outletId() {
        return ConfigReader.get("outlet.id");
    }

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    // =====================================================================
    //  GET /merchant/outlet  (list)
    // =====================================================================

    @Test(priority = 1)
    public void getOutletsTest() {
        Response response = client.getOutlets(getAuthToken());

        ResponseAssertion.assertOutletList(response);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "id", "name");
    }

    @Test(priority = 2)
    public void getOutletsWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getOutlets(NO_TOKEN));
    }

    @Test(priority = 3)
    public void getOutletsWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getOutlets("this.is.not.a.valid.token"));
    }

    @Test(priority = 4)
    public void getOutletsWithTamperedTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getOutlets(tamper(getAuthToken())));
    }

    @Test(priority = 5)
    public void getOutletsContentTypeTest() {
        Response response = client.getOutlets(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertContentTypeContains(response, "application/json");
    }

    @Test(priority = 6)
    public void getOutletsResponseTimeTest() {
        Response response = client.getOutlets(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertResponseTimeBelow(response, 5000);
    }

    // =====================================================================
    //  GET /outlet/{outletId}  (view)
    // =====================================================================

    @Test(priority = 10)
    public void getOutletDetailTest() {
        Response response = client.getOutlet(getAuthToken(), outletId());

        ResponseAssertion.assertOutletDetail(response);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertFieldsNotEmpty(response,
                "businessDetail.doingBusinessAs",
                "businessDetail.mcc.id",
                "businessDetail.mcc.name",
                "businessDetail.txnCurrency"
                );
    }

    @Test(priority = 11)
    public void getOutletDetailWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getOutlet(NO_TOKEN, outletId()));
    }

    @Test(priority = 12)
    public void getOutletDetailWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getOutlet("this.is.not.a.valid.token", outletId()));
    }

    @Test(priority = 13)
    public void getUnknownOutletTest() {
        Response response = client.getOutlet(getAuthToken(), UNKNOWN_OUTLET_ID);

        // 404 per swagger; 403 if the API treats an outlet of another merchant as forbidden
        ResponseAssertion.assertStatusCodeIn(response, 403, 404);
    }

    @Test(priority = 14)
    public void getOutletWithInvalidIdTest() {
        Response response = client.getOutlet(getAuthToken(), "abc");

        ResponseAssertion.assertStatusCodeIn(response, 400, 403, 404, 422);
    }

    @Test(priority = 15)
    public void getOutletDetailResponseTimeTest() {
        Response response = client.getOutlet(getAuthToken(), outletId());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertResponseTimeBelow(response, 5000);
    }

    // =====================================================================
    //  PUT /outlet/{outletId}  (sends a change REQUEST)
    // =====================================================================

    @Test(priority = 20)
    public void updateOutletWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.updateOutlet(NO_TOKEN, outletId(), "{}"));
    }

    @Test(priority = 21)
    public void updateOutletWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.updateOutlet("this.is.not.a.valid.token", outletId(), "{}"));
    }

    @Test(priority = 22)
    public void updateOutletWithMalformedJsonTest() {
        Response response = client.updateOutlet(getAuthToken(), outletId(), "this is not json");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 23)
    public void updateUnknownOutletTest() {
        Response response = client.updateOutlet(getAuthToken(), UNKNOWN_OUTLET_ID, "{}");

        ResponseAssertion.assertStatusCodeIn(response, 403, 404);
    }


    @Test(priority = 24, enabled = true)
    public void updateOutletWithCurrentDetailsSendsRequestTest() {
        Response current = client.getOutlet(getAuthToken(), outletId());
        ResponseAssertion.assertStatusCode(current, 200);
        JsonPath j = current.jsonPath();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("doingBusinessAs", j.getString("businessDetail.doingBusinessAs"));
        body.put("mcc", j.getString("businessDetail.mcc.id"));
        body.put("averageTxnAmount", j.getString("businessDetail.averageTxnAmount"));
        body.put("txnCurrency", j.getString("businessDetail.txnCurrency"));
        body.put("province", j.getString("businessAddress.province"));
        body.put("district", j.getString("businessAddress.district"));
        body.put("municipality", j.getString("businessAddress.municipality.id"));
        body.put("ward", j.getString("businessAddress.ward"));
        body.put("street", j.getString("businessAddress.street"));
        body.put("houseNumber", j.getString("businessAddress.houseNumber"));
        body.put("landline", j.getString("businessAddress.landline"));
        body.put("email", j.getString("businessAddress.email"));
        body.put("mobile", j.getString("businessAddress.mobile"));
        body.put("location", j.getString("businessAddress.location"));

        Response response = client.updateOutlet(getAuthToken(), outletId(), body);

        ResponseAssertion.assertOutletUpdateRequestSent(response);
    }
}