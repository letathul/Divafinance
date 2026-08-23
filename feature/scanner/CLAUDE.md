# feature/scanner

**Purpose:** Two ways to get transactions in without typing them: photograph a receipt and
review what OCR read off it, or paste/import a CSV statement. A third tab lists past scans.

**Gradle:** `:feature:scanner` · `diva.kmp.compose`
**Depends on:** `:core:domain`, **`:core:data`**, `:core:model`, `:core:ui`, `:core:common`.
Android adds ML Kit text recognition and `androidx-activity-compose` (result launchers).
**Paired with:** `:dynamic:scanner_dynamic` — ML Kit is delivered on demand, so OCR may be
genuinely absent at runtime.

`:core:data` is the documented exception the module shares with `quickadd`/`settings`/`demo`:
the review screen reads `SettingsRepository` for the default account, card and currency, and
those reads have no use-case wrapper.

## Key files

`src/commonMain/kotlin/com/divafinance/feature/scanner/`

| File | What it does |
|------|--------------|
| `ScannerViewModel.kt` | `ScannerTab` = `RECEIPT` / `HISTORY` / `IMPORT`. State carries the capture nonce, `reviewReceiptId`, and the CSV tab's fields; `receipts` is a separate `StateFlow` off `GetReceiptsUseCase`. |
| `ScannerScreen.kt` | `DivaRoutes.SCANNER` — tab host for all three paths |
| `review/ReceiptReviewViewModel.kt` | The review form. Its **only** input is a `receiptId`. |
| `review/ReceiptReviewScreen.kt` | `DivaRoutes.RECEIPT_REVIEW` (`scanner/review/{receiptId}`) |
| `capture/ImageCapture.kt` | `ImageSource` · `ImageCaptureResult` · `fun interface ImageCaptureRequester` + `@Composable expect fun rememberImageCaptureRequester()` |
| `capture/TextFilePicker.kt` | `TextFileResult` · `fun interface TextFilePicker` + `@Composable expect fun rememberTextFilePicker()` — the CSV import's file chooser |
| `capture/ReceiptThumbnail.kt` | `@Composable expect fun rememberReceiptThumbnail(path, maxDimension)` |
| `ocr/OcrEngine.kt` | `expect class OcrEngine()` — `suspend recognizeText(imagePath): OcrResult` and `isAvailable()` |

### Actuals

| Source set | `OcrEngine` | `ImageCapture` | `TextFilePicker` | `ReceiptThumbnail` |
|------------|-------------|----------------|------------------|--------------------|
| `androidMain` | ML Kit | `TakePicture` + `PickVisualMedia` + runtime CAMERA permission | `OpenDocument` | downsampled `BitmapFactory` |
| `iosMain` | Vision | `UIImagePickerController` + `PHPickerViewController` | `UIDocumentPickerViewController` | `null` |
| `jvmMain` | unavailable, empty result | reports `Failed` | reports `Failed` | `null` |

## Conventions / gotchas

- **The ViewModel decides whether to launch; the screen owns the launcher** — Android needs an
  Activity result contract. `captureRequestNonce` carries the decision across and the screen's
  `LaunchedEffect` keys on it. It **must stay monotonic**: restarting at zero reopens the camera
  on a value already handled. Same pattern as `quickadd`'s `permissionRequestNonce`.
- **`PROCESSED` means "linked to a transaction", nothing else.** `ParseReceiptUseCase` always
  writes `PENDING` (or `FAILED` when OCR returned nothing); only `ConfirmReceiptUseCase`
  promotes a receipt, and it writes both sides of the link. A `PENDING` row is what the history
  tab offers to resume.
- **Resuming a receipt and reviewing a fresh scan are the same code path.** The review VM seeds
  every field from the stored row, so a review survives process death with no draft state.
- **Check `isOcrAvailable()` before offering the scan tab.** An unavailable engine returns empty
  text rather than throwing, which would read as a failed scan instead of an unsupported device.
  There is deliberately **no demo-scan button** — a fabricated result hides a real problem.
- Parsing lives in `:core:domain` (`ReceiptParser`, pure and heavily tested), not here. No
  merchant or amount regexes in the feature layer.
- Captured images go to `filesDir/receipts/`, never `cacheDir` — `Receipt.imagePath` is a
  persisted column the history tab re-reads, and an eviction would orphan it.
- The **FileProvider lives in `composeApp`'s manifest**, not this module's, because this module
  is consumed by both the base APK and the scanner split. The authority is derived at runtime as
  `"${context.packageName}.fileprovider"`.
- `ScannerScreen`'s `onReviewReceipt`/`onOpenTransaction` **must keep their no-op defaults** —
  `ScannerDynamicActivity` hosts the screen with no NavHost.
- Don't add ML Kit imports to `commonMain`.
- **`TextFilePicker` returns content, not a path**, unlike `ImageCaptureRequester`. A
  statement is read once and never referred to again, so copying it into app storage the
  way a receipt image is copied would leave a file nothing ever reads. On iOS the read
  happens inside a `startAccessingSecurityScopedResource` window — a picked file is outside
  the sandbox and the read fails without it.
- **`iosMain` has two files in this package with private helpers.** A private top-level
  name is file-scoped but still collides in resolution, so `TextFilePicker.ios.kt` uses
  `DocumentPickerDelegate` / `topViewControllerForFilePicker` rather than reusing
  `ImageCapture.ios.kt`'s `PickerDelegate` / `topViewController` names.
- **The import form never asks for a raw id.** Account and card come from chip rows over
  `AccountRepository` / `CardRepository`; a single account is preselected, and tapping the
  selected card chip clears it (that's how the optional card is unset).
- Imported rows are categorised by `CategoryPredictionEngine` on the merchant string.
  History is read **once, before the loop**, and rows imported in the same pass don't feed
  the next — so the result doesn't depend on the file's row order.

## Tests

`src/jvmTest/` — `ScannerViewModelTest`, `ScannerScreenTest`, `ReceiptReviewViewModelTest`,
`ReceiptReviewScreenTest` (the last two under `review/`), plus `TestDoubles.kt`. Compose UI
tests cannot live in `commonTest`; the desktop host is the only one that can run them.

On this host `OcrEngine.isAvailable()` is false and the capture requester reports `Failed`, so
the scan tab always renders the unavailable branch — seed `FakeReceiptRepository` to test the
history and review paths instead.

```bash
./gradlew :feature:scanner:jvmTest
```
