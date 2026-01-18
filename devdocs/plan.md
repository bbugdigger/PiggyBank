# PiggyBank - Project Plan

## Overview

PiggyBank is a personal finance management application inspired by GnuCash, built with Kotlin Multiplatform and Compose Multiplatform. It implements a proper double-entry accounting system to help users track their finances across multiple currencies.

## Goals

### Primary Goals
- **Double-Entry Accounting**: Every transaction must balance (sum of all splits = 0)
- **Multi-Currency Support**: USD, EUR, and Serbian Dinar (RSD) with exchange rate tracking
- **Offline-First**: Local SQLite database with PostgreSQL sync for self-hosting
- **Multi-User**: Authentication and separate account books per user
- **Cross-Platform**: Desktop (JVM) first, then Android, iOS, and Web

### Non-Goals (for MVP)
- Budgeting features
- Investment portfolio tracking
- Bank import (OFX/QIF files)
- Subscription management
- Mobile-specific features (widgets, notifications)

## Core Accounting Concepts

Based on research from:
- [Accounting for Computer Scientists](https://martin.kleppmann.com/2011/03/07/accounting-for-computer-scientists.html)
- [Accounting for Developers](https://www.moderntreasury.com/journal/accounting-for-developers-part-i)
- [Beancount Double-Entry Method](https://beancount.github.io/docs/the_double_entry_counting_method.html)

### Account Types (5 Root Accounts per User)
| Type | Normal Balance | Description |
|------|----------------|-------------|
| **Assets** | Positive (+) | Things you own (bank accounts, cash, investments) |
| **Liabilities** | Negative (-) | Things you owe (credit cards, loans, mortgages) |
| **Equity** | Negative (-) | Net worth (retained earnings, opening balances) |
| **Income** | Negative (-) | Money received (salary, interest, dividends) |
| **Expenses** | Positive (+) | Money spent (food, rent, utilities) |

### The Fundamental Rule
> **The sum of all postings (splits) in a transaction must equal zero.**

Example: Buying groceries for $50 with a debit card
```
Transaction: "Groceries at Whole Foods" - 2024-01-15
  Split 1: Expenses:Food:Groceries    +50.00 USD (debit)
  Split 2: Assets:Bank:Checking       -50.00 USD (credit)
  ─────────────────────────────────────────────
  Total:                                0.00 USD ✓
```

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                       composeApp (UI)                               │
├─────────────────────────────────────────────────────────────────────┤
│  Screens:                                                           │
│  ├── Auth: Login, Register                                          │
│  ├── Dashboard: Overview, quick actions                             │
│  ├── Accounts: Tree view, account details                           │
│  ├── Transactions: Entry form, register list                        │
│  ├── Recurring: Templates list, create/edit                         │
│  ├── ExchangeRates: View/edit rates                                 │
│  └── Reports: Balance Sheet, Income Statement                       │
│                                                                     │
│  Theme: MaterialTheme with Light/Dark support                       │
└─────────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────────┐
│                       shared (Domain + Data)                        │
├─────────────────────────────────────────────────────────────────────┤
│  Domain:                                                            │
│  ├── Entities (User, Account, Transaction, Split, etc.)             │
│  ├── UseCases (CreateTransaction, GetBalance, ConvertCurrency...)   │
│  └── Repositories (interfaces)                                      │
│                                                                     │
│  Data:                                                              │
│  ├── LocalDataSource (SQLDelight)                                   │
│  ├── RemoteDataSource (Ktor Client)                                 │
│  ├── SyncManager                                                    │
│  └── Repository implementations                                     │
└─────────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────────┐
│                       server (Ktor)                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Routes:                                                            │
│  ├── POST /auth/register, POST /auth/login                          │
│  ├── GET/POST/PUT/DELETE /accounts                                  │
│  ├── GET/POST/PUT/DELETE /transactions                              │
│  ├── GET/POST/PUT/DELETE /recurring-transactions                    │
│  ├── GET/POST /exchange-rates                                       │
│  └── GET /reports/balance-sheet, /reports/income-statement          │
│                                                                     │
│  Database: PostgreSQL via Exposed ORM                               │
└─────────────────────────────────────────────────────────────────────┘
```

## Data Model

### Core Entities

```
User
├── id: UUID
├── username: String (unique)
├── passwordHash: String
├── email: String?
└── createdAt: Instant

Account
├── id: UUID
├── userId: UUID (FK -> User)
├── parentId: UUID? (FK -> Account, for hierarchy)
├── name: String
├── type: AccountType (ASSET, LIABILITY, EQUITY, INCOME, EXPENSE)
├── currency: Currency (USD, EUR, RSD)
├── description: String?
├── placeholder: Boolean (true = container only, can't hold postings)
└── createdAt: Instant

Transaction
├── id: UUID
├── userId: UUID (FK -> User)
├── date: LocalDate
├── description: String
├── createdAt: Instant
└── updatedAt: Instant

Split (Posting)
├── id: UUID
├── transactionId: UUID (FK -> Transaction)
├── accountId: UUID (FK -> Account)
├── amount: BigDecimal (+ for debit, - for credit)
├── currency: Currency
└── memo: String?

ExchangeRate
├── id: UUID
├── fromCurrency: Currency
├── toCurrency: Currency
├── rate: BigDecimal
├── date: LocalDate
└── source: String? (manual, API, etc.)

RecurringTransaction
├── id: UUID
├── userId: UUID (FK -> User)
├── name: String
├── description: String
├── frequency: Frequency (DAILY, WEEKLY, MONTHLY, YEARLY)
├── interval: Int
├── startDate: LocalDate
├── endDate: LocalDate?
├── nextDueDate: LocalDate
├── lastProcessedDate: LocalDate?
└── isActive: Boolean

RecurringSplit
├── id: UUID
├── recurringTransactionId: UUID (FK -> RecurringTransaction)
├── accountId: UUID (FK -> Account)
├── amount: BigDecimal
├── currency: Currency
└── memo: String?
```

### Key Constraints
- `SUM(splits.amount) = 0` for every transaction
- Account names unique within same parent for same user
- Username unique across all users

## Technology Stack

| Layer | Technology |
|-------|------------|
| UI | Compose Multiplatform |
| Local DB | SQLDelight |
| Server DB | PostgreSQL (via Docker) |
| Server ORM | Exposed |
| Server Framework | Ktor |
| Auth | JWT (JSON Web Tokens) |
| HTTP Client | Ktor Client |
| DI | Koin |
| Date/Time | kotlinx-datetime |
| Serialization | kotlinx-serialization |
| Password Hashing | BCrypt |

## Implementation Phases

### Phase 1: Foundation (Week 1-2)
1. Docker Compose setup for PostgreSQL
2. Exposed ORM setup and database tables
3. Basic Ktor server structure with health check
4. SQLDelight setup for local database
5. User registration and login with JWT authentication
6. Password hashing with BCrypt

### Phase 2: Core Accounting (Week 3-4)
7. Account CRUD operations
8. Account hierarchy (parent-child relationships)
9. Default accounts creation on user signup
10. Transaction CRUD with splits
11. Double-entry validation (splits must sum to zero)
12. Account register view (transactions for an account)

### Phase 3: Multi-Currency (Week 5)
13. Exchange rate entity and storage
14. Manual exchange rate entry
15. Currency conversion in transaction splits
16. Multi-currency balance calculations

### Phase 4: Recurring Transactions (Week 6)
17. Recurring transaction templates
18. Frequency scheduling (daily, weekly, monthly, yearly)
19. Auto-generation of due transactions
20. Upcoming transactions view

### Phase 5: Reports & Sync (Week 7-8)
21. Balance Sheet report
22. Income Statement report
23. Offline-first sync engine
24. Conflict resolution (last-write-wins for MVP)

### Phase 6: UI Polish (Week 9)
25. Light/Dark theme implementation
26. Account tree view with expand/collapse
27. Form-based transaction entry UI
28. Error handling and user feedback

## API Endpoints

### Authentication
- `POST /api/auth/register` - Create new user
- `POST /api/auth/login` - Login and receive JWT
- `POST /api/auth/refresh` - Refresh JWT token

### Accounts
- `GET /api/accounts` - List all accounts for user
- `GET /api/accounts/{id}` - Get single account
- `POST /api/accounts` - Create account
- `PUT /api/accounts/{id}` - Update account
- `DELETE /api/accounts/{id}` - Delete account (if no transactions)
- `GET /api/accounts/{id}/balance` - Get account balance

### Transactions
- `GET /api/transactions` - List transactions (with filters)
- `GET /api/transactions/{id}` - Get single transaction with splits
- `POST /api/transactions` - Create transaction
- `PUT /api/transactions/{id}` - Update transaction
- `DELETE /api/transactions/{id}` - Delete transaction
- `GET /api/accounts/{id}/transactions` - Get transactions for account

### Recurring Transactions
- `GET /api/recurring-transactions` - List recurring transactions
- `GET /api/recurring-transactions/{id}` - Get single recurring transaction
- `POST /api/recurring-transactions` - Create recurring transaction
- `PUT /api/recurring-transactions/{id}` - Update recurring transaction
- `DELETE /api/recurring-transactions/{id}` - Delete recurring transaction
- `POST /api/recurring-transactions/{id}/generate` - Generate pending transactions

### Exchange Rates
- `GET /api/exchange-rates` - List exchange rates
- `POST /api/exchange-rates` - Add exchange rate
- `GET /api/exchange-rates/convert` - Convert amount between currencies

### Reports
- `GET /api/reports/balance-sheet` - Balance sheet for date
- `GET /api/reports/income-statement` - Income statement for date range

## Currencies

| Code | Name | Symbol |
|------|------|--------|
| USD | US Dollar | $ |
| EUR | Euro | € |
| RSD | Serbian Dinar | RSD |

## Default Accounts Created for New Users

```
Assets (placeholder)
├── Cash
├── Bank
│   └── Checking
└── Investments

Liabilities (placeholder)
├── Credit Card
└── Loans

Equity (placeholder)
├── Opening Balances
└── Retained Earnings

Income (placeholder)
├── Salary
├── Interest
└── Other Income

Expenses (placeholder)
├── Food
│   ├── Groceries
│   └── Restaurants
├── Housing
│   ├── Rent
│   └── Utilities
├── Transportation
└── Entertainment
```

## References

- [GnuCash Manual](https://www.gnucash.org/docs/v5/C/gnucash-manual.html)
- [Accounting for Computer Scientists](https://martin.kleppmann.com/2011/03/07/accounting-for-computer-scientists.html)
- [Accounting for Developers Part I](https://www.moderntreasury.com/journal/accounting-for-developers-part-i)
- [Double-Entry Bookkeeping for Programmers](https://www.balanced.software/double-entry-bookkeeping-for-programmers/)
- [Beancount Double-Entry Method](https://beancount.github.io/docs/the_double_entry_counting_method.html)
