# feature/backup

**Purpose:** Export the whole database to a JSON archive on disk and import one back.
Lists existing backup files with confirm dialogs for the destructive actions.

**Gradle:** `:feature:backup` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, `:core:ui`, `:core:common`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/backup/`

| File | What it does |
|------|--------------|
| `BackupViewModel.kt` | `BackupUiState` holds the file list plus in-flight flags and two pending-confirmation pairs (`showImportConfirmDialog`/`pendingImportFile`, `showDeleteConfirmDialog`/`pendingDeleteFile`). Outcomes are sealed classes — `ExportResult.Success(fileName)`/`Error`, `ImportResult.Success(accounts, cards, transactions)`/`Error`. Takes `ExportBackupUseCase`, `ImportBackupUseCase`, and `FileSystem`. |
| `BackupRestoreScreen.kt` | `DivaRoutes.BACKUP` — file list, export button, import/delete flows |
| `BackupProgressDialog.kt` | Progress surface for export/import |

## Conventions / gotchas

- **Import and delete are gated behind confirmation.** Each is a
  `show*ConfirmDialog` + `pending*File` pair — set both, act only on confirm, clear both
  after. Don't wire a button straight to the use case.
- Failures are modelled as `ExportResult.Error` / `ImportResult.Error` in state rather
  than thrown. Every path must land in one of the sealed results so the UI can report it.
- Archive serialization is `BackupArchive` from `core:model` via a locally configured
  `Json` instance. Changing the archive shape is a compatibility break for existing files
  — old backups still have to parse.
- File I/O goes through `FileSystem` from `:core:common` (an `expect class`), never
  through `java.io`, or the iOS target won't compile.

## Tests

`src/jvmTest/BackupRestoreScreenTest.kt` — Compose UI test.

```bash
./gradlew :feature:backup:jvmTest
```
