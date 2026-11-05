package np.com.moco.assertions;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ResponseAssertion {

    public static final String TOKEN_HEADER = "X-MOCO-MPS-TOKEN";
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern YEAR_MONTH = Pattern.compile("\\d{4}-\\d{2}");

    // ---- status code ----

    public static void assertStatusCode(Response response, int expected) {
        assertStatusCode(response, expected, "Unexpected status code");
    }

    public static void assertStatusCode(Response response, int expected, String message) {
        Assert.assertEquals(response.statusCode(), expected, withBody(message, response));
    }

    public static void assertStatusCodeIn(Response response, int... allowed) {
        List<Integer> allowedList = Arrays.stream(allowed).boxed().toList();
        Assert.assertTrue(allowedList.contains(response.statusCode()),
                withBody("Status " + response.statusCode() + " not in " + allowedList, response));
    }

    // shortcuts for the error codes used in the swagger
    public static void assertBadRequest(Response response)   { assertStatusCode(response, 400, "Expected 400 Bad Request"); }
    public static void assertUnauthorized(Response response) { assertStatusCode(response, 401, "Expected 401 Unauthorized"); }
    public static void assertForbidden(Response response)    { assertStatusCode(response, 403, "Expected 403 Forbidden"); }
    public static void assertNotFound(Response response)     { assertStatusCode(response, 404, "Expected 404 Not Found"); }
    public static void assertConflict(Response response)     { assertStatusCode(response, 409, "Expected 409 Conflict"); }
    public static void assertUnprocessable(Response response){ assertStatusCode(response, 422, "Expected 422 Unprocessable Entity"); }

    // ---- token (X-MOCO-MPS-TOKEN) ----

    public static void assertTokenPresent(Response response) {
        String token = response.getHeader(TOKEN_HEADER);
        Assert.assertTrue(token != null && !token.isBlank(),
                "Expected header '" + TOKEN_HEADER + "' but it was missing or blank");
    }

    public static void assertTokenAbsent(Response response) {
        String token = response.getHeader(TOKEN_HEADER);
        Assert.assertTrue(token == null || token.isBlank(),
                "Did not expect header '" + TOKEN_HEADER + "' but got one");
    }

    // ---- cookies ----

    public static void assertCookiesNotEmpty(Response response) {
        Assert.assertFalse(response.getCookies().isEmpty(),
                "Expected the response to return cookies, but none were returned");
    }

    public static void assertCookiesEmpty(Response response) {
        Assert.assertTrue(response.getCookies().isEmpty(),
                "Expected no cookies, but got: " + response.getCookies().keySet());
    }

    public static void assertCookiePresent(Response response, String cookieName) {
        Assert.assertTrue(response.getCookies().containsKey(cookieName),
                "Expected cookie '" + cookieName + "' but got: " + response.getCookies().keySet());
    }

    // ---- headers / content type / timing ----

    public static void assertContentType(Response response, ContentType expected) {
        Assert.assertTrue(response.contentType().toLowerCase().contains(expected.toString().toLowerCase()),
                "Expected content type " + expected + " but was " + response.contentType());
    }

    public static void assertContentTypeContains(Response response, String expected) {
        String actual = response.contentType();
        Assert.assertTrue(actual != null && actual.toLowerCase().contains(expected.toLowerCase()),
                "Expected content type to contain '" + expected + "' but was " + actual);
    }

    public static void assertHeaderEquals(Response response, String header, String expected) {
        Assert.assertEquals(response.getHeader(header), expected,
                "Unexpected value for header '" + header + "'");
    }

    public static void assertResponseTimeBelow(Response response, long maxMillis) {
        Assert.assertTrue(response.time() < maxMillis,
                "Response took " + response.time() + " ms, expected under " + maxMillis + " ms");
    }

    // ---- body (plain text) ----

    public static void assertBodyNotEmpty(Response response) {
        Assert.assertFalse(response.asString().isBlank(), "Expected a response body, but it was empty");
    }

    public static void assertBodyEmpty(Response response) {
        Assert.assertTrue(response.asString().isBlank(),
                "Expected an empty body but got: " + response.asString());
    }

    public static void assertBodyContains(Response response, String text) {
        Assert.assertTrue(response.asString().contains(text),
                "Expected body to contain '" + text + "' but body was: " + response.asString());
    }

    // ---- body (JSON, using JsonPath such as "email" or "role.name" or "[0].id") ----

    public static void assertJsonFieldEquals(Response response, String jsonPath, Object expected) {
        Object actual = response.jsonPath().get(jsonPath);
        Assert.assertEquals(actual, expected,
                "Unexpected value for '" + jsonPath + "' | body: " + response.asString());
    }

    public static void assertJsonFieldNotNull(Response response, String jsonPath) {
        Assert.assertNotNull(response.jsonPath().get(jsonPath),
                "Expected '" + jsonPath + "' in body: " + response.asString());
    }

    public static void assertJsonFieldsPresent(Response response, String... jsonPaths) {
        for (String path : jsonPaths) {
            assertJsonFieldNotNull(response, path);
        }
    }

    public static void assertJsonListNotEmpty(Response response, String jsonPath) {
        List<?> list = response.jsonPath().getList(jsonPath);
        Assert.assertTrue(list != null && !list.isEmpty(),
                "Expected a non-empty list at '" + jsonPath + "' | body: " + response.asString());
    }

    public static void assertJsonListSizeAtMost(Response response, String jsonPath, int max) {
        List<?> list = response.jsonPath().getList(jsonPath);
        Assert.assertNotNull(list, "Expected a list at '" + jsonPath + "' | body: " + response.asString());
        Assert.assertTrue(list.size() <= max,
                "Expected at most " + max + " items but got " + list.size() + " | body: " + response.asString());
    }

    /** Every item in the list (use "$" for a root array) must have all fields. Nested: "role.id". */
    public static void assertEachItemHasFields(Response response, String listPath, String... fields) {
        List<Map<String, Object>> items = listAt(response, listPath);
        for (int i = 0; i < items.size(); i++) {
            for (String field : fields) {
                Assert.assertNotNull(valueAt(items.get(i), field),
                        "Item " + i + " of '" + listPath + "' is missing '" + field + "' | body: " + response.asString());
            }
        }
    }

    /** Every item's field equals expected (case-insensitive). Good for filter tests. */
    public static void assertAllItemsFieldEquals(Response response, String listPath, String field, Object expected) {
        List<Map<String, Object>> items = listAt(response, listPath);
        for (int i = 0; i < items.size(); i++) {
            Object actual = valueAt(items.get(i), field);
            Assert.assertTrue(actual != null && String.valueOf(actual).equalsIgnoreCase(String.valueOf(expected)),
                    "Item " + i + " field '" + field + "' expected '" + expected + "' but was '" + actual + "'");
        }
    }

    /** Every item's field contains text (case-insensitive). Good for name search tests. */
    public static void assertAllItemsFieldContains(Response response, String listPath, String field, String text) {
        List<Map<String, Object>> items = listAt(response, listPath);
        for (int i = 0; i < items.size(); i++) {
            Object actual = valueAt(items.get(i), field);
            Assert.assertTrue(actual != null && String.valueOf(actual).toLowerCase().contains(text.toLowerCase()),
                    "Item " + i + " field '" + field + "' expected to contain '" + text + "' but was '" + actual + "'");
        }
    }

    // ---- CSV ----

    /** The first line of a CSV is the header row; check it has these columns. */
    public static void assertCsvHasColumns(Response response, String... columns) {
        String header = response.asString().lines().findFirst().orElse("");
        for (String column : columns) {
            Assert.assertTrue(header.toLowerCase().contains(column.toLowerCase()),
                    "Expected CSV header to contain '" + column + "' but header was: " + header);
        }
    }

    /** Header row plus at least one data row. */
    public static void assertCsvHasDataRows(Response response) {
        long lines = response.asString().lines().filter(l -> !l.isBlank()).count();
        Assert.assertTrue(lines >= 2,
                "Expected a CSV header and at least one data row but found " + lines + " line(s)");
    }

    // ---- files (PDF / PNG) ----

    public static void assertPdf(Response response) {
        assertStatusCode(response, 200, "PDF download failed");
        assertContentTypeContains(response, "application/pdf");
        byte[] b = response.asByteArray();
        Assert.assertTrue(b.length > 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F',
                "Response body is not a valid PDF");
    }

    public static void assertPng(Response response) {
        assertStatusCode(response, 200, "Image download failed");
        assertContentTypeContains(response, "image/png");
        byte[] b = response.asByteArray();
        Assert.assertTrue(b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G',
                "Response body is not a valid PNG");
    }

    // ---- format helpers ----

    public static void assertDateFormat(String value, String label) {
        Assert.assertTrue(value != null && DATE.matcher(value.trim()).find(),
                "Expected " + label + " in yyyy-MM-dd format but was: " + value);
    }

    public static void assertYearMonthFormat(String value) {
        Assert.assertTrue(value != null && YEAR_MONTH.matcher(value).matches(),
                "Expected yyyy-MM but was: " + value);
    }

    // =====================================================================
    //  SWAGGER SPECIFIC: Merchant Portal System v1.1.0
    // =====================================================================

    // ---- common success shapes ----

    /** 200 + JSON */
    public static void assertOkJson(Response response) {
        assertStatusCode(response, 200);
        assertContentTypeContains(response, "application/json");
    }

    /** 200 + text/plain (POST / PUT / DELETE endpoints) */
    public static void assertOkText(Response response) {
        assertStatusCode(response, 200);
        assertContentTypeContains(response, "text/plain");
    }

    /** 200 + text/plain + refreshed token */
    public static void assertOkTextWithToken(Response response) {
        assertOkText(response);
        assertTokenPresent(response);
    }

    // ---- /role, /permission, /mcc, /municipality ----

    public static void assertRoleList(Response response) {
        assertOkJson(response);
        assertJsonListNotEmpty(response, "$");
        assertEachItemHasFields(response, "$", "id", "name");
    }

    public static void assertPermissionList(Response response) {
        assertOkJson(response);
        assertJsonListNotEmpty(response, "$");
        assertEachItemHasFields(response, "$", "id", "section", "operation", "description");
    }

    public static void assertMccList(Response response) {
        assertOkJson(response);
        assertJsonListNotEmpty(response, "$");
        assertEachItemHasFields(response, "$", "code", "name");
    }

    public static void assertMunicipalityList(Response response) {
        assertOkJson(response);
        assertJsonListNotEmpty(response, "$");
    }

    // ---- /merchant/dashboard ----

    public static void assertDashboard(Response response) {
        assertOkJson(response);
        assertJsonFieldsPresent(response, "totalTxnCount", "totalTxnAmount");
        assertNonNegative(response, "totalTxnCount");
        assertNonNegative(response, "totalTxnAmount");
    }

    // ---- /operator (otp, pin, authentication) ----

    public static void assertAuthenticationSuccess(Response response) {
        assertStatusCode(response, 200, "Operator authentication failed");
        assertTokenPresent(response);
        assertJsonListNotEmpty(response, "$");
        assertEachItemHasFields(response, "$", "id", "name", "operatorStatus");
    }

    public static void assertAuthenticationFailed(Response response) {
        assertStatusCode(response, 401, "Expected authentication to fail");
        assertTokenAbsent(response);
    }

    /** 403 after 5 wrong attempts */
    public static void assertOperatorBlocked(Response response) {
        assertStatusCode(response, 403, "Expected operator to be blocked");
    }

    public static void assertOtpSent(Response response)        { assertOkText(response); }
    public static void assertPinActivatedOrReset(Response response) { assertOkText(response); }
    public static void assertPinChanged(Response response)     { assertOkTextWithToken(response); }
    public static void assertLogoutSuccess(Response response)  { assertOkText(response); }

    // ---- /merchant/operator ----

    public static void assertOperatorList(Response response) {
        assertOkJson(response);
        assertTokenPresent(response);
        assertJsonListNotEmpty(response, "$");
        assertEachItemHasFields(response, "$", "id", "name", "status", "role.id", "role.name", "merchant.id");
    }

    /** Search can return an empty list, so only the shape is checked. */
    public static void assertOperatorSearchResult(Response response) {
        assertOkJson(response);
        Assert.assertNotNull(response.jsonPath().getList("$"), "Expected a JSON array | body: " + response.asString());
        assertEachItemHasFields(response, "$", "id", "name", "status");
    }

    /** GET /merchant/operator/{id} and POST /merchant/{merchantId}/operator/login */
    public static void assertOperatorModel(Response response) {
        assertOkJson(response);
        assertJsonFieldsPresent(response, "id", "name", "status", "role.id", "role.name", "merchant.id");
    }

    /** Add / update / delete / change status */
    public static void assertOperatorMutationSuccess(Response response) {
        assertOkTextWithToken(response);
    }

    // ---- /merchant/outlet, /outlet/{outletId} ----

    public static void assertOutletList(Response response) {
        assertOkJson(response);
        assertJsonListNotEmpty(response, "$");
        assertEachItemHasFields(response, "$", "id", "name");
    }

    public static void assertOutletDetail(Response response) {
        assertOkJson(response);
        assertJsonFieldsPresent(response,
                "businessDetail.doingBusinessAs",
                "businessDetail.mcc.id",
                "businessDetail.mcc.name",
                "businessDetail.txnCurrency",
                "businessAddress.province",
                "businessAddress.district",
                "businessAddress.municipality.id",
                "businessAddress.ward");
    }

    /** PUT /outlet/{outletId}: request is sent, not applied */
    public static void assertOutletUpdateRequestSent(Response response) {
        assertOkTextWithToken(response);
    }

    // ---- terminals ----

    /** GET /outlet/{outletId}/terminal and GET /terminal */
    public static void assertTerminalList(Response response) {
        assertOkJson(response);
        assertJsonListNotEmpty(response, "$");
        assertEachItemHasFields(response, "$", "terminalId", "name");
    }

    /** GET /outlet/{outletId}/terminal/{terminalId} */
    public static void assertTerminalDetail(Response response) {
        assertOkJson(response);
        assertJsonFieldsPresent(response, "terminalId", "name");
    }

    public static void assertTerminalDetail(Response response, String expectedTerminalId) {
        assertTerminalDetail(response);
        assertJsonFieldEquals(response, "terminalId", expectedTerminalId);
    }

    /** Add / update / delete terminal */
    public static void assertTerminalMutationSuccess(Response response) {
        assertOkTextWithToken(response);
    }

    // ---- /merchant/transaction ----

    public static void assertTransactionList(Response response) {
        assertOkJson(response);
        assertJsonFieldsPresent(response,
                "totalTransactionCount", "totalTransactionAmount", "totalRefundedAmount", "netAmount");
        Assert.assertNotNull(response.jsonPath().getList("transaction"),
                "Expected 'transaction' array | body: " + response.asString());
        assertEachItemHasFields(response, "transaction",
                "purchaseTxnId", "outletId", "transactionDate", "interchange", "amount", "currency", "status");
    }

    public static void assertTransactionListNotEmpty(Response response) {
        assertTransactionList(response);
        assertJsonListNotEmpty(response, "transaction");
    }

    /** Pagination: never more rows than the requested count */
    public static void assertTransactionCountAtMost(Response response, int count) {
        assertJsonListSizeAtMost(response, "transaction", count);
    }

    public static void assertTransactionsAllOutlet(Response response, String outletId) {
        assertAllItemsFieldEquals(response, "transaction", "outletId", outletId);
    }

    public static void assertTransactionsAllInterchange(Response response, String interchange) {
        assertAllItemsFieldEquals(response, "transaction", "interchange", interchange);
    }

    /** netAmount = totalTransactionAmount - totalRefundedAmount */
    public static void assertTransactionNetAmountConsistent(Response response) {
        double total = response.jsonPath().getDouble("totalTransactionAmount");
        double refunded = response.jsonPath().getDouble("totalRefundedAmount");
        double net = response.jsonPath().getDouble("netAmount");
        Assert.assertEquals(net, total - refunded, 0.01,
                "netAmount != totalTransactionAmount - totalRefundedAmount | body: " + response.asString());
    }

    /** CSV download */
    public static void assertTransactionCsv(Response response) {
        assertStatusCode(response, 200);
        assertBodyNotEmpty(response);
        assertCsvHasDataRows(response);
    }

    /** Dynamic QR (PNG) */
    public static void assertDynamicQr(Response response) {
        assertPng(response);
    }

    /** GET /merchant/transaction/status */
    public static void assertTransactionStatus(Response response) {
        assertOkJson(response);
        assertJsonFieldsPresent(response,
                "purchaseTxnId", "transactionDate", "interchange", "amount", "currency", "status", "transactionType");
    }

    public static void assertTransactionStatusEquals(Response response, String expectedStatus) {
        assertTransactionStatus(response);
        assertJsonFieldEquals(response, "status", expectedStatus);
    }

    public static void assertRefundedNotAboveAmount(Response response) {
        Number refunded = response.jsonPath().get("totalRefundedAmount");
        Number amount = response.jsonPath().get("amount");
        if (refunded != null && amount != null) {
            Assert.assertTrue(refunded.doubleValue() <= amount.doubleValue() + 0.001,
                    "totalRefundedAmount " + refunded + " is more than amount " + amount);
        }
    }

    // ---- refunds ----

    /** GET /merchant/transaction/refund/{id} */
    public static void assertRefundTransactionList(Response response) {
        assertOkJson(response);
        Assert.assertNotNull(response.jsonPath().getList("$"), "Expected a JSON array | body: " + response.asString());
        assertEachItemHasFields(response, "$",
                "purchaseTxnId", "refundTxnId", "merchantRequestedTimestamp", "amount", "refundReason", "status");
    }

    public static void assertRefundsBelongToPurchase(Response response, String purchaseTxnId) {
        assertAllItemsFieldEquals(response, "$", "purchaseTxnId", purchaseTxnId);
    }

    /** POST /merchant/transaction/refund/{id} */
    public static void assertRefundRequestSent(Response response) {
        assertStatusCode(response, 200, "Refund request failed");
        assertTokenPresent(response);
    }

    // ---- /merchant ----

    public static void assertMerchantDetail(Response response) {
        assertOkJson(response);
        assertJsonFieldsPresent(response,
                "businessDetail.doingBusinessAs",
                "businessDetail.registeredBusinessName",
                "businessDetail.registrationNumber",
                "businessDetail.pan",
                "businessDetail.settlementType",
                "businessAddress.province",
                "businessAddress.district",
                "businessAddress.municipality.id",
                "settlementInformation.bankName",
                "settlementInformation.accountNumber",
                "settlementInformation.accountName");
        assertJsonListNotEmpty(response, "businessDetail.interchanges");
    }

    public static void assertMerchantUpdateRequestSent(Response response) {
        assertOkTextWithToken(response);
    }

    public static void assertInterchangeList(Response response) {
        assertStatusCode(response, 200);
        assertJsonListNotEmpty(response, "$");
        assertEachItemHasFields(response, "$", "id", "name");
    }

    // ---- advice ----

    public static void assertAdviceList(Response response) {
        assertOkJson(response);
        assertJsonListNotEmpty(response, "$");
        assertEachItemHasFields(response, "$", "reportName", "date");
        for (Map<String, Object> item : listAt(response, "$")) {
            assertDateFormat(String.valueOf(item.get("date")), "advice date");
        }
    }

    public static void assertAdvicePdf(Response response) {
        assertPdf(response);
    }

    public static void assertAdviceZipLink(Response response) {
        assertOkJson(response);
        assertJsonFieldNotNull(response, "downloadLink");
        String link = response.jsonPath().getString("downloadLink");
        Assert.assertTrue(link.startsWith("http") || link.startsWith("/"),
                "downloadLink does not look like a URL/path: " + link);
    }

    // ---- reports ----

    public static void assertSummaryReport(Response response) {
        assertOkJson(response);
        assertJsonFieldsPresent(response, "totalUniqueCustomers", "totalTransactionCount", "totalTransactionAmount");
        assertNonNegative(response, "totalUniqueCustomers");
        assertNonNegative(response, "totalTransactionCount");
        assertNonNegative(response, "totalTransactionAmount");
    }

    public static void assertCustomerReport(Response response) {
        assertOkJson(response);
        Assert.assertNotNull(response.jsonPath().getList("$"), "Expected a JSON array | body: " + response.asString());
        assertEachItemHasFields(response, "$", "payerName", "payerNumber", "totalAmount");
    }

    public static void assertDaywiseReport(Response response) {
        assertOkJson(response);
        Assert.assertNotNull(response.jsonPath().getList("$"), "Expected a JSON array | body: " + response.asString());
        assertEachItemHasFields(response, "$", "day", "totalCount", "totalAmount");
    }

    /** hour must be 0..23 */
    public static void assertHourwiseReport(Response response) {
        assertOkJson(response);
        Assert.assertNotNull(response.jsonPath().getList("$"), "Expected a JSON array | body: " + response.asString());
        assertEachItemHasFields(response, "$", "hour", "totalCount", "totalAmount");
        for (Map<String, Object> item : listAt(response, "$")) {
            int hour = ((Number) item.get("hour")).intValue();
            Assert.assertTrue(hour >= 0 && hour <= 23, "Hour out of range 0-23: " + hour);
        }
    }

    // ---- outlet QR (PDF) ----

    public static void assertOutletQrPdf(Response response) {
        assertPdf(response);
    }

    // ---- activity log ----
    // NOTE: the swagger spells the array 'acivityLog' (missing 't'). Change this if the API fixes it.
    private static final String ACTIVITY_LIST = "activityLog";

    public static void assertActivityLog(Response response) {
        assertOkJson(response);
        assertJsonFieldNotNull(response, "totalActivityCount");
        Assert.assertNotNull(response.jsonPath().getList(ACTIVITY_LIST),
                "Expected '" + ACTIVITY_LIST + "' array | body: " + response.asString());
        assertEachItemHasFields(response, ACTIVITY_LIST,
                "operatorId", "timestamp", "description", "permission.id", "permission.section", "permission.operation");
    }

    public static void assertActivityLogCountAtMost(Response response, int count) {
        assertJsonListSizeAtMost(response, ACTIVITY_LIST, count);
    }

    public static void assertActivityLogAllOperator(Response response, String operatorId) {
        assertAllItemsFieldEquals(response, ACTIVITY_LIST, "operatorId", operatorId);
    }

    // ---- helpers ----

    private static String withBody(String message, Response response) {
        return message + " | response body: " + response.asString();
    }

    private static void assertNonNegative(Response response, String jsonPath) {
        Number n = response.jsonPath().get(jsonPath);
        Assert.assertNotNull(n, "Expected number at '" + jsonPath + "' | body: " + response.asString());
        Assert.assertTrue(n.doubleValue() >= 0, "'" + jsonPath + "' should not be negative but was " + n);
    }

    private static List<Map<String, Object>> listAt(Response response, String path) {
        List<Map<String, Object>> list = response.jsonPath().getList(path);
        Assert.assertNotNull(list, "Expected a list at '" + path + "' | body: " + response.asString());
        return list;
    }

    private static Object valueAt(Map<?, ?> map, String path) {
        Object current = map;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
        }
        return current;
    }
    /** Each field must exist in the response body and not be empty. Nested: "role.name". */
    public static void assertFieldsNotEmpty(Response response, String... jsonPaths) {
        for (String path : jsonPaths) {
            Object value = response.jsonPath().get(path);
            Assert.assertFalse(isEmpty(value),
                    "Field '" + path + "' is missing or empty | body: " + response.asString());
        }
    }

    /** Response must be a non-empty array (use "$" for a root array), and every item must have each field, not empty. */
    public static void assertListFieldsNotEmpty(Response response, String listPath, String... fields) {
        List<Map<String, Object>> items = listAt(response, listPath);
        Assert.assertFalse(items.isEmpty(), "List at '" + listPath + "' is empty | body: " + response.asString());
        for (int i = 0; i < items.size(); i++) {
            for (String field : fields) {
                Object value = valueAt(items.get(i), field);
                Assert.assertFalse(isEmpty(value),
                        "Item " + i + " of '" + listPath + "': field '" + field + "' is missing or empty | body: " + response.asString());
            }
        }
    }

    /** null or blank text counts as empty. A number such as 0 is not empty. */
    private static boolean isEmpty(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

}