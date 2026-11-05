package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.TerminalClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;


public class TerminalTest extends BaseClass {

    private static final String NO_TOKEN = null;
    private static final String UNKNOWN_OUTLET_ID = "O999999999";
    private static final String UNKNOWN_TERMINAL_ID = "T999999999";
    private static final String CREATED_NAME = "Api Test Terminal";
    private static final String UPDATED_NAME = "Api Test Terminal Updated";
    private static final String REASON = "{\"reason\":\"automated api test\"}";

    private final TerminalClient client = new TerminalClient();

    // set by the lifecycle tests
    private String createdTerminalId;

    // ---------------- values from config ----------------

    /** An outlet that already has at least one terminal, e.g. O000006165. */
    private String outletId() {
        return ConfigReader.get("outlet.id");
    }

    private String sharedKey() {
        return ConfigReader.get("terminal.sharedKey");
    }

    private String notificationMethod() {
        return ConfigReader.get("terminal.notificationMethod");
    }

    private String callbackUrl() {
        return ConfigReader.get("terminal.callbackUrl");
    }

    // ---------------- helpers ----------------

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    private String terminalBody(String name) {
        return String.format(
                "{\"name\":\"%s\",\"sharedKey\":\"%s\",\"notificationMethod\":\"%s\",\"callbackUrl\":\"%s\"}",
                name, sharedKey(), notificationMethod(), callbackUrl());
    }

    /** Looks a terminal up by name in the outlet's terminal list. Returns null if it isn't there. */
    private String findTerminalIdByName(String name) {
        Response list = client.getTerminals(getAuthToken(), outletId());
        if (list.getStatusCode() != 200) {
            return null;
        }
        return list.jsonPath().getString("find { it.name == '" + name + "' }.terminalId");
    }

    // =====================================================================
    //  GET /outlet/{outletId}/terminal  (list)
    // =====================================================================

