# Implementation Plan — Receipt attachments in the feed and transaction detail

> Plan, not a build log. Once these land, record them in `IMPLEMENTATION_STATUS.md` and
> update the module `CLAUDE.md` files listed at the end.

**In this update:** attachments render in the feed (cropped preview) and on the transaction
detail screen (whole receipt, zoomed out), plus a pinch-zoom full-screen viewer. All files
stay on the device.

**Deferred to a later update:** cloud offload to Google Drive / iCloud. Gated on registering
the app in Google Cloud and on an Apple iCloud container. Design notes are at the end.

**No schema change. No migration. One dependency moves modules; nothing new is added.**

---

## Why

Receipts are captured, parsed and stored, but never displayed outside the review form:

- `TransactionDetailScreen.kt:87` renders the raw receipt UUID as a text row.
- `feature/feed` shows a `CategoryTile` and nothing else.
- The scanner history tab (`ScannerScreen.kt:236-263`) shows no thumbnail.

The pixels already exist in `filesDir/receipts/`. Three things block showing them:

1. **iOS has no image decoding at all.** `ReceiptThumbnail.ios.kt` is a hard-coded
   `actual fun rememberReceiptThumbnail(...) = null` with zero interop; the jvm actual is
   the same. Task 1 is the prerequisite for everything else.
2. **`core/ui` has no image code** — no `Image`, no `ImageBitmap`, no painter, and no image
   library in the version catalog.
3. **Neither `:feature:feed` nor `:feature:transactions` can reach the decoder.** It lives
   in `:feature:scanner` and `decodeOriented` is `internal`. Two more features now need it,
   so by `core/ui/CLAUDE.md`'s own rule it belongs in `:core:ui`.

---

## Task 1 — Image loader in `:core:ui`

**New** `core/ui/src/commonMain/kotlin/com/divafinance/core/ui/image/LocalImage.kt`

```kotlin
/**
 * Decodes a local image file, downsampled and EXIF-oriented, off the main thread.
 * Null while decoding, when [path] is null, and where the platform has no decoder.
 *
 * Takes a path rather than a domain type on purpose: core:ui never learns what a receipt
 * is, and the same loader serves any future attachment without change.
 */
@Composable expect fun rememberLocalImage(path: String?, maxDimension: Int = 512): ImageBitmap?
```

**New source-set directories.** `core/ui` currently has only `commonMain`, `iosMain`,
`jvmSharedMain`, `jvmTest`. Android and desktop need different decoders, so create
`core/ui/src/androidMain/kotlin/…` and `core/ui/src/jvmMain/kotlin/…`. Both source sets
already exist in the KMP model via `diva.kmp.library`'s hierarchy template — only the
directories are missing. **Do not touch the hierarchy template**: hand-wiring source sets
switches the default template off and silently unhooks `iosMain` from the Native targets.

| Actual | Implementation |
|---|---|
| `LocalImage.android.kt` | Move `decodeOriented` here verbatim from `feature/scanner/src/androidMain/.../capture/ImageDecoding.android.kt` and drop `internal`. It already does the `inJustDecodeBounds` two-pass downsample and all 8 EXIF orientations. |
| `LocalImage.ios.kt` | **New interop.** `UIImage(contentsOfFile:)` → redraw through `UIGraphicsImageRenderer` at the target size to normalise orientation → `CGImage` → `CGBitmapContextCreate` into an RGBA8888 buffer → `ImageBitmap`. Needs `@OptIn(ExperimentalForeignApi::class)`. |
| `LocalImage.jvm.kt` | `ImageIO.read` → `BufferedImage` → `toComposeImageBitmap()`, scaled to `maxDimension`. |

**Implement the jvm actual properly rather than returning null.** Compose UI tests run on
the jvm host, so this is what turns the image branch from structurally untestable into
asserted-in-CI. Every screen below is otherwise only verifiable by hand on a device.

**New** `core/ui/src/commonMain/.../image/ImageCache.kt` — an LRU keyed on
`path + maxDimension`, bounded by entry count.

