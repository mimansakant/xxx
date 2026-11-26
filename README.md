# 404 Collective Budget Service

The Budget Service is a Java REST API for tracking a client's budgets,
spending categories, and purchases. The separate `client/` application adds a
browser-based budgeting experience with user accounts, locally tracked bank
accounts, and budget reports.

> **Current status:** The Java Budget Service and companion bank-budgeting
> client are implemented. The API descriptions later in this README are
> historical project-spec notes; use the application source for current API
> behavior and the instructions below to start the applications.

## Running the service and client locally

### Requirements

- Java 21
- Maven
- Windows Command Prompt on Windows, or a terminal on macOS/Linux

### Start the Budget Service

From the repository root, open a terminal and run the command for your
operating system:

**Windows Command Prompt**

```bat
mvn spring-boot:run
```

**macOS/Linux**

```sh
mvn spring-boot:run
```

The service listens at `http://localhost:8080` by default. Keep this terminal
running. If port 8080 is already in use, start the service on another port:

**Windows Command Prompt**

```bat
set "SERVER_PORT=8090"
mvn spring-boot:run
```

**macOS/Linux**

```sh
SERVER_PORT=8090 mvn spring-boot:run
```

### Start the client

Open a second terminal at the repository root. The following commands use the
first seeded Harbor Bank client. Its ID and API key are a demo pair stored in
`src/main/resources/data/clients.json`.

**Windows Command Prompt**

```bat
cd client
set "BANK_SERVICE_BASE_URL=http://localhost:8080"
set "BANK_SERVICE_CLIENT_ID=19ecb8c4-776c-4af0-ac41-b490b9408cf6"
set "BANK_SERVICE_API_KEY=714fe10a-f709-4a2d-be3a-2ad940447851"
mvn spring-boot:run
```

**macOS/Linux**

```sh
cd client
export BANK_SERVICE_BASE_URL=http://localhost:8080
export BANK_SERVICE_CLIENT_ID=19ecb8c4-776c-4af0-ac41-b490b9408cf6
export BANK_SERVICE_API_KEY=714fe10a-f709-4a2d-be3a-2ad940447851
mvn spring-boot:run
```

If you started the service on port 8090, set
`BANK_SERVICE_BASE_URL` to `http://localhost:8090` in the client terminal
instead. Keep the matching client ID and API key together. Open
`http://localhost:8081` after the client starts.

### Demo logins

The client creates the following demo users on startup. Sign in through the
client page; these users have personal, unlinked budgets and do not require
creating an account first.

| Username | Password | Seeded budgets |
| --- | --- | ---: |
| `harbor.alex` | `HarborDemo!2026` | 4 |
| `harbor.jordan` | `BudgetDemo#2026` | 3 |
| `harbor.taylor` | `SavingsDemo!26` | 3 |

These are public demo credentials. Do not reuse them for real users or
production deployments.

### Pre-populated service data

The service reads and writes JSON files under
`src/main/resources/data/`. They are pre-populated with:

- `clients.json`: the original five service clients plus three new clients
  with distinct IDs and API keys. Two new clients are named Harbor Bank; the
  third is Northstar Bank.
- `budgets.json`: 30 budgets, 10 for each of the three new clients. Budget IDs
  are unique; names may repeat.
- `categories.json`: 86 categories across those budgets. Each budget has
  between 0 and 5 categories.
- `purchases.json`: 204 purchases. Each seeded category has between 0 and 5
  purchases.

The three demo users are mapped to the ten budgets owned by the first seeded
Harbor Bank client, split 4/3/3. Budgets are associated with client IDs so the
two Harbor Bank clients remain isolated despite sharing a bank name. The
client database defaults to `client/data/`; demo-user seeding is enabled by
default. To disable demo-user seeding, set `BANK_CLIENT_DEMO_SEED_ENABLED` to
`false` before starting the client (`set "BANK_CLIENT_DEMO_SEED_ENABLED=false"`
in Windows Command Prompt, or `export BANK_CLIENT_DEMO_SEED_ENABLED=false` on
macOS/Linux).

The client tracks accounts and purchases locally; it does not connect to real
bank accounts or import bank transactions.

## Technology and tools

| Purpose | Technology |
| --- | --- |
| Application language and build | Java 21 and Maven |
| Continuous integration | GitHub Actions |
| Unit testing | JUnit 5 |
| Mocking | Mockito |
| API testing | Postman |
| Coverage reporting | JaCoCo |
| Style checks | Checkstyle |
| Static analysis | PMD |
| Project management | Google Docs |

The source tree uses the `com.collective404.budget` package. Application code,
tests, quality-check configuration, and Postman assets are organized under
`src/`, `config/`, and `postman/` respectively.

## API operations

The endpoint descriptions below specify the intended behavior. The exact JSON
schemas, query-parameter names, and datastore implementation will be defined
as the API is implemented.

### `POST /budget/category` — Create a category

The caller provides their API key in the `X-API-Key` header, a category name,
and a non-zero spending limit in dollars. A budget identifier may also be
provided. If there is no budget identifier, the service creates a budget for
the authenticated client along with the category.

- The name must be a non-empty, meaningful string (not only numbers or
  symbols), and must be unique within the budget, ignoring letter case.
- The limit must be numeric and have no more than two decimal places.
- A provided budget identifier must be valid and belong to the caller.
- A missing or unknown API key returns **401 Unauthorized**.
- On success, the service returns **201 Created** with the category ID, name,
  limit, amount spent (`$0.00` initially), and remaining amount (initially the
  limit).
