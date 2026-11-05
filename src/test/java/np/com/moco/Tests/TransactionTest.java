package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.OutletClient;
import np.com.moco.Client.TransactionClient;
import np.com.moco.Utils.ConfigReader;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transaction APIs: list/search, CSV download, refunds, status and dynamic QR.
 *
 * Tests that need a real transaction use the newest purchase transaction of the last 30 days
 * and are SKIPPED (not failed) when there is none.
 */
public class TransactionTest extends BaseClass {

    private static final String NO_TOKEN = null;
    private static final String UNKNOWN_TXN_ID = "TXN-DOES-NOT-EXIST-0001";

    private final TransactionClient client = new TransactionClient();
    private final OutletClient outletClient = new OutletClient();

    private String outletId;
    private Map<String, String> sample;   // a real purchase transaction, fetched lazily

    // ---------------- helpers ----------------

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    private String outletId() {
        if (outletId == null) {
            Response outlets = outletClient.getOutlets(getAuthToken());
            ResponseAssertion.assertStatusCode(outlets, 200);
            outletId = outlets.jsonPath().getString("[0].id");
        }
        return outletId;
    }

    /** Last 30 days, first 10 records, of the given type. */
    private Map<String, String> listParams(String type) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("startDate", LocalDate.now().minusDays(30).toString());
        params.put("endDate", LocalDate.now().toString());
        params.put("type", type);
        params.put("startRecord", "0");
        params.put("count", "10");
        return params;
    }

    /** Same as listParams but without the paging parameters (for the CSV download). */
    private Map<String, String> downloadParams(String type) {
        Map<String, String> params = listParams(type);
        params.remove("startRecord");
        params.remove("count");
        return params;
    }

    private Map<String, String> sampleTransaction() {
        if (sample == null) {
            Response response = client.getTransactions(getAuthToken(), listParams("purchase"));
            ResponseAssertion.assertStatusCode(response, 200);

            List<Map<String, Object>> items = response.jsonPath().getList("transaction");
            if (items == null || items.isEmpty()) {
                throw new SkipException("No purchase transactions in the last 30 days to test with");
            }
            Map<String, Object> txn = items.get(0);

            String date = String.valueOf(txn.get("transactionDate"));
            Map<String, String> found = new LinkedHashMap<>();
            found.put("purchaseTxnId", String.valueOf(txn.get("purchaseTxnId")));
            found.put("outletId", String.valueOf(txn.get("outletId")));
            found.put("referenceId", String.valueOf(txn.get("referenceId")));
            found.put("transactionDate", date.length() >= 10 ? date.substring(0, 10) : date);
            found.put("status", String.valueOf(txn.get("status")));
            sample = found;
        }
        return sample;
    }

    // =====================================================================
    //  GET /merchant/transaction  (list + search)
    // =====================================================================

    @Test(priority = 1)
    public void getPurchaseTransactionsTest() {
        Response response = client.getTransactions(getAuthToken(), listParams("purchase"));

        ResponseAssertion.assertTransactionList(response);
        ResponseAssertion.assertTokenPresent(response);
        ResponseAssertion.assertFieldsNotEmpty(response,
                "totalTransactionCount", "totalTransactionAmount", "totalRefundedAmount", "netAmount");
    }

    @Test(priority = 2)
    public void getRefundTransactionsTest() {
        ResponseAssertion.assertTransactionList(client.getTransactions(getAuthToken(), listParams("refund")));
    }

    @Test(priority = 3)
    public void getVoidTransactionsTest() {
        ResponseAssertion.assertTransactionList(client.getTransactions(getAuthToken(), listParams("void")));
    }

    @Test(priority = 4)
    public void transactionItemsHaveRequiredValuesTest() {
        sampleTransaction();   // skips this test when there are no transactions
        Response response = client.getTransactions(getAuthToken(), listParams("purchase"));

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertListFieldsNotEmpty(response, "transaction",
                "purchaseTxnId", "outletId", "transactionDate", "interchange", "amount", "currency", "status");
    }

    @Test(priority = 5)
    public void transactionTotalsAreConsistentTest() {
        Response response = client.getTransactions(getAuthToken(), listParams("purchase"));

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertTransactionNetAmountConsistent(response);
    }

    @Test(priority = 6)
    public void transactionCountLimitIsRespectedTest() {
        Map<String, String> params = listParams("purchase");
        params.put("count", "1");

        Response response = client.getTransactions(getAuthToken(), params);

        ResponseAssertion.assertTransactionList(response);
        ResponseAssertion.assertTransactionCountAtMost(response, 1);
    }

    @Test(priority = 7)
    public void filterTransactionsByOutletTest() {
        Map<String, String> params = listParams("purchase");
        params.put("outletId", outletId());

        Response response = client.getTransactions(getAuthToken(), params);

        ResponseAssertion.assertTransactionList(response);
        ResponseAssertion.assertTransactionsAllOutlet(response, outletId());
    }

    @Test(priority = 8)
    public void filterTransactionsByAllTerminalsTest() {
        Map<String, String> params = listParams("purchase");
        params.put("terminalId", "all");

        ResponseAssertion.assertTransactionList(client.getTransactions(getAuthToken(), params));
    }

    @Test(priority = 9)
    public void filterTransactionsByInterchangeTest() {
        Map<String, String> params = listParams("purchase");
        params.put("interchange", "nchl");

        Response response = client.getTransactions(getAuthToken(), params);

        ResponseAssertion.assertTransactionList(response);
        ResponseAssertion.assertTransactionsAllInterchange(response, "nchl");
    }

    @Test(priority = 10)
    public void filterTransactionsBySettledTest() {
        Map<String, String> params = listParams("purchase");
        params.put("settlementStatus", "settled");

        ResponseAssertion.assertTransactionList(client.getTransactions(getAuthToken(), params));
    }

    @Test(priority = 11)
    public void filterTransactionsByNotSettledTest() {
        Map<String, String> params = listParams("purchase");
        params.put("settlementStatus", "notSettled");

        ResponseAssertion.assertTransactionList(client.getTransactions(getAuthToken(), params));
    }

    @Test(priority = 12)
    public void getTransactionsWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getTransactions(NO_TOKEN, listParams("purchase")));
    }

    @Test(priority = 13)
    public void getTransactionsWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.getTransactions("this.is.not.a.valid.token", listParams("purchase")));
    }

    @Test(priority = 14)
    public void getTransactionsWithTamperedTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.getTransactions(tamper(getAuthToken()), listParams("purchase")));
    }

    @DataProvider(name = "requiredParams")
    public Object[][] requiredParams() {
        return new Object[][] {
                {"startDate"}, {"endDate"}, {"type"}, {"startRecord"}, {"count"}
        };
    }

    @Test(dataProvider = "requiredParams", priority = 15)
    public void getTransactionsWithMissingParamTest(String missing) {
        Map<String, String> params = listParams("purchase");
        params.remove(missing);

        Response response = client.getTransactions(getAuthToken(), params);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @DataProvider(name = "invalidParams")
    public Object[][] invalidParams() {
        return new Object[][] {
                {"startDate", "01-01-2026"}, {"startDate", "not-a-date"}, {"endDate", "2026-13-45"},
                {"type", "xyz"}, {"count", "@"}, {"count", "abc"}, {"startRecord", "@"}, {"startRecord", "abc"}
        };
    }

    @Test(dataProvider = "invalidParams", priority = 16)
    public void getTransactionsWithInvalidParamTest(String param, String badValue) {
        Map<String, String> params = listParams("purchase");
        params.put(param, badValue);

        Response response = client.getTransactions(getAuthToken(), params);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    @Test(priority = 17)
    public void getTransactionsWithEndBeforeStartTest() {
        Map<String, String> params = listParams("purchase");
        params.put("startDate", LocalDate.now().toString());
        params.put("endDate", LocalDate.now().minusDays(10).toString());

        Response response = client.getTransactions(getAuthToken(), params);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    // =====================================================================
    //  GET /merchant/transaction/download  (CSV)
    // =====================================================================

    @Test(priority = 20)
    public void downloadTransactionsTest() {
        Response response = client.downloadTransactions(getAuthToken(), downloadParams("purchase"));

        // 404 = no transactions in the range
        ResponseAssertion.assertStatusCodeIn(response, 200, 404);
        if (response.getStatusCode() == 200) {
            ResponseAssertion.assertBodyNotEmpty(response);
        }
    }

    @Test(priority = 21)
    public void downloadTransactionsWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.downloadTransactions(NO_TOKEN, downloadParams("purchase")));
    }

    @Test(priority = 22)
    public void downloadTransactionsWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(
                client.downloadTransactions("this.is.not.a.valid.token", downloadParams("purchase")));
    }

    @Test(priority = 23)
    public void downloadTransactionsWithoutTypeTest() {
        Map<String, String> params = downloadParams("purchase");
        params.remove("type");

        Response response = client.downloadTransactions(getAuthToken(), params);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }

    // =====================================================================
    //  GET /merchant/transaction/refund/{id}
    // =====================================================================

    @Test(priority = 30)
    public void getRefundsOfPurchaseTest() {
        String txnId = sampleTransaction().get("purchaseTxnId");

        Response response = client.getRefunds(getAuthToken(), txnId);

        // 404 = this purchase has no refunds
        ResponseAssertion.assertStatusCodeIn(response, 200, 404);
        if (response.getStatusCode() == 200) {
            ResponseAssertion.assertRefundTransactionList(response);
            ResponseAssertion.assertRefundsBelongToPurchase(response, txnId);
        }
    }

    @Test(priority = 31)
    public void getRefundsWithoutTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getRefunds(NO_TOKEN, UNKNOWN_TXN_ID));
    }

    @Test(priority = 32)
    public void getRefundsWithInvalidTokenTest() {
        ResponseAssertion.assertUnauthorized(client.getRefunds("this.is.not.a.valid.token", UNKNOWN_TXN_ID));
    }

    @Test(priority = 33)
    public void getRefundsOfUnknownTransactionTest() {
        ResponseAssertion.assertStatusCodeIn(client.getRefunds(getAuthToken(), UNKNOWN_TXN_ID), 404, 422);
    }

    // =====================================================================
    //  POST /merchant/transaction/refund/{id}  (REFUND REQUEST)
    // =====================================================================

    @Test(priority = 40)
    public void refundWithoutTokenTest() {
        Response response = client.refund(NO_TOKEN, UNKNOWN_TXN_ID, "1", "automated api test", ConfigReader.get("pin"));

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 41)
    public void refundWithInvalidTokenTest() {
        Response response = client.refund("this.is.not.a.valid.token", UNKNOWN_TXN_ID, "1",
                "automated api test", ConfigReader.get("pin"));

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 42)
    public void refundUnknownTransactionTest() {
        Response response = client.refund(getAuthToken(), UNKNOWN_TXN_ID, "1",
                "automated api test", ConfigReader.get("pin"));

        ResponseAssertion.assertStatusCodeIn(response, 404, 422);
    }

    @Test(priority = 43)
    public void refundWithoutAmountTest() {
        Response response = client.refund(getAuthToken(), UNKNOWN_TXN_ID, null,
                "automated api test", ConfigReader.get("pin"));

        ResponseAssertion.assertStatusCodeIn(response, 400, 404, 422);
    }

    @Test(priority = 44)
    public void refundWithoutReasonTest() {
        Response response = client.refund(getAuthToken(), UNKNOWN_TXN_ID, "1", null, ConfigReader.get("pin"));

        ResponseAssertion.assertStatusCodeIn(response, 400, 404, 422);
    }

    @Test(priority = 45)
    public void refundWithoutPinTest() {
        Response response = client.refund(getAuthToken(), UNKNOWN_TXN_ID, "1", "automated api test", null);

        ResponseAssertion.assertStatusCodeIn(response, 400, 404, 422);
    }

    /**
     * Disabled on purpose: this sends a real refund request for a real purchase transaction.
     * It refunds 1 (the smallest amount) of the newest purchase. Turn it on only where that is harmless.
     * (A wrong-PIN refund test is intentionally not included, because a wrong PIN may count
     * towards the operator's 5-attempt lockout.)
     */
    @Test(priority = 46, enabled = false)
    public void refundPurchaseTest() {
        String txnId = sampleTransaction().get("purchaseTxnId");

        Response response = client.refund(getAuthToken(), txnId, "1", "automated api test", ConfigReader.get("pin"));

        ResponseAssertion.assertRefundRequestSent(response);
    }

    // =====================================================================
    //  GET /merchant/transaction/status
    // =====================================================================

    @Test(priority = 50)
    public void getTransactionStatusTest() {
        Map<String, String> txn = sampleTransaction();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("outletId", txn.get("outletId"));
        params.put("referenceNumber", txn.get("referenceId"));
        params.put("transactionDate", txn.get("transactionDate"));

        Response response = client.getTransactionStatus(getAuthToken(), params);

        ResponseAssertion.assertTransactionStatusEquals(response, txn.get("status"));
        ResponseAssertion.assertFieldsNotEmpty(response, "purchaseTxnId", "amount", "currency", "status");
    }

    @Test(priority = 51)
    public void getTransactionStatusWithoutTokenTest() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("outletId", outletId());
        params.put("referenceNumber", "ref-1");
        params.put("transactionDate", LocalDate.now().toString());

        ResponseAssertion.assertUnauthorized(client.getTransactionStatus(NO_TOKEN, params));
    }

    @Test(priority = 52)
    public void getTransactionStatusOfUnknownReferenceTest() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("outletId", outletId());
        params.put("referenceNumber", UNKNOWN_TXN_ID);
        params.put("transactionDate", LocalDate.now().toString());

        ResponseAssertion.assertStatusCodeIn(client.getTransactionStatus(getAuthToken(), params), 404, 422);
    }

    @DataProvider(name = "statusRequiredParams")
    public Object[][] statusRequiredParams() {
        return new Object[][] { {"outletId"}, {"referenceNumber"}, {"transactionDate"} };
    }

    @Test(dataProvider = "statusRequiredParams", priority = 53)
    public void getTransactionStatusWithMissingParamTest(String missing) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("outletId", outletId());
        params.put("referenceNumber", UNKNOWN_TXN_ID);
        params.put("transactionDate", LocalDate.now().toString());
        params.remove(missing);

        Response response = client.getTransactionStatus(getAuthToken(), params);

        ResponseAssertion.assertStatusCodeIn(response, 400, 422);
    }


}