This is load-bearing, not an optimisation. `rememberReceiptThumbnail` today is a bare
`produceState` keyed on the path, which re-decodes every time a composable re-enters
composition. That is fine for one review screen and wrong for a scrolling feed, where it
would re-decode on every fling. `produceState` still cancels on dispose, so scrolling past a
row aborts an in-flight decode.

**Modify** `feature/scanner/src/commonMain/.../capture/ReceiptThumbnail.kt` — collapse the
`expect`/3 actuals into a one-line delegate to `rememberLocalImage`, keeping its existing
call sites in `ReceiptReviewScreen` untouched. Delete the three actual files and
`ImageDecoding.android.kt`.

**Modify** `feature/scanner/build.gradle.kts` — remove `libs.androidx.exifinterface`.
**Modify** `core/ui/build.gradle.kts` — add it to a new `androidMain.dependencies` block.

---

## Task 2 — Attachment resolution in `:core:domain` / `:core:data`

**New** `core/domain/src/commonMain/.../usecase/scanner/GetReceiptForTransactionUseCase.kt`
wrapping `ReceiptRepository.getByTransactionId` — implemented, correct, and currently with
**zero callers**. Register it in `composeApp/.../di/DomainModule.kt`; a use case is not
reachable until it is.

**New** extension, next to the use case, so no caller assembles the list by hand:

```kotlin
/** Page 1 is imagePath; the rest are pagePaths, in printed order. */
val Receipt.attachmentPaths: List<String>
    get() = listOfNotNull(imagePath) + pagePaths
```

**Modify** `core/data/.../repository/ReceiptRepository.kt` + `ReceiptRepositoryImpl.kt` —
add one batch read so the feed does not issue a query per row:

```kotlin
suspend fun getCoverPathsByTransactionIds(ids: List<String>): Map<String, String>
```

**Modify** `core/database/.../Receipt.sq` — add the backing query. Note this is a `.sq`
**query** addition only, not a table change, so **no `.sqm` migration and no new `.db`
snapshot**:

```sql
selectCoverPathsByTransactionIds:
SELECT transaction_id, image_path FROM Receipt
WHERE transaction_id IN ? AND image_path IS NOT NULL;
```

**Modify** `core/testing/.../fake/FakeReceiptRepository.kt` and
`feature/demo/src/commonTest/.../FakeRepositories.kt` — both implement `ReceiptRepository`
and will not compile without the new method.

### Non-image attachments

Everything the app can produce today is a JPEG — the camera path, ML Kit's document scanner
and VisionKit all write JPEG, and there is no other way to attach a file. PDFs cannot occur
yet. The UI still branches on file extension and renders a document icon rather than
attempting a decode, so a future PDF or CSV degrades to an icon instead of a blank box.
**Rendering PDF pages is out of scope** — that needs `PdfRenderer` / `PDFKit` and belongs
with whatever feature first creates one.

---

## Task 3 — `:core:ui` component changes

**Modify** `core/ui/.../component/LedgerRow.kt` — `TransactionRow` gains a slot:

```kotlin
fun TransactionRow(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,   // new; null keeps CategoryTile
    onClick: () -> Unit = {},
)
```

Body: `leading?.invoke() ?: CategoryTile(transaction.category)`. Same slot pattern
`DivaListRow` already uses (`GroupedList.kt:159`).

**The thumbnail must be 44.dp**, matching `CategoryTile`'s default size, or the row height
shifts between rows that have a receipt and rows that don't. The row's
`heightIn(min = diva.rowMinHeight + 12.dp)` will not save it.

**Modify** `core/ui/.../component/MomentCard.kt` — add
`cover: (@Composable BoxScope.() -> Unit)? = null`, defaulting to the existing category
duotone `Brush.linearGradient`.