- A duplicate category name in the same budget returns **409 Conflict** and
  includes the existing category ID. A failed request does not create or
  change a budget or category.

Creating a client's first category also creates their budget. The new category
appears in subsequent budget reports and increases the budget's total planned
spending by its limit.

### `PATCH /budget/category` — Update a category

The caller provides their client ID, budget ID, and category ID, along with a
new name, limit, or both. Omitted category fields keep their existing values.

- The category must belong to the specified client and budget.
- A supplied name must be non-empty and unique within that budget, ignoring
  letter case.
- A supplied limit must be a non-negative number. Reducing it below the
  amount already spent is allowed and can result in a negative remaining
  amount.
- On success, the service returns **200 OK** with the category ID, updated
  name, limit, and remaining amount.
- Repeating the same update returns **200 OK** with the same category
  information, provided no intervening purchases changed spending.
- Failed validation or lookup does not save changes.

The budget and category must already exist, normally through
`POST /budget/category`. A successful update is reflected in later budget
reports; changing the limit also changes the remaining amount, percentage
used, and total planned spending.

### `GET /budget/{budgetId}` — Retrieve a budget report

The caller sends the budget ID in the path and its API key in the
`X-API-Key` header. A client can only read its own budgets. The request has
no body and does not modify stored data.

The report includes every category in the budget, the budget's purchases,
and category and overall totals. For each category, the service calculates
the limit, amount spent, amount remaining, and percentage used. Purchase
amounts round up and remaining amounts round down. Remaining can go negative
when a category is overspent.

- Success returns **200 OK** with the report.
- A budget ID that isn't a UUID returns **400 Bad Request**.
- A missing or unknown API key returns **401 Unauthorized**.
- A budget that belongs to another client returns **403 Forbidden**.
- A budget ID that doesn't exist returns **404 Not Found**.
- An unavailable datastore returns **500 Internal Server Error**.

Errors return a JSON body with `status` (the HTTP code) and `reason`.
Repeating the request without intervening data changes returns the same
report. Updates, purchase creation, and purchase deletion are reflected in
subsequent reports.

### `POST /purchases` — Add a purchase

The caller provides a client ID, budget ID, purchase name, positive amount,
and category.

- The client and budget must exist, and the budget must belong to the client.
- The amount must be positive and have no more than two decimal places.
- A matching category in the budget receives the purchase. If no category
  matches, the purchase is counted under Miscellaneous.
- Success returns **201 Created** with the purchase ID, amount, and category.
- A budget must already exist, normally created by
  `POST /budget/category`. If validation or lookup fails, no purchase is
  added.

The purchase appears in subsequent budget reports and affects the spent,
remaining, and percentage-used amounts for its category (or Miscellaneous).

### `DELETE /purchases/{id}` — Delete a purchase

The caller provides their client ID and budget ID; the purchase ID is supplied
in the URL path. There is no request body.

- The purchase must exist and belong to the specified client and budget.
- Success returns **200 OK** with the deleted purchase's ID, amount, and
  category, plus recalculated spent and remaining amounts for that category
  (or Miscellaneous).
- Missing or invalid identifiers return **400 Bad Request**. A missing client,
  budget, or purchase, or a purchase that does not belong to the caller and
  budget, returns **404 Not Found** with an error message.
- An unavailable datastore returns **500 Internal Server Error** with an
  error message.
- The first successful request deletes the purchase. Repeating the request
  returns **404 Not Found** because the purchase is already gone; either way,
  the final state has no such purchase.

Deleting a purchase does not change the category record. Its totals are
recomputed from the remaining purchases and are reflected in future reports.
The purchase must have been created with `POST /purchases`, which requires an
existing budget.

## Build and quality checks

Run the Maven verification lifecycle from the repository root:

```sh
mvn clean verify
```

This compiles the project, runs JUnit tests, and runs the configured Checkstyle
and PMD checks. JaCoCo creates a coverage report during `verify` when tests
produce coverage data; the HTML report is written to
`target/site/jacoco/index.html`. The GitHub Actions workflow runs this command
for pushes and pull requests targeting `main`.

The companion client has a separate Maven build. Run its tests from the
repository root with `mvn -f client/pom.xml test`. For running both
applications and signing in with demo users, follow the TA run guide above.

## Repository layout

```text
.
├── .github/workflows/ci.yml
├── config/
│   ├── checkstyle.xml
│   └── pmd-ruleset.xml
├── postman/
│   ├── budget-api.postman_collection.json
│   └── local.postman_environment.json
├── reports/
│   ├── checkstyle/
│   ├── jacoco/
│   └── pmd/
├── src/
│   ├── main/
│   │   ├── resources/data/
│   │   └── java/com/collective404/budget/
│   └── test/java/com/collective404/budget/
├── client/
│   ├── src/
│   ├── pom.xml
│   └── README.md
├── pom.xml
├── AI Disclosure.md
└── README.md
```

## Project management
https://docs.google.com/document/d/1dPkSL4XaQVGZ5zcGpYZGQl_Knnaq3DSBpCox5-VsUOk/edit?usp=sharing
https://docs.google.com/document/d/1uZDjhIlISiKRD8sdttvT3Qu4LVy5J90z3o2sYpQBNwg/edit?usp=sharing
