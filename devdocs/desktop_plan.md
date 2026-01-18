# PiggyBank Desktop MVP Plan

## Overview

PiggyBank is an open-source personal finance application built with Kotlin Multiplatform and Compose Multiplatform. This document outlines the plan for the Desktop MVP, which aims to replicate the core functionality of GnuCash with a modern tech stack.

## Goals

1. **GnuCash-like Experience**: Familiar UI for users migrating from GnuCash
2. **Double-Entry Accounting**: Proper debit/credit system where every transaction balances
3. **Multi-Currency Support**: USD, EUR, and RSD with conversion to default currency (RSD)
4. **Hierarchical Accounts**: Tree structure with 5 root types (Assets, Liabilities, Equity, Income, Expenses)
5. **Spreadsheet-Style Register**: Inline editing with zebra rows and keyboard navigation

## Configuration

| Setting | Value |
|---------|-------|
| Default Currency | RSD (Serbian Dinar) |
| Date Format | YYYY-MM-DD (ISO 8601) |
| Authentication | Server-required (JWT) |
| First Launch | Auto-create default account hierarchy |

## Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                     Desktop App (Compose)                     │
├───────────────────────────────────────────────────────────────┤
│  UI Layer (composeApp/src/commonMain)                         │
│  ├── ui/                                                      │
│  │   ├── theme/           # Colors, typography, shapes        │
│  │   ├── screens/         # Login, Main screens               │
│  │   ├── components/      # Reusable UI components            │
│  │   └── viewmodel/       # State management                  │
│  └── di/                  # Koin dependency injection         │
├───────────────────────────────────────────────────────────────┤
│  Shared Layer (shared/src/commonMain)                         │
│  └── PiggyBankApi.kt      # HTTP client (already exists)      │
├───────────────────────────────────────────────────────────────┤
│  Server (Ktor)            # Backend (already exists)          │
└───────────────────────────────────────────────────────────────┘
```

## Screen Flow

```
App Launch
    │
    ▼
┌─────────────────┐
│  Login Screen   │  ← Username/password
│  [Login]        │    or "Register"
│  [Register]     │
└────────┬────────┘
         │ Success
         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Main Screen                             │
├─────────────────────────────────────────────────────────────┤
│ Menu Bar: File | Edit | View | Transaction | Reports | Help │
├─────────────────────────────────────────────────────────────┤
│ Toolbar: [Save] [New Acct] [Delete] │ [New Txn] [Transfer]  │
├─────────────────────────────────────────────────────────────┤
│ Tabs: [Accounts] [Checking ✕] [Salary ✕]                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   TAB CONTENT (Account Tree or Account Register)             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Phases

### Phase 1: Project Setup
- Create UI package structure in `composeApp/src/commonMain`
- Set up Koin dependency injection
- Define theme colors (GnuCash-style: zebra green, yellow selection)
- Create base App composable with navigation

### Phase 2: Login Screen
- Username and password text fields
- Login button → POST `/api/auth/login`
- Register button → POST `/api/auth/register`
- Error display for failed attempts
- Store JWT token for subsequent requests

### Phase 3: Main Screen Layout
- Menu bar with File, Edit, View, Transaction, Reports, Help
- Toolbar with common actions (Save, New Account, Delete, etc.)
- Tab system with closeable tabs
- "Accounts" tab always open (non-closeable)

### Phase 4: Account Tree View
- Fetch from GET `/api/accounts/tree`
- Expandable/collapsible tree nodes (▶/▼)
- Columns: Account Name, Description, Currency, Balance
- Double-click row → open Account Register in new tab
- Show aggregate balance in RSD (default currency)

### Phase 5: Account Register (Read-only)
- Fetch transactions from GET `/api/accounts/{id}/transactions`
- Spreadsheet-style table
- Columns: Date, Num, Description, Transfer, R, Deposit, Withdrawal, Balance
- Zebra row styling (alternating light green / white)
- Yellow highlight for selected row
- Running balance calculation

### Phase 6: Inline Editing
- Click cell to edit
- Tab to move between cells
- Enter to save row
- Empty row at bottom for new transactions
- Create simple 2-split transactions
- Auto-complete for Transfer account field

### Phase 7: Split Transactions
- "Show Splits" toggle button
- Expandable rows showing all splits
- Multi-split entry UI
- Validate splits sum to zero before save

### Phase 8: Multi-Currency Display
- Show currency symbol with amounts (e.g., $100, €50, 1000 RSD)
- Convert to RSD for aggregate balances
- Use exchange rates from database (or hardcoded for MVP)

## UI Styling Reference

### Colors (GnuCash-inspired)
```kotlin
// Zebra rows
val zebraLight = Color(0xFFE8F5E9)  // Light green
val zebraWhite = Color(0xFFFFFFFF)  // White

// Selection
val selectedRow = Color(0xFFFFFF99) // Yellow

// Header
val headerBackground = Color(0xFF4CAF50) // Green
val headerText = Color(0xFFFFFFFF)        // White

// Borders
val cellBorder = Color(0xFFBDBDBD)        // Gray
```

### Account Register Columns
| Column | Width | Editable | Description |
|--------|-------|----------|-------------|
| Date | 100dp | Yes | YYYY-MM-DD format |
| Num | 60dp | Yes | Check/invoice number |
| Description | Flex | Yes | Transaction description |
| Transfer | 200dp | Yes | Counter account (or "-- Split --") |
| R | 30dp | No | Reconcile status (n/c/y) |
| Deposit | 100dp | Yes | Money in (for this account) |
| Withdrawal | 100dp | Yes | Money out (for this account) |
| Balance | 100dp | No | Running balance |

## Double-Entry Accounting Rules

### Account Types and Normal Balances
| Type | Normal Balance | Increases With | Decreases With |
|------|----------------|----------------|----------------|
| Asset | Debit | Debit (+) | Credit (-) |
| Liability | Credit | Credit (-) | Debit (+) |
| Equity | Credit | Credit (-) | Debit (+) |
| Income | Credit | Credit (-) | Debit (+) |
| Expense | Debit | Debit (+) | Credit (-) |

### Transaction Rules
1. Every transaction has 2+ splits
2. All splits must sum to zero
3. Positive amount = Debit, Negative amount = Credit
4. For asset accounts: Deposit = Debit, Withdrawal = Credit

### Example: Salary Deposit with Tax Deduction
```
Transaction: "January Salary"
Splits:
  - Assets:Checking    +2500.00 (Debit - increases asset)
  - Expenses:Tax        +400.00 (Debit - increases expense)
  - Expenses:Insurance  +100.00 (Debit - increases expense)
  - Income:Salary      -3000.00 (Credit - increases income)
  
Sum: 2500 + 400 + 100 - 3000 = 0 ✓
```

## Future Enhancements (Post-MVP)

- [ ] Account reconciliation workflow
- [ ] Search and filter transactions
- [ ] Reports (Balance Sheet, Income Statement, Cash Flow)
- [ ] Scheduled/recurring transactions
- [ ] Import from GnuCash/CSV/OFX
- [ ] Data export
- [ ] Budgeting
- [ ] Multi-user support
- [ ] Offline mode with sync
