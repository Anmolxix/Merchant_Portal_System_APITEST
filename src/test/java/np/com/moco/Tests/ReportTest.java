package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.MerchantClient;
import np.com.moco.Client.ReportClient;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;


public class ReportTest extends BaseClass {

    private static final String NO_TOKEN = null;
    private static final int LOOKBACK_MONTHS = 6;

    // a month far in the past where there should be no transactions
    private static final String EMPTY_MONTH = "2000-01";

    private final ReportClient client = new ReportClient();

    // fetched lazily
    private String dataMonth;
    private String outletId;

    // ---------------- helpers ----------------

    private String currentMonth() {
        return YearMonth.now().toString();   // yyyy-MM
    }

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    /** Reads a value that may arrive as 12 or "12". */
    private double number(Response response, String path) {
        Object value = response.jsonPath().get(path);
        Assert.assertNotNull(value, "'" + path + "' is missing in: " + response.asString());
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            Assert.fail("'" + path + "' is not numeric: '" + value + "'");
            return 0; // unreachable
        }
    }

    /** Sum of one numeric field over every item of a list response. */
    private double sum(Response response, String field) {
        List<Map<String, Object>> items = response.jsonPath().getList("$");
        double total = 0;
        for (Map<String, Object> item : items) {
            total += Double.parseDouble(String.valueOf(item.get(field)).trim());
        }
        return total;
    }

    /** The most recent month (up to LOOKBACK_MONTHS back) that has transactions, or skip. */
    private String dataMonth() {
        if (dataMonth != null) {
            return dataMonth;
        }
        for (int i = 0; i < LOOKBACK_MONTHS; i++) {
            String month = YearMonth.now().minusMonths(i).toString();
            Response summary = client.getSummary(getAuthToken(), month, null);
            if (summary.getStatusCode() == 200 && number(summary, "totalTransactionCount") > 0) {
                dataMonth = month;
                return dataMonth;
            }
        }
        throw new SkipException("No transactions in the last " + LOOKBACK_MONTHS
                + " months, so report data tests are skipped");
    }

    private String outletId() {
        if (outletId == null) {
            Response outlets = new MerchantClient().getOutlets(getAuthToken());
            ResponseAssertion.assertStatusCode(outlets, 200);
            outletId = outlets.jsonPath().getString("[0].id");
            Assert.assertNotNull(outletId, "The merchant has no outlets");
        }
        return outletId;
    }

    /** A month with no data may be an empty 200 list or a 404 (the swagger lists 404 "Transaction not found"). */
    private void assertEmptyOrNotFound(Response response) {
        ResponseAssertion.assertStatusCodeIn(response, 200, 404);
        if (response.getStatusCode() == 200) {
            Assert.assertTrue(response.jsonPath().getList("$").isEmpty(),
                    "Expected an empty list for a month with no data | body: " + response.asString());
        }
    }

    @DataProvider(name = "reportTypes")
    public Object[][] reportTypes() {
        return new Object[][] {
                {ReportClient.SUMMARY}, {ReportClient.CUSTOMER}, {ReportClient.DAYWISE}, {ReportClient.HOURWISE}
        };
    }

    // =====================================================================
    //  Checks shared by all four reports
    // =====================================================================

    @Test(dataProvider = "reportTypes", priority = 1)
    public void reportWithoutTokenTest(String type) {
        ResponseAssertion.assertUnauthorized(client.getReport(type, NO_TOKEN, currentMonth(), null));
    }

    @Test(dataProvider = "reportTypes", priority = 2)
    public void reportWithInvalidTokenTest(String type) {
        ResponseAssertion.assertUnauthorized(
                client.getReport(type, "this.is.not.a.valid.token", currentMonth(), null));
    }

    @Test(dataProvider = "reportTypes", priority = 3)
    public void reportWithTamperedTokenTest(String type) {
        ResponseAssertion.assertUnauthorized(
                client.getReport(type, tamper(getAuthToken()), currentMonth(), null));
    }

    @Test(dataProvider = "reportTypes", priority = 4)
    public void reportWithoutYearMonthTest(String type) {
        Response response = client.getReport(type, getAuthToken(), null, null);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(dataProvider = "reportTypes", priority = 5)
    public void reportWithInvalidYearMonthTest(String type) {
        // wrong order, an impossible month, and plain text
        for (String bad : new String[] {"04-2026", "2026-13", "not-a-month"}) {
            Response response = client.getReport(type, getAuthToken(), bad, null);

            ResponseAssertion.assertStatusCodeIn(response, 400, 422);
        }
    }

    @Test(dataProvider = "reportTypes", priority = 6)
    public void reportContentTypeAndResponseTimeTest(String type) {
        Response response = client.getReport(type, getAuthToken(), dataMonth(), null);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertContentTypeContains(response, "application/json");
        ResponseAssertion.assertResponseTimeBelow(response, 5000);
    }

    // =====================================================================
    //  GET /merchant/report/summary
    // =====================================================================

    @Test(priority = 10)
    public void getSummaryReportTest() {
        Response response = client.getSummary(getAuthToken(), currentMonth(), null);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertFieldsNotEmpty(response,
                "totalUniqueCustomers", "totalTransactionCount", "totalTransactionAmount");
    }

    @Test(priority = 11)
    public void getSummaryValuesAreNotNegativeTest() {
        Response response = client.getSummary(getAuthToken(), dataMonth(), null);

        ResponseAssertion.assertStatusCode(response, 200);
        Assert.assertTrue(number(response, "totalUniqueCustomers") >= 0, "totalUniqueCustomers is negative");
        Assert.assertTrue(number(response, "totalTransactionCount") > 0, "totalTransactionCount should be above 0");
        Assert.assertTrue(number(response, "totalTransactionAmount") > 0, "totalTransactionAmount should be above 0");
    }

    @Test(priority = 12)
    public void getSummaryByOutletIsNotAboveMerchantTotalTest() {
        Response total = client.getSummary(getAuthToken(), dataMonth(), null);
        Response outlet = client.getSummary(getAuthToken(), dataMonth(), outletId());

        ResponseAssertion.assertStatusCode(total, 200);
        ResponseAssertion.assertStatusCode(outlet, 200);
        ResponseAssertion.assertFieldsNotEmpty(outlet,
                "totalUniqueCustomers", "totalTransactionCount", "totalTransactionAmount");
        Assert.assertTrue(number(outlet, "totalTransactionCount") <= number(total, "totalTransactionCount"),
                "One outlet has more transactions than the whole merchant");
        Assert.assertTrue(number(outlet, "totalTransactionAmount") <= number(total, "totalTransactionAmount") + 0.01,
                "One outlet has a bigger amount than the whole merchant");
    }

    @Test(priority = 13)
    public void getSummaryForFutureMonthTest() {
        Response response = client.getSummary(getAuthToken(), "2099-01", null);

        // the swagger doesn't say how a future month is handled
        ResponseAssertion.assertStatusCodeIn(response, 200, 404, 422);
        if (response.getStatusCode() == 200) {
            Assert.assertEquals(number(response, "totalTransactionCount"), 0.0,
                    "A future month should have no transactions");
        }
    }

    @Test(priority = 14)
    public void getSummaryForUnknownOutletTest() {
        Response response = client.getSummary(getAuthToken(), currentMonth(), "O999999999");

        ResponseAssertion.assertStatusCodeIn(response, 400, 403, 404, 422);
    }

    // =====================================================================
    //  GET /merchant/report/customer
    // =====================================================================

    @Test(priority = 20)
    public void getCustomerReportTest() {
        Response response = client.getCustomer(getAuthToken(), dataMonth(), null);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "payerName", "payerNumber", "totalAmount");
    }

    @Test(priority = 21)
    public void getCustomerReportForMonthWithNoDataTest() {
        assertEmptyOrNotFound(client.getCustomer(getAuthToken(), EMPTY_MONTH, null));
    }

    @Test(priority = 22)
    public void getCustomerReportByOutletTest() {
        Response response = client.getCustomer(getAuthToken(), dataMonth(), outletId());

        // this outlet may have had no customers that month
        ResponseAssertion.assertStatusCodeIn(response, 200, 404);
    }

    // =====================================================================
    //  GET /merchant/report/daywise
    // =====================================================================

    @Test(priority = 30)
    public void getDaywiseReportTest() {
        Response response = client.getDaywise(getAuthToken(), dataMonth(), null);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "day", "totalCount", "totalAmount");
    }

    @Test(priority = 31)
    public void daywiseCountsAddUpToSummaryTest() {
        Response daywise = client.getDaywise(getAuthToken(), dataMonth(), null);
        Response summary = client.getSummary(getAuthToken(), dataMonth(), null);

        ResponseAssertion.assertStatusCode(daywise, 200);
        ResponseAssertion.assertStatusCode(summary, 200);
        Assert.assertEquals(sum(daywise, "totalCount"), number(summary, "totalTransactionCount"),
                "Daywise counts don't add up to the summary count");
        Assert.assertEquals(sum(daywise, "totalAmount"), number(summary, "totalTransactionAmount"), 0.01,
                "Daywise amounts don't add up to the summary amount");
    }

    @Test(priority = 32)
    public void getDaywiseReportForMonthWithNoDataTest() {
        assertEmptyOrNotFound(client.getDaywise(getAuthToken(), EMPTY_MONTH, null));
    }

    // =====================================================================
    //  GET /merchant/report/hourwise
    // =====================================================================

    @Test(priority = 40)
    public void getHourwiseReportTest() {
        Response response = client.getHourwise(getAuthToken(), dataMonth(), null);

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertListFieldsNotEmpty(response, "$", "hour", "totalCount", "totalAmount");
    }

    @Test(priority = 41)
    public void hourwiseHoursAreBetween0And23Test() {
        Response response = client.getHourwise(getAuthToken(), dataMonth(), null);
        ResponseAssertion.assertStatusCode(response, 200);

        List<Map<String, Object>> items = response.jsonPath().getList("$");
        for (int i = 0; i < items.size(); i++) {
            double hour = Double.parseDouble(String.valueOf(items.get(i).get("hour")).trim());
            Assert.assertTrue(hour >= 0 && hour <= 23, "Item " + i + " has an hour outside 0-23: " + hour);
        }
    }

    @Test(priority = 42)
    public void hourwiseCountsAddUpToSummaryTest() {
        Response hourwise = client.getHourwise(getAuthToken(), dataMonth(), null);
        Response summary = client.getSummary(getAuthToken(), dataMonth(), null);

        ResponseAssertion.assertStatusCode(hourwise, 200);
        ResponseAssertion.assertStatusCode(summary, 200);
        Assert.assertEquals(sum(hourwise, "totalCount"), number(summary, "totalTransactionCount"),
                "Hourwise counts don't add up to the summary count");
        Assert.assertEquals(sum(hourwise, "totalAmount"), number(summary, "totalTransactionAmount"), 0.01,
                "Hourwise amounts don't add up to the summary amount");
    }

    @Test(priority = 43)
    public void getHourwiseReportForMonthWithNoDataTest() {
        assertEmptyOrNotFound(client.getHourwise(getAuthToken(), EMPTY_MONTH, null));
    }
}