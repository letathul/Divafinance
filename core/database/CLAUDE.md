# core/database

**Purpose:** The SQLDelight layer — schema, queries, migrations, enum column adapters, and
the per-platform driver. Owns SQL and nothing else; no domain logic and no mapping to
model types (that's `core:data`).

**Gradle:** `:core:database` · `diva.kmp.library` + SQLDelight plugin. Database name
`DivaFinanceDb`, package `com.divafinance.core.database`.
**Depends on:** `:core:model` (for the enums the adapters convert), `:core:common`
**Depended on by:** `:core:data` only. **Nothing else may depend on this module** —
generated row types stop at the data layer.

## Key files

| Path | What it does |
|------|--------------|
| `src/commonMain/sqldelight/.../DivaFinance.sq` | Every `CREATE TABLE`: `Account`, `CreditCard`, `RewardRule`, `DivaTransaction`, `Receipt`, `ReceiptLineItem`, `FeedPost`, `UserSettings`, `GraphThreshold`, `Person`, `CustomCategory`, `LedgerEntry`, plus the indices |
| `Account.sq` `CreditCard.sq` `RewardRule.sq` `Transaction.sq` `Receipt.sq` `ReceiptLineItem.sq` `FeedPost.sq` `Settings.sq` `GraphThreshold.sq` `Person.sq` `CustomCategory.sq` `LedgerEntry.sq` | Named queries per table. Schema is *not* redeclared here — only `DivaFinance.sq` creates tables. |
| `src/commonMain/sqldelight/.../migrations/1.sqm` | v1 → v2. Adds the three `DivaTransaction` indices to existing installs. |
| `src/commonMain/sqldelight/.../migrations/4.sqm` | v4 → v5. Adds `CustomCategory`, plus `tags` and `custom_category_id` on `DivaTransaction`. |
| `src/commonMain/sqldelight/databases/1.db` | Committed schema snapshot of v1. Used by `verifyMigrations`. |
| `src/commonMain/kotlin/.../adapter/EnumColumnAdapters.kt` | `ColumnAdapter`s storing each enum by `.name` as TEXT |
| `src/commonMain/kotlin/.../DatabaseDriverFactory.kt` | `expect class` with `create(): SqlDriver`; actuals in `androidMain` (AndroidSqliteDriver), `jvmMain` (JdbcSqliteDriver), `iosMain` (NativeSqliteDriver) |

## Conventions / gotchas

- **`verifyMigrations = true`.** The build fails if the `.sqm` files don't reproduce the
  `.sq` schema. This is the only thing standing between a schema edit and a bricked
  upgrade — don't switch it off to get a build green.
- **Never regenerate `databases/1.db`.** It captures the shape shipped before any
  migration existed; regenerating it after a `.sq` edit destroys the migration baseline.
- Any schema change needs a **new numbered `.sqm`**. `CREATE INDEX` inside a `.sq` file
  only runs during `Schema.create()`, so existing installs never see it without a
  migration — that's exactly what `1.sqm` exists to fix.
- **New columns go last.** `ALTER TABLE ADD COLUMN` appends, and `verifyMigrations`
  compares shapes, so a column declared mid-table in `DivaFinance.sq` will never match what
  the migration produces. `others_share`, `tags` and `custom_category_id` are last on
  `DivaTransaction` for exactly this reason.
- Regenerate the schema snapshot after a schema edit with
  `./gradlew :core:database:generateCommonMainDivaFinanceDbSchema`, and check the new
  numbered `.db` in alongside the `.sqm`.
- The transaction table is `DivaTransaction`, not `Transaction` — `TRANSACTION` is a SQL
  keyword.
- Enums round-trip through `valueOf(name)`, so renaming an enum constant in `core:model`
  makes existing rows fail to decode. Treat it as a data migration.

## Tests

No test source of its own. Schema correctness is enforced at build time by
`verifyMigrations`; behaviour is covered through `core:data` and `core:domain` tests.

```bash
./gradlew :core:database:build     # runs migration verification
```
