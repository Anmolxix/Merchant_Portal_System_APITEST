package np.com.moco.Tests;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import np.com.moco.Client.MerchantClient;
import np.com.moco.Client.RoleClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MerchantTest extends BaseClass {

    private static final String NO_TOKEN = null;
    private static final String REASON = "{\"reason\":\"automated api test\"}";
    private static final String UNKNOWN_OPERATOR_ID = "9800000000";
    private static final String CREATED_NAME = "Api Test Operator";
    private static final String UPDATED_NAME = "Api Test Operator Updated";

    private final MerchantClient client = new MerchantClient();

    // test data, fetched lazily so a missing config key doesn't stop the whole class
    private String roleId;
    private String outletId;

    // ---------------- helpers ----------------

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    /** The throwaway operator id. Refuses to run if it is the same as the real login operator. */
    private String testOperatorId() {
        String testId = ConfigReader.get("test.operator.id");
        Assert.assertNotEquals(testId, ConfigReader.get("id"),
                "test.operator.id must NOT be the operator you log in with");
        return testId;
    }

    private String roleId() {
        if (roleId == null) {
            Response roles = new RoleClient().getRoles(getAuthToken());
            ResponseAssertion.assertStatusCode(roles, 200);
            roleId = roles.jsonPath().getString("[1].id");
        }
        return roleId;
    }

    private String outletId() {
        if (outletId == null) {
            Response outlets = client.getOutlets(getAuthToken());
            ResponseAssertion.assertStatusCode(outlets, 200);
            outletId = outlets.jsonPath().getString("[0].id");
        }
        return outletId;
    }

    private String operatorBody(String id, String name, String role, String outlet) {
        return String.format(
                "{\"id\":%s,\"name\":\"%s\",\"role\":{\"id\":%s},\"merchant\":{\"outlets\":[{\"id\":\"%s\"}]}}",
                id, name, role, outlet);
    }

    private String validOperatorBody(String name) {
        return operatorBody(testOperatorId(), name, roleId(), outletId());
    }

    // =====================================================================
    //  GET /merchant
    // =====================================================================

    @Test(priority = 1)
    public void getMerchantDetailsTest() {
        Response response = client.getMerchant(getAuthToken());

        ResponseAssertion.assertMerchantDetail(response);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertFieldsNotEmpty(response,
                "businessDetail.doingBusinessAs",
                "businessDetail.registeredBusinessName",
                "settlementInformation.bankName",
                "settlementInformation.accountNumber",
                "settlementInformation.accountName");
    }

    @Test(priority = 2)
    public void getMerchantDetailsWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getMerchant(NO_TOKEN));
    }

    @Test(priority = 3)
    public void getMerchantDetailsWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getMerchant("this.is.not.a.valid.token"));
    }

    @Test(priority = 4)
    public void getMerchantDetailsWithEmptyTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getMerchant(""));
    }

    @Test(priority = 5)
    public void getMerchantDetailsWithTamperedTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getMerchant(tamper(getAuthToken())));
    }

    // =====================================================================
    //  PUT /merchant  (sends a change REQUEST to the acquirer)
    // =====================================================================

    @Test(priority = 6)
    public void updateMerchantWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.updateMerchant(NO_TOKEN, "{}"));
    }

    @Test(priority = 7)
    public void updateMerchantWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(client.updateMerchant("this.is.not.a.valid.token", "{}"));
    }

    @Test(priority = 8)
    public void updateMerchantWithMalformedJsonTest() {
        Response response = client.updateMerchant(getAuthToken(), "this is not json");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    /**
     * Disabled on purpose: a successful call submits a real merchant information change
     * request. Turn it on only against an environment where that is harmless.
     * It sends back the merchant's CURRENT details unchanged.
     */
    @Test(priority = 9, enabled = false)
    public void updateMerchantWithCurrentDetailsSendsRequestTest() {
        Response current = client.getMerchant(getAuthToken());
        ResponseAssertion.assertStatusCode(current, 200);
        JsonPath j = current.jsonPath();

        List<Map<String, Object>> interchanges = new ArrayList<>();
        for (Object interchangeId : j.getList("businessDetail.interchanges.id")) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", interchangeId);
            interchanges.add(item);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("doingBusinessAs", j.getString("businessDetail.doingBusinessAs"));
        body.put("registeredBusinessName", j.getString("businessDetail.registeredBusinessName"));
        body.put("registrationNumber", j.getString("businessDetail.registrationNumber"));
        body.put("registeredWith", j.getString("businessDetail.registeredWith"));
        body.put("registrationDate", j.getString("businessDetail.registrationDate"));
        body.put("pan", j.getString("businessDetail.pan"));
        body.put("settlementType", j.getString("businessDetail.settlementType"));
        body.put("establishmentType", j.getString("businessDetail.establishmentType"));
        body.put("interchanges", interchanges);
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

        Response response = client.updateMerchant(getAuthToken(), body);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
    }

    // =====================================================================
    //  GET /merchant/acquirer/interchange
    // =====================================================================

    @Test(priority = 10)
    public void getInterchangesTest() {
        Response response = client.getInterchanges(getAuthToken());

        ResponseAssertion.assertInterchangeList(response);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "id", "name");
    }

    @Test(priority = 11)
    public void getInterchangesWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getInterchanges(NO_TOKEN));
    }

    @Test(priority = 12)
    public void getInterchangesWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getInterchanges("this.is.not.a.valid.token"));
    }

    // =====================================================================
    //  GET /merchant/operator  (list + search)
    // =====================================================================

    @Test(priority = 13)
    public void getOperatorsTest() {
        Response response = client.getOperators(getAuthToken());

        ResponseAssertion.assertOperatorList(response);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "id", "name", "status", "role.id", "role.name");
    }

    @Test(priority = 14)
    public void getOperatorsWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getOperators(NO_TOKEN));
    }

    @Test(priority = 15)
    public void getOperatorsWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getOperators("this.is.not.a.valid.token"));
    }

    @Test(priority = 16)
    public void searchOperatorsByStatusEnabledTest() {
        Response response = client.getOperators(getAuthToken(), Map.of("status", "enabled"));

        ResponseAssertion.assertOperatorSearchResult(response);
        ResponseAssertion.assertAllItemsFieldEquals(response, "$", "status", "enabled");
    }

    @Test(priority = 17)
    public void searchOperatorsByStatusDisabledTest() {
        Response response = client.getOperators(getAuthToken(), Map.of("status", "disabled"));

        ResponseAssertion.assertOperatorSearchResult(response);
        ResponseAssertion.assertAllItemsFieldEquals(response, "$", "status", "disabled");
    }

    @Test(priority = 18)
    public void searchOperatorsByRoleTest() {
        Response all = client.getOperators(getAuthToken());
        ResponseAssertion.assertStatusCode(all, 200);
        String role = all.jsonPath().getString("[0].role.id");

        Response response = client.getOperators(getAuthToken(), Map.of("roleId", role));

        ResponseAssertion.assertOperatorSearchResult(response);
        ResponseAssertion.assertJsonListNotEmpty(response, "$");
        ResponseAssertion.assertAllItemsFieldEquals(response, "$", "role.id", role);
    }

    @Test(priority = 19)
    public void searchOperatorsByOutletTest() {
        Response response = client.getOperators(getAuthToken(), Map.of("outletId", outletId()));

        ResponseAssertion.assertOperatorSearchResult(response);
    }

    @Test(priority = 20)
    public void searchOperatorsByNameTest() {
        Response all = client.getOperators(getAuthToken());
        ResponseAssertion.assertStatusCode(all, 200);
        String name = all.jsonPath().getString("[0].name");

        Response response = client.getOperators(getAuthToken(), Map.of("name", name));

        ResponseAssertion.assertOperatorSearchResult(response);
        ResponseAssertion.assertJsonListNotEmpty(response, "$");
        ResponseAssertion.assertAllItemsFieldContains(response, "$", "name", name);
    }

    @Test(priority = 21)
    public void searchOperatorsWithUnknownNameTest() {
        Response response = client.getOperators(getAuthToken(), Map.of("name", "zzzz-no-such-operator-zzzz"));

        // the swagger doesn't say whether "no match" is an empty 200 or a 404
        ResponseAssertion.assertStatusCodeIn(response, 200, 404);
        if (response.getStatusCode() == 200) {
            Assert.assertTrue(response.jsonPath().getList("$").isEmpty(),
                    "Expected an empty list for an unknown name | body: " + response.asString());
        }
    }

    // =====================================================================
    //  Operator lifecycle: add -> view -> update -> disable -> enable -> delete
    // =====================================================================

    @Test(priority = 30)
    public void addOperatorTest() {
        Response response = client.addOperator(getAuthToken(), validOperatorBody(CREATED_NAME));

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
    }

    @Test(priority = 31, dependsOnMethods = "addOperatorTest")
    public void addDuplicateOperatorTest() {
        Response response = client.addOperator(getAuthToken(), validOperatorBody(CREATED_NAME));

        ResponseAssertion.assertConflict(response);
    }

    @Test(priority = 32, dependsOnMethods = "addOperatorTest")
    public void viewCreatedOperatorTest() {
        Response response = client.getOperator(getAuthToken(), testOperatorId());

        ResponseAssertion.assertOperatorModel(response);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertFieldsNotEmpty(response, "id", "name", "status", "role.id", "role.name");
        Assert.assertEquals(response.jsonPath().getString("id"), testOperatorId(), "Wrong operator returned");
        Assert.assertEquals(response.jsonPath().getString("name"), CREATED_NAME, "Wrong operator name");
    }

    @Test(priority = 33, dependsOnMethods = "viewCreatedOperatorTest")
    public void createdOperatorAppearsInListTest() {
        Response response = client.getOperators(getAuthToken(), Map.of("name", CREATED_NAME));

        ResponseAssertion.assertOperatorSearchResult(response);
        List<String> ids = response.jsonPath().getList("id").stream().map(String::valueOf).toList();
        Assert.assertTrue(ids.contains(testOperatorId()),
                "Created operator " + testOperatorId() + " not found in search results: " + ids);
    }

    @Test(priority = 34, dependsOnMethods = "createdOperatorAppearsInListTest")
    public void updateOperatorTest() {
        Response response = client.updateOperator(getAuthToken(), testOperatorId(), validOperatorBody(UPDATED_NAME));

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);

        Response after = client.getOperator(getAuthToken(), testOperatorId());
        ResponseAssertion.assertStatusCode(after, 200);
        Assert.assertEquals(after.jsonPath().getString("name"), UPDATED_NAME, "Name was not updated");
    }

    @Test(priority = 35, dependsOnMethods = "updateOperatorTest")
    public void disableOperatorTest() {
        Response response = client.updateOperatorStatus(getAuthToken(), testOperatorId(), "disabled", REASON);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);

        Response after = client.getOperator(getAuthToken(), testOperatorId());
        ResponseAssertion.assertStatusCode(after, 200);
        // top-level "status" is the account state (e.g. "inactive" until activated);
        // enabled/disabled is reported in merchant.operatorStatus
        Assert.assertEquals(after.jsonPath().getString("merchant.operatorStatus"), "disabled", "Operator was not disabled");
    }

    @Test(priority = 36, dependsOnMethods = "disableOperatorTest")
    public void enableOperatorTest() {
        Response response = client.updateOperatorStatus(getAuthToken(), testOperatorId(), "enabled", REASON);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);

        Response after = client.getOperator(getAuthToken(), testOperatorId());
        ResponseAssertion.assertStatusCode(after, 200);
        Assert.assertEquals(after.jsonPath().getString("merchant.operatorStatus"), "enabled", "Operator was not enabled");
    }

    @Test(priority = 37, dependsOnMethods = "enableOperatorTest")
    public void deleteOperatorTest() {
        Response response = client.deleteOperator(getAuthToken(), testOperatorId(), REASON);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
    }

    @Test(priority = 38, dependsOnMethods = "deleteOperatorTest")
    public void viewDeletedOperatorTest() {
        ResponseAssertion.assertNotFound(client.getOperator(getAuthToken(), testOperatorId()));
    }

    /** Safety net: removes the throwaway operator if a test above failed half way. */
    @AfterClass(alwaysRun = true)
    public void cleanUpCreatedOperator() {
        try {
            client.deleteOperator(getAuthToken(), testOperatorId(), REASON);
        } catch (Throwable ignored) {
            // nothing to clean up, or cleanup not possible
        }
    }

    // =====================================================================
    //  POST /merchant/operator  (negative)
    // =====================================================================

    @Test(priority = 40)
    public void addOperatorWithoutTokenTest() {
        Response response = client.addOperator(NO_TOKEN, validOperatorBody(CREATED_NAME));

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 41)
    public void addOperatorWithInvalidTokenTest() {
        Response response = client.addOperator("this.is.not.a.valid.token", validOperatorBody(CREATED_NAME));

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 42)
    public void addOperatorWithoutNameTest() {
        String body = String.format(
                "{\"id\":%s,\"role\":{\"id\":%s},\"merchant\":{\"outlets\":[{\"id\":\"%s\"}]}}",
                testOperatorId(), roleId(), outletId());

        Response response = client.addOperator(getAuthToken(), body);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 43)
    public void addOperatorWithInvalidIdTest() {
        String body = String.format(
                "{\"id\":\"abc\",\"name\":\"%s\",\"role\":{\"id\":%s},\"merchant\":{\"outlets\":[{\"id\":\"%s\"}]}}",
                CREATED_NAME, roleId(), outletId());

        Response response = client.addOperator(getAuthToken(), body);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 44)
    public void addOperatorWithUnknownRoleTest() {
        Response response = client.addOperator(getAuthToken(),
                operatorBody(testOperatorId(), CREATED_NAME, "999999", outletId()));

        ResponseAssertion.assertStatusCodeIn(response, 400, 404, 422);
    }

    // =====================================================================
    //  GET /merchant/operator/{id}  (negative)
    // =====================================================================

    @Test(priority = 50)
    public void viewOperatorWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getOperator(NO_TOKEN, UNKNOWN_OPERATOR_ID));
    }

    @Test(priority = 51)
    public void viewUnknownOperatorTest() {
        ResponseAssertion.assertNotFound(client.getOperator(getAuthToken(), UNKNOWN_OPERATOR_ID));
    }

    @Test(priority = 52)
    public void viewOperatorWithInvalidIdTest() {
        Response response = client.getOperator(getAuthToken(), "abc");

        ResponseAssertion.assertStatusCodeIn(response, 400, 404, 422);
    }

    // =====================================================================
    //  PUT /merchant/operator/{id}  (negative)
    // =====================================================================

    @Test(priority = 53)
    public void updateOperatorWithoutTokenTest() {
        Response response = client.updateOperator(NO_TOKEN, UNKNOWN_OPERATOR_ID,
                operatorBody(UNKNOWN_OPERATOR_ID, UPDATED_NAME, roleId(), outletId()));

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 54)
    public void updateUnknownOperatorTest() {
        Response response = client.updateOperator(getAuthToken(), UNKNOWN_OPERATOR_ID,
                operatorBody(UNKNOWN_OPERATOR_ID, UPDATED_NAME, roleId(), outletId()));

        ResponseAssertion.assertNotFound(response);
    }

    // =====================================================================
    //  PUT /merchant/operator/{id}/{status}  (negative)
    // =====================================================================

    @Test(priority = 55)
    public void updateOperatorStatusWithoutTokenTest() {
        Response response = client.updateOperatorStatus(NO_TOKEN, UNKNOWN_OPERATOR_ID, "disabled", REASON);

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 56)
    public void updateStatusOfUnknownOperatorTest() {
        Response response = client.updateOperatorStatus(getAuthToken(), UNKNOWN_OPERATOR_ID, "disabled", REASON);

        ResponseAssertion.assertNotFound(response);
    }

    @Test(priority = 57)
    public void updateOperatorStatusWithInvalidValueTest() {
        Response response = client.updateOperatorStatus(getAuthToken(), UNKNOWN_OPERATOR_ID, "not-a-status", REASON);

        ResponseAssertion.assertStatusCodeIn(response, 400, 404, 422);
    }

    // =====================================================================
    //  DELETE /merchant/operator/{id}  (negative)
    // =====================================================================

    @Test(priority = 58)
    public void deleteOperatorWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.deleteOperator(NO_TOKEN, UNKNOWN_OPERATOR_ID, REASON));
    }

    @Test(priority = 59)
    public void deleteUnknownOperatorTest() {
        ResponseAssertion.assertNotFound(client.deleteOperator(getAuthToken(), UNKNOWN_OPERATOR_ID, REASON));
    }
}