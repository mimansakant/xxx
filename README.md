# 404 Collective Budget Service

The Budget Service is a proposed API for tracking a client's budget, spending
categories, and purchases. Clients can create categories with spending limits,
record and delete purchases, and retrieve a report comparing spending with
those limits.

> **Project status:** This repository currently contains the project scaffold
> and API specification. The Java source and test files are stubs; the endpoint
> behaviors below describe the intended API and are not yet implemented.

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

The caller provides a client identifier, a category name, and a non-zero
spending limit in dollars. A budget identifier may also be provided. If there
is no budget identifier, the service creates a budget for the client along
with the category.

- The name must be a non-empty, meaningful string (not only numbers or
  symbols), and must be unique within the budget, ignoring letter case.
- The limit must be numeric and have no more than two decimal places.
- A provided budget identifier must be valid and belong to the caller.
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

### `GET /budget` — Retrieve a budget report

The caller provides their client ID. The request has no body and does not
modify stored data.

The report includes every category (including the budget's automatically
available **Miscellaneous** category), the client's current purchases, and
category and overall totals. For each category, the service calculates the
limit, amount spent, amount remaining, and percentage used from the client's
purchases. Purchases without a matching category are counted under
Miscellaneous.

- Success returns **200 OK** with the report.
- A missing client ID returns **400 Bad Request**.
- A client with no budget returns **404 Not Found** with an error message.
- An unavailable datastore returns **500 Internal Server Error** with an
  error message.
- Repeating the request without intervening data changes returns the same
  report. The operation has no side effects.

The budget is created when its first category is created. Updates, purchase
creation, and purchase deletion are reflected in subsequent reports.

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

The current source and test files are empty stubs, so there are no implemented
endpoints or meaningful test coverage yet. Add unit tests as functionality is
implemented. Use the Postman collection under `postman/` for network-level API
tests once the API is running.

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
├── src/
│   ├── main/
│   │   ├── data/
│   │   └── java/com/collective404/budget/
│   └── test/java/com/collective404/budget/
├── pom.xml
└── README.md
```

## Project management
https://docs.google.com/document/d/1dPkSL4XaQVGZ5zcGpYZGQl_Knnaq3DSBpCox5-VsUOk/edit?usp=sharing
https://docs.google.com/document/d/1uZDjhIlISiKRD8sdttvT3Qu4LVy5J90z3o2sYpQBNwg/edit?usp=sharing

## AI Disclosure
AI was used in our project for the following:
- Creating automated test suites: prompts were to create code for specified test case branches
- Create the baseline folder structure with file stubs
- Create the original checkstyle.xml, which was then edited to follow that of the Miniproject
- Create BudgetDataStore. Prompted to create a class for file reads and writes, resulting code edited for errors