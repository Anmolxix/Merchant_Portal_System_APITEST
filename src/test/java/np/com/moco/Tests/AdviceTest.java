package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.AdviceClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;


public class AdviceTest extends BaseClass {

    private static final String NO_TOKEN = null;

    private final AdviceClient client = new AdviceClient();

    // ---------------- config values (read when a test runs) ----------------

    /** Start of a date range (max 32 days) that HAS advice reports, yyyy-MM-dd. */
    private String startDate() {
        return ConfigReader.get("advice.startDate");
    }

    /** End of that same range, yyyy-MM-dd. */
    private String endDate() {
        return ConfigReader.get("advice.endDate");
    }

    /** Name of a report that exists in that range (from the GET /merchant/advice response). */
    private String reportName() {
        return ConfigReader.get("advice.reportName");
    }

    /** Date of that report, yyyy-MM-dd. */
    private String reportDate() {
        return ConfigReader.get("advice.reportDate");
    }

    /** A range (max 32 days) with NO advice reports. */
    private String noReportStartDate() {
        return ConfigReader.get("advice.noReportStartDate");
    }

    private String noReportEndDate() {
        return ConfigReader.get("advice.noReportEndDate");
    }

    /** A single date with NO advice report. */
    private String noReportDate() {
        return ConfigReader.get("advice.noReportDate");
    }

    /** A range LONGER than 32 days. */
    private String longRangeStartDate() {
        return ConfigReader.get("advice.longRangeStartDate");
    }

    private String longRangeEndDate() {
        return ConfigReader.get("advice.longRangeEndDate");
    }

    // ---------------- helpers ----------------

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    private String dateOnly(Object value) {
        String text = String.valueOf(value);
        return text.length() > 10 ? text.substring(0, 10) : text;
    }

    // =====================================================================
    //  GET /merchant/advice  (list report dates)
    // =====================================================================