**The cover already has content drawn on it**: `amount` and `caption` at `BottomStart`, and
the `badge` `GlassSurface`. Over a category wash those are legible; over an arbitrary
receipt photo they will not be. When `cover != null`, draw a bottom-up scrim
(`Brush.verticalGradient(Transparent → scrim)`) between the image and that text. Keep the
existing `detectTapGestures` double-tap-to-flag block on the `Box` — the cover slot goes
behind it, not around it.

Update the KDoc at `MomentCard.kt:47-51`, which currently states there is no image pipeline
in this app.

**New** `core/ui/.../component/ImageViewer.kt`:

```kotlin
@Composable
fun ImageViewer(paths: List<String>, startIndex: Int = 0, onClose: () -> Unit)
```

Nothing exists to reuse — there is no dialog, sheet, modal, zoom helper or `Popup` anywhere
in `core/ui`. Full-bleed `Box` over `MaterialTheme.colorScheme.scrim`; pinch and pan via
`Modifier.pointerInput` + `detectTransformGestures`, translation clamped to the scaled
bounds so the image cannot be flung off screen; double-tap toggles fit/fill; a horizontal
pager across `paths`; a close affordance. Decodes at a much larger `maxDimension` than any
thumbnail.

---

## Task 4 — Feed

**Modify** `feature/feed/.../FeedViewModel.kt` — `LedgerItem` gains
`attachmentCoverPath: String? = null`, populated from
`getCoverPathsByTransactionIds` over the transaction ids already in the day sections.

**Modify** `feature/feed/.../FeedScreen.kt` — `LedgerEntry` (L151-180) passes a thumbnail
into `TransactionRow(leading = …)` and `MomentCard(cover = …)`. Both fall back automatically
when the path is null or the decode returns null.

**Crop to the top, not the centre.** A receipt is tall and narrow, so a centre-crop into a
44dp square shows the middle of the item list — visually identical for every receipt the
user owns. The merchant name and logo are at the top. Use `ContentScale.Crop` with
`alignment = Alignment.TopCenter` in both slots.

Fidelity ladder across the four surfaces:

| Surface | Box | `maxDimension` | Scale |
|---|---|---|---|
| Ledger row leading slot | 44dp rounded square | 128 | `Crop`, `TopCenter` |
| MomentCard cover | full width, 16:9 | 512 | `Crop`, `TopCenter` |
| Detail screen card | full width, ~200dp | 1024 | `Fit` — the whole receipt |
| Full-screen viewer | full bleed | 2048 | `Fit`, then zoomable |

The feed crops, the detail screen shows the whole thing zoomed out, the viewer is where it
becomes legible. Tapping a feed row still opens the transaction — the thumbnail is not a
separate tap target.

---

## Task 5 — Transaction detail

**New** `feature/transactions/.../TransactionDetailViewModel.kt` taking `transactionId`,
reading the transaction from `TransactionRepository` and the receipt from
`GetReceiptForTransactionUseCase`.

**Modify** `composeApp/.../di/ViewModelModule.kt` — register with the
`viewModel { params -> …(params.get(), get()) }` form already used for `ReportViewModel`,
`PersonDetailViewModel` and `ReceiptReviewViewModel`.

**Modify** `composeApp/.../DivaNavHost.kt:178-188` — switch to
`koinViewModel(key = id) { parametersOf(id) }`.

> This also fixes a live bug. The route currently picks the row out of
> `transactionsViewModel.filteredTransactions`, so an active category, type or search filter
> makes the detail screen `return@composable` and render nothing.

**Modify** `feature/transactions/.../TransactionDetailScreen.kt:87` — replace
`transaction.receiptId?.let { DetailRow("Receipt", it) }` with an **Attachments**
`DivaCard`:

- the cover rendered `Fit`, so the whole receipt is visible without tapping
- a horizontal strip of page thumbnails when `attachmentPaths.size > 1`
- merchant / total / date as read off the receipt, and `receipt.lineItems` when non-empty
- tapping any of it opens the viewer **at that page index**
- when the transaction has no receipt the card is absent entirely — not an empty state

