package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.ActivityLogClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ActivityLogTest extends BaseClass {

    private static final String NO_TOKEN = null;

    // the swagger spells this "acivityLog" (a typo); the real API returns "activityLog"
    private static final String LIST = "activityLog";

    private final ActivityLogClient client = new ActivityLogClient();

    // ---------------- values from config ----------------

    /** Start of a range (max 30 days) that HAS activity, yyyy-MM-dd. */
    private String startDate() {
        return ConfigReader.get("activity.startDate");
    }

    private String endDate() {
        return ConfigReader.get("activity.endDate");
    }

    /** A section id that has activity in that range. */
    private String sectionId() {
        return ConfigReader.get("activity.sectionId");
    }

    /** A range (max 30 days) with NO activity. */
    private String noActivityStartDate() {
        return ConfigReader.get("activity.noActivityStartDate");
    }

    private String noActivityEndDate() {
        return ConfigReader.get("activity.noActivityEndDate");
    }

    private String longRangeStartDate() {
        return ConfigReader.get("activity.longRangeStartDate");
    }

    private String longRangeEndDate() {
        return ConfigReader.get("activity.longRangeEndDate");
    }

    /** The operator we log in as. */
    private String loginOperatorId() {
        return ConfigReader.get("id");
    }

    // ---------------- helpers ----------------

    /** First 10 records of the valid range. */
    private Response getFirstTenRecords() {
        return client.getActivityLog(getAuthToken(), startDate(), endDate(), null, null, "0", "10");
    }

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    // =====================================================================
    //  Valid request
    // =====================================================================

    @Test(priority = 1)
    public void getActivityLogTest() {
        Response response = getFirstTenRecords();

        ResponseAssertion.assertActivityLog(response);   // 200, JSON, total count, item fields
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertFieldsNotEmpty(response, "totalActivityCount");
        ResponseAssertion.assertListFieldsNotEmpty(response, LIST,
                "operatorId", "timestamp", "description", "permission.section", "permission.operation");
    }

    @Test(priority = 2)
    public void getActivityLogContentTypeTest() {
        Response response = getFirstTenRecords();

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertContentTypeContains(response, "application/json");
    }

    @Test(priority = 3)
    public void getActivityLogResponseTimeTest() {
        Response response = getFirstTenRecords();

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertResponseTimeBelow(response, 5000);
    }

    // =====================================================================
    //  Token problems
    // =====================================================================

    @Test(priority = 4)
    public void withoutTokenTest() {
        Response response = client.getActivityLog(NO_TOKEN, startDate(), endDate(), null, null, "0", "10");

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 5)
    public void withInvalidTokenTest() {
        Response response = client.getActivityLog("this.is.not.a.valid.token",
                startDate(), endDate(), null, null, "0", "10");

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 6)
    public void withTamperedTokenTest() {
        Response response = client.getActivityLog(tamper(getAuthToken()),
                startDate(), endDate(), null, null, "0", "10");

        ResponseAssertion.assertUnauthorized(response);
    }

    // =====================================================================
    //  Missing required parameters
    // =====================================================================

    @Test(priority = 7)
    public void withoutStartDateTest() {
        Response response = client.getActivityLog(getAuthToken(), null, endDate(), null, null, "0", "10");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 8)
    public void withoutEndDateTest() {
        Response response = client.getActivityLog(getAuthToken(), startDate(), null, null, null, "0", "10");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 9)
    public void withoutStartRecordTest() {
        Response response = client.getActivityLog(getAuthToken(), startDate(), endDate(), null, null, null, "10");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 10)
    public void withoutCountTest() {
        Response response = client.getActivityLog(getAuthToken(), startDate(), endDate(), null, null, "0", null);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    // =====================================================================
    //  Invalid values
    // =====================================================================

    @Test(priority = 11)
    public void withInvalidDateFormatTest() {
        Response response = client.getActivityLog(getAuthToken(), "not-a-date", endDate(), null, null, "0", "10");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 12)
    public void withStartDateAfterEndDateTest() {
        // the same two dates as the valid range, swapped
        Response response = client.getActivityLog(getAuthToken(), endDate(), startDate(), null, null, "0", "10");

        // the swagger doesn't say how a reversed range is handled
        ResponseAssertion.assertStatusCodeIn(response, 400, 404, 422);
    }

    @Test(priority = 13)
    public void withRangeLongerThan30DaysTest() {
        Response response = client.getActivityLog(getAuthToken(),
                longRangeStartDate(), longRangeEndDate(), null, null, "0", "10");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 14)
    public void withNonNumericCountTest() {
        Response response = client.getActivityLog(getAuthToken(), startDate(), endDate(), null, null, "0", "abc");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 15)
    public void withNonNumericStartRecordTest() {
        Response response = client.getActivityLog(getAuthToken(), startDate(), endDate(), null, null, "abc", "10");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    // =====================================================================
    //  Paging
    // =====================================================================

    @Test(priority = 16)
    public void countLimitsNumberOfRecordsTest() {
        Response response = client.getActivityLog(getAuthToken(), startDate(), endDate(), null, null, "0", "2");

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertActivityLogCountAtMost(response, 2);
    }

    @Test(priority = 17)
    public void startRecordBeyondTheLastRecordTest() {
        Response response = client.getActivityLog(getAuthToken(), startDate(), endDate(), null, null, "1000000", "10");

        // an empty page (200) or "not found" (404) are both reasonable
        ResponseAssertion.assertStatusCodeIn(response, 200, 404);
        if (response.getStatusCode() == 200) {
            Assert.assertTrue(response.jsonPath().getList(LIST).isEmpty(),
                    "Expected no records past the end | body: " + response.asString());
        }
    }

    // =====================================================================
    //  Filters
    // =====================================================================

    @Test(priority = 18)
    public void filterByOperatorIdTest() {
        Response response = client.getActivityLog(getAuthToken(),
                startDate(), endDate(), loginOperatorId(), null, "0", "10");

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertActivityLogAllOperator(response, loginOperatorId());
    }

    @Test(priority = 19)
    public void filterByUnknownOperatorIdTest() {
        Response response = client.getActivityLog(getAuthToken(),
                startDate(), endDate(), "9800000000", null, "0", "10");

        // nothing for this operator: an empty 200, "not found", or forbidden if the role can't see others
        ResponseAssertion.assertStatusCodeIn(response, 200, 403, 404);
        if (response.getStatusCode() == 200) {
            Assert.assertTrue(response.jsonPath().getList(LIST).isEmpty(),
                    "Expected no records for an unknown operator | body: " + response.asString());
        }
    }

    @Test(priority = 20)
    public void filterBySectionIdTest() {
        Response response = client.getActivityLog(getAuthToken(),
                startDate(), endDate(), null, sectionId(), "0", "10");

        ResponseAssertion.assertStatusCodeIn(response, 200, 404);
        if (response.getStatusCode() == 200) {
            ResponseAssertion.assertActivityLog(response);
        }
    }

    @Test(priority = 21)
    public void filterByNonNumericSectionIdTest() {
        Response response = client.getActivityLog(getAuthToken(),
                startDate(), endDate(), null, "abc", "0", "10");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    // =====================================================================
    //  No activity
    // =====================================================================

    @Test(priority = 22)
    public void rangeWithNoActivityTest() {
        Response response = client.getActivityLog(getAuthToken(),
                noActivityStartDate(), noActivityEndDate(), null, null, "0", "10");

        // the swagger lists 404 "Activity not found"; an empty 200 is also reasonable
        ResponseAssertion.assertStatusCodeIn(response, 200, 404);
        if (response.getStatusCode() == 200) {
            Assert.assertTrue(response.jsonPath().getList(LIST).isEmpty(),
                    "Expected no records for a range with no activity | body: " + response.asString());
        }
    }
}