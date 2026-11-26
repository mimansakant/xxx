# Harbor bank budgeting client

This is a separate Spring Boot application and browser client for the budget service. It adds app-user sign-up/sign-in, user-owned tracking accounts, account-linked or personal budgets, purchase categorization and editing, and date/account/category filtered views. Accounts and transactions are entered into this app; this version does not connect to a bank or import bank transactions.

## Requirements

- Java 21
- Maven
- A running budget service
- A pre-provisioned service API key and its matching service client ID

The service does not issue API keys. Configure one unique key per deployed client instance on the server; do not put it in `app.js`, the browser, or a checked-in properties file.

## Demo logins

The client creates these demo users and their personal (not account-linked) budget assignments when it starts. Passwords are stored as PBKDF2 hashes in the client database.

| Username | Password | Seeded budgets |
| --- | --- | ---: |
| `harbor.alex` | `HarborDemo!2026` | 4 |
| `harbor.jordan` | `BudgetDemo#2026` | 3 |
| `harbor.taylor` | `SavingsDemo!26` | 3 |

These are public demo credentials, not production credentials. The budgets belong to the first seeded Harbor Bank service client (`19ecb8c4-776c-4af0-ac41-b490b9408cf6`). To view them, configure `BANK_SERVICE_CLIENT_ID` to that ID and `BANK_SERVICE_API_KEY` to the matching key in `src/main/resources/data/clients.json`. The other seeded service clients and their budgets are available for service API testing, but are not assigned to these client users.

## Run locally

Start the existing budget service from the repository root:

```powershell
mvn spring-boot:run
```

In another PowerShell terminal, configure the client instance and start it:

```powershell
Set-Location client
$env:BANK_SERVICE_BASE_URL = "http://localhost:8080"
$env:BANK_SERVICE_API_KEY = "<pre-provisioned-api-key>"
$env:BANK_SERVICE_CLIENT_ID = "<matching-service-client-id>"
mvn spring-boot:run
```

Open `http://localhost:8081`. The client serves its own UI and makes budget-service calls from the companion backend, so the service API key is never sent to the browser.

If budget views report HTTP 401, the service at `BANK_SERVICE_BASE_URL` did
not recognize the configured API key. Start the service from the repository
root, make sure the API key and client ID come from the same `clients.json`
entry, and restart the client after changing environment variables. If port
8080 is occupied, run the service on another port and point
`BANK_SERVICE_BASE_URL` at that port.

Optional settings:

- `BANK_CLIENT_PORT` changes the client port (default `8081`).
- `BANK_CLIENT_DATABASE_URL` changes the H2 database URL (default file-backed database under `client/data/`).
- `BANK_CLIENT_DATABASE_USERNAME` and `BANK_CLIENT_DATABASE_PASSWORD` configure the local database login.
- `BANK_CLIENT_DEMO_SEED_ENABLED=false` disables creation of the public demo users and their budget mappings.

## How data is handled

- User passwords are PBKDF2-hashed. Sign-in tokens are 12-hour in-memory bearer sessions; restarting the client signs users out.
- User profiles, tracking accounts, budget ownership mappings, and purchase overrides persist in the client database. Protect the database file and backups as sensitive financial data.
- The service API owns budget/category creation and purchase creation/deletion. The client stores account associations and user edits that the service API cannot represent, then applies them when composing reports.
- A new budget starts with one category so the service can create it. Add more categories from the Budgets view.
- Automatic category assignment uses purchase-description keywords and the budget's existing categories; users can change the suggested category later.
- Purchases in an account-linked budget allow category edits only. Personal-budget purchases can also have their name, amount, and date edited.
- Purchases without an account use a personal budget. Date, account, budget, and category filters are applied by the companion app.
- The service API currently does not expose bank authentication, account records, purchase updates, or date-filtered reports. The client implements the app-level behavior locally and does not claim to synchronize with a financial institution.

This is a local tracking client, not a production bank-integration product. Use HTTPS, a managed/encrypted database and backups, secret management, and operational controls before deploying real customer financial data.
