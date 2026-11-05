# Merchant Portal System API Tests

Automated REST API test suite for the **MOCO Merchant Portal System (MPS)**, written against the
exposed API collection (Swagger 2.0, v1.1.0, updated 2026-10-09).

- **Base URL:** `https://mps0.moco.com.np/api`
- **Auth:** operator login returns a JWT in the `X-MOCO-MPS-TOKEN` response header. Every protected
  call sends it back, and each response returns a refreshed token.

## Tech stack

- Java 17
- TestNG 7.11
- REST Assured 6.0.1
- Maven

## What is covered

| Swagger tag | Test class | Endpoints |
|---|---|---|
| Role | `RolesTest` | `GET /role`, `GET /permission` |
| MCC | `MccTest` | `GET /mcc` |
| Address | `AddressTest` | `GET /municipality` |
| Dashboard | `DashboardTest` | `GET /merchant/dashboard` |
| Merchant and operators | `MerchantTest` | view and update merchant, interchanges, operator list/search, add, view, update, change status, delete |
| Outlet | `OutletTest` | `GET /merchant/outlet`, `GET` / `PUT /outlet/{outletId}` |
| Terminal | `TerminalTest` | list, add, view, update and delete terminals, `GET /terminal` |
| Transaction | `TransactionTest` | list and filter, CSV download, refunds, transaction status |
| Advice | `AdviceTest` | advice report dates, PDF download, zip download link |
| Activity Log | `ActivityLogTest` | `GET /merchant/operator/activity` |

Most endpoints have both positive and negative tests:

- missing, empty, invalid and tampered `X-MOCO-MPS-TOKEN` (expects `401`)
- missing or malformed required parameters (expects `400` / `422`)
- unknown resources (expects `404`)
- duplicates (expects `409`)
- content type and response-time checks

## Project structure

```
src/test/java/np/com/moco/
├── Client/        API clients, one per resource
├── Tests/         TestNG test classes
├── assertions/    Reusable response assertions (ResponseAssertion)
├── base/          BaseClass: login and token handling
└── Utils/         ConfigReader and helpers
```

`ResponseAssertion` holds the shared checks (status codes, token header, JSON fields, list
contents, CSV/PDF/PNG validation) and a helper for each response shape in the swagger, so the
tests stay short.

## Setup

1. Clone the repo and open it in IntelliJ IDEA (or any Maven-aware IDE).
2. Create your config file with the base URL, the login operator `id` and `pin`, and a spare
   `test.operator.id` for the operator lifecycle tests. **Never commit real credentials.**
3. Run the suite:

   ```
   mvn test
   ```

## Latest run

283 tests in about 75 seconds: **258 passed, 20 failed, 5 skipped.** The failures are data or
environment issues, not API outages:

| Cause | Tests affected |
|---|---|
| Missing permissions (`403`) | terminal list, view and delete |
| No data on the account (`404`) | advice reports, void/refund/settled transactions, terminals, disabled operators |
| Test input rejected (`422`) | terminal callback URL, transaction reference number, activity-log section id |
| Skipped | terminal lifecycle tests that depend on `addTerminalTest` |

The 404s are not bugs: this API returns `404` for an empty result instead of an empty `200` list.

## Notes on test behaviour

- **Operator lifecycle** tests (add, view, update, change status, delete) create ONE throwaway
  operator using `test.operator.id` and delete it afterwards. Use a spare number, never a real
  operator. The test refuses to run if it matches the login operator.
- **Terminal lifecycle** tests are chained with `dependsOnMethods`. If adding a terminal fails,
  the rest are reported as skipped.
- **Disabled on purpose:** tests with real side effects are off by default (merchant information
  change request, refund request). Enable them only on an environment where that is harmless.
- **Transaction tests** use the newest purchase transaction from the last 30 days and are
  skipped, not failed, when there is none.
- **Wrong-PIN tests** are left out, because failed attempts count towards the 5-attempt operator
  lockout.
- **Permissions:** the login operator needs the terminal view, add, update and delete permissions
  for the terminal tests to pass.

## Known differences between the swagger and the live API

| Endpoint | Swagger says | Tests expect (observed behaviour) |
|---|---|---|
| `GET /mcc` | each item has `code` and `name` | each item has `id` and `name` |
| `GET /outlet/{outletId}` | response includes `settlementInformation` | only `businessDetail` and `businessAddress` are returned |
| `GET /merchant/transaction/qr` | returns `image/png` over a plain GET | WebSocket connection, so these tests are disabled |
| `GET /merchant/operator/activity` | array is named `acivityLog` (typo) | tests use the same spelling; change `ACTIVITY_LIST` if the API fixes it |
| Operator status | `enabled` / `disabled` | a disabled operator reads back as `inactive` |
| List endpoints with no results | not documented | `404` instead of an empty `200` |

If a test fails on one of these, check this table first.

## Known limitations

- The dynamic QR endpoint can't be tested with REST Assured. A WebSocket client would be needed.
- Some tests accept more than one status code (for example `400` or `422`), because the swagger
  doesn't say which one the API uses for a given validation failure.
