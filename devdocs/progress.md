# PiggyBank - Development Progress

## Current Status

**Phase**: 6 - UI Implementation (In Progress)  
**Focus**: Desktop UI with Compose Multiplatform  
**Last Updated**: 2026-01-18

---

## Phase 1: Foundation

### Infrastructure Setup
- [x] Create Docker Compose file for PostgreSQL
- [x] Add required dependencies to gradle/libs.versions.toml
- [x] Configure server module with Exposed ORM
- [x] Set up database connection and configuration
- [x] Create database tables with Exposed

### Server Structure
- [x] Set up Ktor application structure
- [x] Configure content negotiation (JSON serialization)
- [x] Add health check endpoint
- [x] Set up logging configuration
- [x] Create basic error handling (StatusPages plugin)

### Authentication
- [x] Create User table and entity
- [x] Implement password hashing with BCrypt
- [x] Set up JWT configuration
- [x] Implement registration endpoint
- [x] Implement login endpoint
- [x] Add JWT authentication middleware
- [ ] Implement token refresh endpoint

### Local Database (SQLDelight)
- [ ] Add SQLDelight plugin and dependencies
- [ ] Create local database schema
- [ ] Generate SQLDelight queries

---

## Phase 2: Core Accounting

### Account Management
- [x] Create Account table and entity
- [x] Implement account CRUD endpoints
- [x] Add account hierarchy support (parent-child)
- [x] Create default accounts on user registration
- [x] Implement account balance calculation
- [x] Implement account tree view endpoint

### Transaction Management
- [x] Create Transaction and Split tables
- [x] Implement transaction CRUD endpoints
- [x] Add double-entry validation (splits sum to zero)
- [x] Implement account register (transactions for account)
- [x] Add date range filtering

---

## Phase 3: Multi-Currency

### Exchange Rates
- [x] Create ExchangeRate table and entity
- [ ] Implement exchange rate CRUD endpoints
- [ ] Add currency conversion logic
- [ ] Support multi-currency balance display

---

## Phase 4: Recurring Transactions

### Recurring Transaction Templates
- [x] Create RecurringTransaction and RecurringSplit tables
- [ ] Implement recurring transaction CRUD endpoints
- [ ] Add frequency scheduling logic
- [ ] Implement auto-generation of due transactions
- [ ] Create upcoming transactions view endpoint

---

## Phase 5: Reports & Sync

### Reports
- [ ] Implement Balance Sheet report
- [ ] Implement Income Statement report
- [ ] Add date range selection for reports

### Sync Engine
- [ ] Design sync protocol
- [ ] Implement change tracking
- [ ] Create sync endpoints
- [ ] Implement conflict resolution (last-write-wins)

---

## Phase 6: UI Implementation

### Theme & Styling
- [x] Set up Material 3 theme
- [x] Implement light mode
- [x] Implement dark mode
- [x] Create theme toggle

### Screens
- [x] Login screen
- [x] Registration screen
- [x] Dashboard/Overview screen
- [x] Account list screen (tree view)
- [x] Account detail screen
- [x] Transaction list screen (register)
- [x] Transaction entry form
- [ ] Recurring transactions screen
- [ ] Exchange rates screen
- [ ] Balance Sheet report screen
- [ ] Income Statement report screen
- [x] Settings screen

### Shared Module
- [x] Add Ktor Client dependencies
- [x] Create domain models (matching server DTOs)
- [x] Create API client (PiggyBankApi)
- [x] Set up API result handling

---

## Completed Items

### Planning
- [x] Define project goals and scope
- [x] Design data model
- [x] Plan architecture
- [x] Choose technology stack
- [x] Create devdocs/plan.md
- [x] Create devdocs/progress.md

### Infrastructure (2026-01-17)
- [x] docker-compose.yml - PostgreSQL 16 Alpine setup
- [x] gradle/libs.versions.toml - Added Exposed, PostgreSQL, HikariCP, BCrypt, Koin, kotlinx-datetime, kotlinx-serialization
- [x] server/build.gradle.kts - Updated with all server dependencies
- [x] gradle.properties - Set Java 17 for Gradle (Kotlin 2.3 compatibility)

### Database Tables (2026-01-17)
- [x] Users table (id, username, email, passwordHash, timestamps)
- [x] Accounts table (id, userId, parentId, name, fullName, type, currency, placeholder, timestamps)
- [x] Transactions table (id, userId, date, description, notes, timestamps)
- [x] Splits table (id, transactionId, accountId, amount, currency, memo)
- [x] ExchangeRates table (id, fromCurrency, toCurrency, rate, date, source)
- [x] RecurringTransactions table (id, userId, name, frequency, interval, dates, isActive)
- [x] RecurringSplits table (id, recurringTransactionId, accountId, amount, currency, memo)