    @Test(priority = 1)
    public void getAdviceListTest() {
        Response response = client.getAdviceList(getAuthToken(), startDate(), endDate());

        ResponseAssertion.assertAdviceList(response);   // 200, JSON, list, date format
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "reportName", "date");
    }

    @Test(priority = 2)
    public void getAdviceListWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getAdviceList(NO_TOKEN, startDate(), endDate()));
    }

    @Test(priority = 3)
    public void getAdviceListWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.getAdviceList("this.is.not.a.valid.token", startDate(), endDate()));
    }

    @Test(priority = 4)
    public void getAdviceListWithTamperedTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.getAdviceList(tamper(getAuthToken()), startDate(), endDate()));
    }

    @Test(priority = 5)
    public void getAdviceListWithoutStartDateTest() {
        Response response = client.getAdviceList(getAuthToken(), null, endDate());

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 6)
    public void getAdviceListWithoutEndDateTest() {
        Response response = client.getAdviceList(getAuthToken(), startDate(), null);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 7)
    public void getAdviceListWithInvalidDateFormatTest() {
        Response response = client.getAdviceList(getAuthToken(), "not-a-date", endDate());

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 8)
    public void getAdviceListWithWrongDateFormatTest() {
        // dd-MM-yyyy instead of yyyy-MM-dd (deliberately wrong)
        Response response = client.getAdviceList(getAuthToken(), "04-11-2026", "04-11-2026");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 9)
    public void getAdviceListStartAfterEndTest() {
        // same dates as the valid range, but swapped
        Response response = client.getAdviceList(getAuthToken(), endDate(), startDate());

        // the swagger doesn't say how a reversed range is handled
        ResponseAssertion.assertStatusCodeIn(response, 400, 404, 422);
    }

    @Test(priority = 10)
    public void getAdviceListForRangeWithNoReportsTest() {
        Response response = client.getAdviceList(getAuthToken(), noReportStartDate(), noReportEndDate());

        // the swagger doesn't say whether "nothing found" is an empty 200 or a 404
        ResponseAssertion.assertStatusCodeIn(response, 200, 404);
        if (response.getStatusCode() == 200) {
            Assert.assertTrue(response.jsonPath().getList("$").isEmpty(),
                    "Expected an empty list for a range with no reports | body: " + response.asString());
        }
    }


    @Test(priority = 11)
    public void getAdviceListWithRangeLongerThan32DaysTest() {
        Response response = client.getAdviceList(getAuthToken(), longRangeStartDate(), longRangeEndDate());

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 12)
    public void getAdviceListDatesAreWithinRangeTest() {
        Response response = client.getAdviceList(getAuthToken(), startDate(), endDate());
        ResponseAssertion.assertStatusCode(response, 200);

        List<Map<String, Object>> items = response.jsonPath().getList("$");
        for (int i = 0; i < items.size(); i++) {
            String date = dateOnly(items.get(i).get("date"));
            Assert.assertTrue(date.compareTo(startDate()) >= 0 && date.compareTo(endDate()) <= 0,
                    "Item " + i + " date " + date + " is outside " + startDate() + " .. " + endDate());
        }
    }

    @Test(priority = 13)
    public void getAdviceListContentTypeTest() {
        Response response = client.getAdviceList(getAuthToken(), startDate(), endDate());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertContentTypeContains(response, "application/json");
    }

    @Test(priority = 14)
    public void getAdviceListResponseTimeTest() {
        Response response = client.getAdviceList(getAuthToken(), startDate(), endDate());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertResponseTimeBelow(response, 5000);
    }

    // =====================================================================
    //  GET /merchant/advice/download  (single report, PDF)
    // =====================================================================

    @Test(priority = 20)
    public void downloadAdvicePdfTest() {
        Response response = client.downloadAdvice(getAuthToken(), reportName(), reportDate());

        ResponseAssertion.assertAdvicePdf(response);   // 200, application/pdf, starts with %PDF
    }

    @Test(priority = 21)
    public void downloadAdviceWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.downloadAdvice(NO_TOKEN, reportName(), reportDate()));
    }

    @Test(priority = 22)
    public void downloadAdviceWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.downloadAdvice("this.is.not.a.valid.token", reportName(), reportDate()));
    }

    @Test(priority = 23)
    public void downloadAdviceWithoutReportNameTest() {
        Response response = client.downloadAdvice(getAuthToken(), null, reportDate());

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 24)
    public void downloadAdviceWithoutDateTest() {
        Response response = client.downloadAdvice(getAuthToken(), reportName(), null);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 25)
    public void downloadAdviceWithInvalidDateFormatTest() {
        Response response = client.downloadAdvice(getAuthToken(), reportName(), "not-a-date");

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 26)
    public void downloadAdviceWithUnknownReportNameTest() {
        Response response = client.downloadAdvice(getAuthToken(), "no-such-report.pdf", reportDate());

        // the API answers 422 "Invalid report name." for a name it doesn't recognise
        ResponseAssertion.assertStatusCodeIn(response, 404, 422);
    }

    @Test(priority = 27)
    public void downloadAdviceForDateWithNoReportTest() {
        Response response = client.downloadAdvice(getAuthToken(), reportName(), noReportDate());

        ResponseAssertion.assertNotFound(response);
    }

    // =====================================================================
    //  GET /merchant/advice/download/zip  (download link for a range)
    // =====================================================================

    @Test(priority = 30)
    public void getAdviceZipLinkTest() {
        Response response = client.getAdviceZipLink(getAuthToken(), startDate(), endDate());

        ResponseAssertion.assertAdviceZipLink(response);   // 200, JSON, downloadLink looks like a URL/path
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertFieldsNotEmpty(response, "downloadLink");
    }

    @Test(priority = 31)
    public void getAdviceZipLinkWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getAdviceZipLink(NO_TOKEN, startDate(), endDate()));
    }

    @Test(priority = 32)
    public void getAdviceZipLinkWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.getAdviceZipLink("this.is.not.a.valid.token", startDate(), endDate()));
    }

    @Test(priority = 33)
    public void getAdviceZipLinkWithoutStartDateTest() {
        Response response = client.getAdviceZipLink(getAuthToken(), null, endDate());

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 34)
    public void getAdviceZipLinkWithoutEndDateTest() {
        Response response = client.getAdviceZipLink(getAuthToken(), startDate(), null);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 35)
    public void getAdviceZipLinkWithInvalidDateFormatTest() {
        Response response = client.getAdviceZipLink(getAuthToken(), "not-a-date", endDate());

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 36)
    public void getAdviceZipLinkForRangeWithNoReportsTest() {
        Response response = client.getAdviceZipLink(getAuthToken(), noReportStartDate(), noReportEndDate());

        ResponseAssertion.assertNotFound(response);
    }


    @Test(priority = 37)
    public void getAdviceZipLinkWithRangeLongerThan32DaysTest() {
        Response response = client.getAdviceZipLink(getAuthToken(), longRangeStartDate(), longRangeEndDate());

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }
}