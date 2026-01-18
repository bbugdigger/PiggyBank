# PiggyBank Desktop MVP - Progress Tracker

## Current Status: Phase 8 Complete - Desktop MVP Ready!

Last Updated: 2026-01-18

---

## Phase 1: Project Setup - COMPLETE
- [x] Create UI package structure (`composeApp/src/commonMain/kotlin/.../ui/`)
- [x] Set up Koin dependency injection modules
- [x] Define theme colors (zebra green, yellow selection, header colors)
- [x] Create PiggyBankTheme composable
- [x] Create base App composable with navigation state
- [x] Wire up App() in jvmMain entry point

## Phase 2: Login Screen - COMPLETE
- [x] Create LoginScreen composable
- [x] Username text field with validation
- [x] Password text field (obscured)
- [x] Login button with loading state
- [x] Register button
- [x] Error message display
- [x] Connect to PiggyBankApi.login()
- [x] Connect to PiggyBankApi.register()
- [x] Navigate to MainScreen on success
- [x] Store auth token in memory

## Phase 3: Main Screen Layout - COMPLETE
- [x] Create MainScreen composable
- [x] Top app bar with title and actions
- [x] Toolbar with icon buttons (New Account, Expand/Collapse All)
- [x] Tab bar component
- [x] Tab state management (open tabs, active tab)
- [x] Closeable tabs (except Accounts)
- [x] Content area that renders based on active tab

## Phase 4: Account Tree View - COMPLETE
- [x] Create AccountTreeView composable
- [x] Fetch accounts from API on load
- [x] Tree node component with expand/collapse
- [x] Indentation based on depth
- [x] Display columns: Name, Description, Currency, Balance
- [x] Aggregate balance for parent accounts (server-side)
- [x] Color-coded by account type
- [x] Double-click handler to open register tab
- [x] Loading and error states

## Phase 5: Account Register (Read-only) - COMPLETE
- [x] Create AccountRegister composable
- [x] Fetch transactions for account
- [x] Table header row with column labels
- [x] Zebra row styling (alternating colors)
- [x] Selected row highlighting (yellow)
- [x] Display columns: Date, Num, Description, Transfer, R, Deposit, Withdrawal, Balance
- [x] Running balance display
- [x] Handle "-- Split Transaction --" display for multi-split transactions
- [x] Voided transaction styling (strikethrough)
- [x] Scroll behavior via LazyColumn
- [x] Loading and error states
- [x] Empty row at bottom for new transactions (placeholder)

## Phase 6: Inline Editing - COMPLETE
- [x] Editable cell component (EditableCell, AmountEditableCell)
- [x] Click to enter edit mode
- [x] Tab navigation between cells
- [x] Enter to save row
- [x] Escape to cancel edit
- [x] Empty row at bottom for new transactions (fully editable)
- [x] Date input (text field, defaults to today)
- [x] Account selector/autocomplete for Transfer field (AccountSelectorCell)
- [x] Numeric input for Deposit/Withdrawal (AmountEditableCell)
- [x] Create transaction via API (createSimpleTransaction)
- [x] Validation (requires date, description, transfer account, and amount)

## Phase 7: Split Transactions - COMPLETE
- [x] "Show Splits" toggle button in toolbar (context-aware, only shows on register tabs)
- [x] Split view header with different columns (Date, Action, Memo, Account, R, Deposit, Withdrawal)
- [x] Transaction header row in split view
- [x] Split detail rows showing all splits
- [x] Fetch transaction details on demand
- [x] Visual distinction between current account's split and others
- [x] Reconcile status display on splits

## Phase 8: Multi-Currency Display - COMPLETE
- [x] Currency formatting utility (CurrencyUtils.kt)
- [x] USD format: $1,234.56
- [x] EUR format: 1,234.56 EUR
- [x] RSD format: 1,234.56 RSD
- [x] Exchange rate conversion (hardcoded rates for MVP: 1 EUR = 117.5 RSD, 1 USD = 108 RSD)
- [x] Convert/format amounts cross-platform (no java.math.BigDecimal)
- [x] Display native currency in account tree
- [x] Proper formatting in account register

---

## Files Created

```
composeApp/src/commonMain/kotlin/com/bugdigger/piggybank/
├── App.kt                           # Main app composable with navigation
├── di/
│   └── AppModule.kt                 # Koin DI module
└── ui/
    ├── theme/
    │   ├── Color.kt                 # Color definitions (GnuCash-style)
    │   ├── Type.kt                  # Typography + MonospaceAmount
    │   └── Theme.kt                 # PiggyBankTheme composable
    ├── screens/
    │   ├── LoginScreen.kt           # Login/register screen
    │   └── MainScreen.kt            # Main tabbed interface with TabBar
    ├── components/
    │   ├── AccountTreeView.kt       # Hierarchical account tree
    │   ├── AccountRegister.kt       # Spreadsheet transaction view
    │   └── EditableCells.kt         # Inline editing components
    ├── util/
    │   └── CurrencyUtils.kt         # Currency formatting & conversion
    └── viewmodel/
        ├── AuthViewModel.kt         # Login state management
        ├── AccountsViewModel.kt     # Account tree state
        └── RegisterViewModel.kt     # Register/transaction state

composeApp/src/jvmMain/kotlin/com/bugdigger/piggybank/
└── main.kt                          # Desktop entry point with Koin init
```

---

## Notes

### Blockers
- None currently

### Decisions Made
- Default currency: RSD
- Date format: YYYY-MM-DD (ISO)
- Auth: Server-required
- First launch: Auto-create default accounts (handled by server on registration)
- Light theme only for MVP (dark theme can be added later)

### Technical Debt
- Android and iOS entry points need to be updated with Koin initialization
- Web entry points need to be updated with Koin initialization
- Exchange rates are hardcoded (should fetch from server in production)
- Need to add menu bar with full functionality
- Editing existing transactions not yet implemented
- Multi-split transaction creation UI not yet implemented (only 2-split)

### To Test
1. Start the server: `./gradlew server:run`
2. Run desktop app: `./gradlew composeApp:run`
3. Register a new user (auto-creates default accounts)
4. Login and view account tree
5. Double-click an account to open register tab
6. Close tabs with X button