Keep `TransactionDetailScreen` a stateless composable taking values and lambdas, as
`SettingsScreen` does, so previews and jvm tests work without Koin.

---

## Task 6 — Viewer route

**Modify** `composeApp/.../DivaNavHost.kt` — add to `DivaRoutes`:

```kotlin
const val RECEIPT_VIEW = "receipt/view/{receiptId}?page={page}"
fun receiptView(receiptId: String, page: Int = 0) = "receipt/view/$receiptId?page=$page"
```

**`DivaRoutesTest` asserts every route literal — adding one is a two-file change by
design.** Update `composeApp/src/commonTest/.../DivaRoutesTest.kt`.

Host the `composable` block in `composeApp`, which depends on every feature, so neither
`:feature:feed` nor `:feature:transactions` gains a dependency on `:feature:scanner`. Read
the nav argument with `backStackEntry.arguments?.read { getStringOrNull("receiptId") }` —
the multiplatform `SavedState` accessor, not `getString()`.

---

## Task 7 — Scanner history (two lines, while here)

**Modify** `feature/scanner/.../ScannerScreen.kt:236-263` — `ReceiptRow` shows no image
either. Add the same 44dp `TopCenter` crop in its leading position.

---

## Tests

| File | Covers |
|---|---|
| `core/ui/src/jvmTest/.../image/LocalImageTest.kt` | decodes a real fixture on the jvm host; a second call hits the LRU rather than re-decoding; a missing path returns null |
| `core/ui/src/jvmTest/.../component/ImageViewerTest.kt` | opens at `startIndex`; close callback fires; renders with an empty `paths` list |
| `core/ui/src/jvmTest/.../component/LedgerRowTest.kt` | `leading = null` renders the `CategoryTile`; a supplied slot replaces it; row height is unchanged between the two |
| `core/domain/src/commonTest/.../GetReceiptForTransactionUseCaseTest.kt` | returns the linked receipt; null when unlinked; `attachmentPaths` ordering and the single-page case |
| `feature/transactions/src/jvmTest/.../TransactionDetailViewModelTest.kt` | loads transaction + receipt; **renders with a filter active** (the bug above); no receipt → no card |
| `feature/feed/src/jvmTest/.../FeedViewModelTest.kt` | cover paths map onto the right `LedgerItem`s; one batch read, not one per row |
| `composeApp/src/commonTest/.../DivaRoutesTest.kt` | the new `RECEIPT_VIEW` literal |

Worth asserting specifically, because they are the easy things to get wrong:

- a transaction with no receipt still renders the category tile and no Attachments card
- a receipt whose file has been deleted from disk falls back to the tile, not a blank box
- a multi-page receipt opens the viewer at the tapped page, not page 1

## Verification