    @Test(priority = 1)
    public void getTerminalsTest() {
        Response response = client.getTerminals(getAuthToken(), outletId());

        ResponseAssertion.assertTerminalList(response);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "terminalId", "name");
    }

    @Test(priority = 2)
    public void getTerminalsWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getTerminals(NO_TOKEN, outletId()));
    }

    @Test(priority = 3)
    public void getTerminalsWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getTerminals("this.is.not.a.valid.token", outletId()));
    }

    @Test(priority = 4)
    public void getTerminalsWithTamperedTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getTerminals(tamper(getAuthToken()), outletId()));
    }

    @Test(priority = 5)
    public void getTerminalsOfUnknownOutletTest() {
        Response response = client.getTerminals(getAuthToken(), UNKNOWN_OUTLET_ID);

        ResponseAssertion.assertStatusCodeIn(response, 403, 404);
    }

    // =====================================================================
    //  GET /terminal  (all terminals, optional outlet filter)
    // =====================================================================

    @Test(priority = 10)
    public void getAllTerminalsTest() {
        Response response = client.getAllTerminals(getAuthToken(), null);

        ResponseAssertion.assertTerminalList(response);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "terminalId", "name");
    }

    @Test(priority = 11)
    public void getAllTerminalsFilteredByOutletTest() {
        Response response = client.getAllTerminals(getAuthToken(), outletId());

        ResponseAssertion.assertTerminalList(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "terminalId", "name");
    }

    @Test(priority = 12)
    public void getAllTerminalsWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getAllTerminals(NO_TOKEN, null));
    }

    @Test(priority = 13)
    public void getAllTerminalsWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getAllTerminals("this.is.not.a.valid.token", null));
    }

    @Test(priority = 14)
    public void getAllTerminalsForUnknownOutletTest() {
        Response response = client.getAllTerminals(getAuthToken(), UNKNOWN_OUTLET_ID);

        ResponseAssertion.assertStatusCodeIn(response, 403, 404);
    }

    // =====================================================================
    //  Terminal lifecycle: add -> find -> view -> update -> delete -> view again
    // =====================================================================

    @Test(priority = 30)
    public void addTerminalTest() {
        Response response = client.addTerminal(getAuthToken(), outletId(), terminalBody(CREATED_NAME));

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
    }

    @Test(priority = 31, dependsOnMethods = "addTerminalTest")
    public void createdTerminalAppearsInListTest() {
        // the add call only returns text, so the new terminal's id is found by its name
        createdTerminalId = findTerminalIdByName(CREATED_NAME);

        Assert.assertNotNull(createdTerminalId, "Created terminal '" + CREATED_NAME + "' is not in the list");
    }

    @Test(priority = 32, dependsOnMethods = "createdTerminalAppearsInListTest")
    public void viewCreatedTerminalTest() {
        Response response = client.getTerminal(getAuthToken(), outletId(), createdTerminalId);

        ResponseAssertion.assertTerminalDetail(response, createdTerminalId);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertFieldsNotEmpty(response, "terminalId", "name");
        Assert.assertEquals(response.jsonPath().getString("name"), CREATED_NAME, "Wrong terminal name");
    }

    @Test(priority = 33, dependsOnMethods = "viewCreatedTerminalTest")
    public void updateTerminalTest() {
        Response response = client.updateTerminal(getAuthToken(), outletId(), createdTerminalId,
                terminalBody(UPDATED_NAME));

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);

        Response after = client.getTerminal(getAuthToken(), outletId(), createdTerminalId);
        ResponseAssertion.assertStatusCode(after, 200);
        Assert.assertEquals(after.jsonPath().getString("name"), UPDATED_NAME, "Name was not updated");
    }

    @Test(priority = 34, dependsOnMethods = "updateTerminalTest")
    public void deleteTerminalTest() {
        Response response = client.deleteTerminal(getAuthToken(), outletId(), createdTerminalId, REASON);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
    }

    @Test(priority = 35, dependsOnMethods = "deleteTerminalTest")
    public void viewDeletedTerminalTest() {
        Response response = client.getTerminal(getAuthToken(), outletId(), createdTerminalId);

        ResponseAssertion.assertNotFound(response);
    }

    /** Safety net: removes the throwaway terminal if a test above failed half way. */
    @AfterClass(alwaysRun = true)
    public void cleanUpCreatedTerminal() {
        try {
            String leftover = findTerminalIdByName(CREATED_NAME);
            if (leftover != null) {
                client.deleteTerminal(getAuthToken(), outletId(), leftover, REASON);
            }
            String leftoverUpdated = findTerminalIdByName(UPDATED_NAME);
            if (leftoverUpdated != null) {
                client.deleteTerminal(getAuthToken(), outletId(), leftoverUpdated, REASON);
            }
        } catch (Throwable ignored) {
            // nothing to clean up, or cleanup not possible
        }
    }

    // =====================================================================
    //  POST /outlet/{outletId}/terminal  (negative)
    // =====================================================================

    @Test(priority = 40)
    public void addTerminalWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.addTerminal(NO_TOKEN, outletId(), terminalBody(CREATED_NAME)));
    }

    @Test(priority = 41)
    public void addTerminalWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.addTerminal("this.is.not.a.valid.token", outletId(), terminalBody(CREATED_NAME)));
    }

    @Test(priority = 42)
    public void addTerminalWithoutNameTest() {
        String body = String.format(
                "{\"sharedKey\":\"%s\",\"notificationMethod\":\"%s\",\"callbackUrl\":\"%s\"}",
                sharedKey(), notificationMethod(), callbackUrl());

        Response response = client.addTerminal(getAuthToken(), outletId(), body);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 43)
    public void addTerminalWithMalformedJsonTest() {
        Response response = client.addTerminal(getAuthToken(), outletId(), "this is not json");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 44)
    public void addTerminalToUnknownOutletTest() {
        Response response = client.addTerminal(getAuthToken(), UNKNOWN_OUTLET_ID, terminalBody(CREATED_NAME));

        ResponseAssertion.assertStatusCodeIn(response, 403, 404);
    }

    // =====================================================================
    //  GET / PUT / DELETE on one terminal  (negative)
    // =====================================================================

    @Test(priority = 50)
    public void viewTerminalWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getTerminal(NO_TOKEN, outletId(), UNKNOWN_TERMINAL_ID));
    }

    @Test(priority = 51)
    public void viewUnknownTerminalTest() {
        ResponseAssertion.assertNotFound(client.getTerminal(getAuthToken(), outletId(), UNKNOWN_TERMINAL_ID));
    }

    @Test(priority = 52)
    public void updateTerminalWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.updateTerminal(NO_TOKEN, outletId(), UNKNOWN_TERMINAL_ID, terminalBody(UPDATED_NAME)));
    }

    @Test(priority = 53)
    public void updateUnknownTerminalTest() {
        Response response = client.updateTerminal(getAuthToken(), outletId(), UNKNOWN_TERMINAL_ID,
                terminalBody(UPDATED_NAME));

        ResponseAssertion.assertNotFound(response);
    }

    @Test(priority = 54)
    public void deleteTerminalWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.deleteTerminal(NO_TOKEN, outletId(), UNKNOWN_TERMINAL_ID, REASON));
    }

    @Test(priority = 55)
    public void deleteUnknownTerminalTest() {
        Response response = client.deleteTerminal(getAuthToken(), outletId(), UNKNOWN_TERMINAL_ID, REASON);

        ResponseAssertion.assertNotFound(response);
    }
}