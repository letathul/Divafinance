# feature/scanner

**Purpose:** Two ways to get transactions in without typing them: OCR a receipt photo, or
paste/import a CSV statement. Both tabs live in one screen.

**Gradle:** `:feature:scanner` · `diva.kmp.compose`
**Depends on:** `:core:domain`, `:core:ui`, `:core:common`. Android adds ML Kit text
recognition.
**Paired with:** `:dynamic:scanner_dynamic` — ML Kit is delivered on demand, so OCR may be
genuinely absent at runtime.

## Key files

`src/commonMain/kotlin/com/divafinance/feature/scanner/`

| File | What it does |
|------|--------------|
| `ScannerViewModel.kt` | `ScannerTab` = `RECEIPT` / `IMPORT`. `ScannerUiState` carries both tabs' state (`lastReceipt`, `csvContent`, `importedCount`, `accountId`/`cardId`, `isProcessing`, `error`). Takes `ParseReceiptUseCase`, `ImportStatementUseCase`, and an `OcrEngine` **defaulted to `OcrEngine()`** so it can be substituted in tests. |
| `ScannerScreen.kt` | `DivaRoutes.SCANNER` — tab host for both paths |
| `ocr/OcrEngine.kt` | `expect class OcrEngine()` — `suspend recognizeText(imagePath): OcrResult` and `isAvailable()` |
| `ocr/OcrResult.kt` | `fullText` + `lines` |

### Actuals

| Source set | Behaviour |
|------------|-----------|
| `androidMain/OcrEngine.android.kt` | ML Kit text recognition |
| `iosMain/OcrEngine.ios.kt` | Vision-based iOS implementation |
| `jvmMain/OcrEngine.jvm.kt` | `isAvailable() = false`, returns an empty `OcrResult` — same "scanner unavailable" path a device without the on-demand module takes |

## Conventions / gotchas

- **Check `isOcrAvailable()` before offering the receipt tab.** Calling `recognizeText`
  on an unavailable engine returns empty text rather than throwing, which reads as a
  failed scan instead of an unsupported device.
- Parsing lives in `:core:domain` (`ParseReceiptUseCase`, `ImportStatementUseCase`), not
  here. This module only captures input and renders results — no regexes for merchant or
  amount extraction in the feature layer.
- The CSV import path has no platform dependency and works everywhere, including the
  desktop test host. It is the sensible thing to test.
- Don't add ML Kit imports to `commonMain`.

## Tests

`src/commonTest/` — `ScannerViewModelTest.kt` and `ScannerScreenTest.kt` (Compose UI),
running on the JVM host against the unavailable-OCR path.

```bash
./gradlew :feature:scanner:jvmTest
```