### Server Code (2026-01-17)
- [x] config/DatabaseConfig.kt - HikariCP connection pool, schema creation
- [x] config/JwtConfig.kt - JWT token generation and verification
- [x] plugins/Serialization.kt - JSON content negotiation
- [x] plugins/Security.kt - JWT authentication setup
- [x] plugins/StatusPages.kt - Error handling and custom exceptions
- [x] plugins/HTTP.kt - CORS and call logging
- [x] service/UserService.kt - Registration, login, user management
- [x] service/AccountService.kt - Account CRUD, hierarchy, default accounts
- [x] service/TransactionService.kt - Transaction CRUD, double-entry validation
- [x] api/routes/AuthRoutes.kt - /api/auth endpoints
- [x] api/routes/AccountRoutes.kt - /api/accounts endpoints
- [x] api/routes/TransactionRoutes.kt - /api/transactions endpoints
- [x] Application.kt - Main entry point, wiring everything together

### DTOs (2026-01-17)
- [x] AuthDtos.kt - RegisterRequest, LoginRequest, AuthResponse, UserResponse
- [x] AccountDtos.kt - CreateAccountRequest, AccountResponse, AccountTreeNode
- [x] TransactionDtos.kt - CreateTransactionRequest, SplitRequest, TransactionResponse, AccountRegisterEntry
- [x] ExchangeRateDtos.kt - CreateExchangeRateRequest, ConvertCurrencyRequest
- [x] RecurringTransactionDtos.kt - CreateRecurringTransactionRequest, RecurringTransactionResponse
- [x] ReportDtos.kt - BalanceSheetResponse, IncomeStatementResponse

### Desktop UI (2026-01-18)
- [x] shared/build.gradle.kts - Added Ktor Client, kotlinx-serialization, kotlinx-datetime
- [x] shared/domain/model/ - Domain models matching server DTOs
- [x] shared/data/api/PiggyBankApi.kt - HTTP client for server communication
- [x] shared/data/api/ApiResult.kt - Result wrapper for API calls
- [x] composeApp/build.gradle.kts - Added Material Icons Extended, kotlinx-datetime
- [x] ui/theme/Theme.kt - Material 3 theme with light/dark mode and accounting colors
- [x] ui/navigation/AppState.kt - Navigation and app state management
- [x] ui/components/CommonComponents.kt - Reusable UI components
- [x] ui/screens/auth/LoginScreen.kt - User login
- [x] ui/screens/auth/RegisterScreen.kt - User registration
- [x] ui/screens/dashboard/DashboardScreen.kt - Main dashboard with account summary
- [x] ui/screens/accounts/AccountsScreen.kt - Hierarchical account tree view
- [x] ui/screens/accounts/AccountDetailScreen.kt - Account details and register
- [x] ui/screens/transactions/TransactionsScreen.kt - Transaction list
- [x] ui/screens/transactions/TransactionDetailScreen.kt - Transaction details
- [x] ui/screens/transactions/TransactionEntryScreen.kt - Create/edit transactions
- [x] ui/screens/settings/SettingsScreen.kt - Theme toggle, logout

---

## Notes & Decisions

### 2026-01-18
- Desktop UI implementation complete with all core screens
- Connected to PostgreSQL database with credentials: myuser/mysecretpassword/piggybank
- Using Material 3 with custom green theme (money/prosperity colors)
- Form-based transaction entry with double-entry validation
- Account tree view with expand/collapse functionality
- Running balance in account register view

### 2026-01-17
- Starting with server/database layer (Ktor + Exposed + PostgreSQL)
- Desktop (JVM) will be the first client platform
- Using form-based transaction entry (not spreadsheet-style like GnuCash)
- Multi-user support from the start with JWT authentication
- Three currencies supported: USD, EUR, RSD
- Java 17 required for Gradle (Kotlin 2.3.0 doesn't support Java 25 yet)
- Using explicit imports to avoid java.util.Currency conflict with our Currency enum

---

## Blockers & Issues

### Resolved
- **Java 25 incompatibility**: Kotlin 2.3.0 doesn't support Java 25. Fixed by setting `org.gradle.java.home` to Java 17 in gradle.properties.
- **Currency name conflict**: java.util.Currency conflicts with our Currency enum. Fixed by using explicit imports instead of wildcard imports.
- **Compose Material 2/3 ambiguity**: Some Material components had ambiguous overloads. Fixed by using explicit Material 3 imports.

### Current
- None

---

## Next Steps

1. ~~Create Docker Compose file for PostgreSQL~~ ✓
2. ~~Add all required dependencies to libs.versions.toml~~ ✓
3. ~~Set up Exposed ORM in server module~~ ✓
4. ~~Create database tables~~ ✓
5. ~~Implement authentication endpoints~~ ✓
6. ~~Fix remaining compilation errors~~ ✓
7. ~~Server build successful~~ ✓
8. ~~Implement Desktop UI~~ ✓
9. Test the application end-to-end
10. Implement remaining endpoints (Exchange Rates, Recurring Transactions, Reports)
11. Add remaining UI screens (Recurring, Exchange Rates, Reports)
12. Start SQLDelight setup for local database (offline support)

---

## How to Run

### Start PostgreSQL
```bash
# Using Docker (or use local PostgreSQL)
docker-compose up -d
```

### Run Server
```bash
.\gradlew.bat :server:run
```

### Run Desktop App
```bash
.\gradlew.bat :composeApp:run
```