```bash
./gradlew :core:ui:jvmTest
./gradlew :core:domain:jvmTest
./gradlew :feature:feed:jvmTest :feature:transactions:jvmTest :feature:scanner:jvmTest
./gradlew :composeApp:testDebugUnitTest
./gradlew jvmTest                    # 597 tests pass today; nothing may regress
./gradlew :composeApp:assembleDebug
./gradlew :dynamic:scanner_dynamic:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

The link task is not optional: a missing iOS actual surfaces as a **link** error there, not
as a compile error in `commonMain`.

On device — a scanned receipt shows a cropped thumbnail in its feed row, a split bill shows
the photo as its MomentCard cover with the amount still legible over it, the detail screen
shows the whole receipt zoomed out, and tapping opens the viewer with pinch-zoom and page
swiping. **Do this on an iOS device specifically**: that path renders nothing at all today,
so it is the only real proof Task 1 worked.

> Carried over from the receipt-scanning work, which is implemented and passing (597 JVM
> tests, migration 3 verified): `:composeApp:assembleDebug`,
> `:dynamic:scanner_dynamic:assembleDebug` and `linkDebugFrameworkIosSimulatorArm64` were
> never run, because the disk filled at the time. They are in the list above.

## Docs to update on completion

`core/ui/CLAUDE.md` — the module gains an image pipeline, so its "no image" statements and
the anti-dependency note become wrong · `feature/feed/CLAUDE.md` ·
`feature/transactions/CLAUDE.md` · `feature/scanner/CLAUDE.md` (the decoder moved out) ·
`core/domain/CLAUDE.md` (new use case) · `IMPLEMENTATION_STATUS.md`.

---

---

# Deferred — cloud offload (next update)

Out of scope. Gated on registering the app in Google Cloud. Recorded so the next update
starts from a decision rather than a blank page.

## Prerequisites, both outside this repo

1. **Google Cloud project + OAuth client IDs.** Android needs the package name plus the
   debug *and* release SHA-1 fingerprints; iOS needs its own client ID and the
   reversed-client-id URL scheme. Scope `drive.file` only — it is non-sensitive, so no OAuth
   verification review is required.
2. **iCloud container** `iCloud.com.divafinance.app` enabled against team `URAT4QWWD5` in
   the Apple Developer portal, then a provisioning-profile refresh.

## Shape of the work

- **Maximise shared Kotlin.** A `CloudSyncEngine` in `commonMain` owns the whole state
  machine, retry policy and every database write; Drive v3 goes over the shared Ktor client.
  Only three things are platform-specific: `CloudTokenProvider` (OAuth),
  `BackgroundScheduler` (WorkManager / `BGTaskScheduler`), and `IcloudFileStore` (ubiquity
  container). Each platform worker is a ~20-line shell calling `runPendingWork()`.
- **Schema.** Migration `4.sqm` (v4 → v5) appending `sync_status`, `cloud_file_id`,
  `cloud_provider`, `cloud_synced_at` to `Receipt`. New columns **must go last** — `ALTER
  TABLE ADD COLUMN` appends, the trap documented in `2.sqm`. `sync_status` *is* the queue;
  no separate table. Default `LOCAL_ONLY`, since every existing row predates cloud sync.
- **Encryption.** A random 256-bit data key wrapped by PBKDF2(PIN), not a PIN-derived file
  key — the latter would orphan every uploaded file on a PIN change. Wrapping means a PIN
  change re-encrypts ~100 bytes. Requires a PIN before cloud sync can be enabled.
- **Android** gets a `:dynamic:cloud_dynamic` split holding only `play-services-auth`, which
  is the actual size win. The `DynamicModule` enum value must match the directory name
  exactly, underscores not hyphens.
- **Upload on save.** `ConfirmReceiptUseCase` sets `PENDING` and requests a sync, then
  returns. Never at capture — the review screen is about to display that file. The engine
  deletes the local copy *only after* the upload is confirmed; a failed upload keeps it.
- **Delete on delete.** `DeleteTransactionUseCase` gains a receipt step **before**
  `transactionRepository.delete`, so `Receipt.transaction_id` is never left dangling. A
  receipt with a `cloud_file_id` becomes a `DELETING` tombstone; the row survives until the
  remote delete succeeds, because dropping it immediately would discard the id and orphan
  the file in the user's Drive permanently.
- **One seam to add early if convenient.** Routing every attachment read through a single
  `resolveAttachmentPath(receiptId, page)` — a pass-through with no I/O today — means the
  cloud version changes one function instead of three screens.

## Two things worth knowing before starting it

- **Deleting a transaction already leaks, today.** `DeleteTransactionUseCase` handles the
  card balance and ledger entries but never touches the receipt, so it strands a `Receipt`
  row pointing at a deleted transaction plus its image file on disk. That is a real bug
  independent of cloud sync and is a few lines in one use case — worth pulling into the
  current update.
- **iOS lazy code loading is not achievable.** Kotlin/Native emits a *static* framework
  (`isStatic = true` in `composeApp/build.gradle.kts`), so all Kotlin links into the app
  binary at launch. There is no iOS equivalent of Play Feature Delivery for code. The Swift
  OAuth code can live in a local SPM package for build hygiene, but the launch-footprint
  saving is Android-only.
