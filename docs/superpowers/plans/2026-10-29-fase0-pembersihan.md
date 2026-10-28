# Fase 0: Pembersihan dan Bug Sheet — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membuang beban yang bertabrakan dengan redesign (migrasi lama, Firebase Auth, tur spotlight, kode mati) dan memperbaiki dua bug bottom sheet yang dilaporkan pemilik, tanpa mengubah perilaku pencatatan.

**Architecture:** Hanya penghapusan dan satu perbaikan komponen bersama (`MankeuSheet`), yang dipakai semua sheet. Tidak ada entitas atau layar baru. Schema database tetap versi 12; versi lebih tua tidak lagi dimigrasikan maupun dipulihkan.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.06.00, Material 3), Room 2.8.4, Google Sign-In (`play-services-auth`), JUnit4, Compose UI test.

**Spec:** `docs/superpowers/specs/2026-10-29-redesign-spec.md` (keputusan #1, #8, #13, #16) dan `docs/audit/2026-10-29_UJI_ALUR_PENGUSAHA.md` (bug sheet).

## Global Constraints

- Uang selalu `Long` rupiah bulat.
- Operasi lokal tidak bergantung pada login, jaringan, Firebase, atau server milik app.
- Tidak ada dependensi baru. Fase ini mengurangi: `firebase-bom`, `firebase-auth`, plugin `google-services`.
- Log (`logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, `logs/CHANGELOG.md`) masuk commit yang sama dengan perubahannya.
- Perintah verifikasi (dari root `mankeu`, Windows): `./gradlew.bat :app:testDebugUnitTest`, `./gradlew.bat :app:lintDebug`,
  `./gradlew.bat :app:assembleDebug`, instrumented: `./gradlew.bat :app:connectedDebugAndroidTest` (emulator `Medium_Phone` menyala).

## File yang disentuh

| Tugas | Hapus | Ubah |
|---|---|---|
| 1 Sheet | – | `presentation/components/MankeuSheet.kt`; tes baru `androidTest/.../components/MankeuSheetTest.kt` |
| 2 Schema lama | `data/StrictCashCleanup.kt`, `data/ReversalCleanup.kt`, `data/LegacyPlanConversion.kt`, `androidTest/.../data/MigrationTest.kt`, `app/schemas/com.mankeu.data.AppDatabase/1.json`–`11.json` | `data/AppDatabase.kt`, `backup/EncryptedBackupManager.kt` |
| 3 Firebase | `auth/FirebaseAuthManager.kt`, `app/google-services.json` | `auth/AuthModels.kt` (+`GoogleIdentity`), `MankeuApplication.kt`, `presentation/AppViewModel.kt`, `presentation/MankeuApp.kt`, `app/build.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `androidTest/.../ViewModelFlowsTest.kt` |
| 4 Tur | `presentation/components/SpotlightTour.kt`, `presentation/guide/TourHost.kt` | `presentation/guide/Guides.kt`, `presentation/guide/HelpUi.kt`, `presentation/AppPreferences.kt`, `MankeuApp.kt`, 8 layar di `presentation/screens/`, `test/.../guide/GuidesTest.kt`, `androidTest/.../HelpScreenshotTest.kt`, `androidTest/.../ModuleScreenshotTest.kt`, `values/strings.xml`, `values-en/strings.xml` |
| 5 Kode mati | fungsi `SkeletonMetricGrid` | `presentation/components/Skeleton.kt` |
| 6 Rilis fase | – | log, `docs/WORK_IN_PROGRESS.md`, `AGENTS.md` (aturan import kolom kredit tetap sampai Fase 5) |

Jalur `presentation/...`, `data/...`, `auth/...`, `backup/...` relatif ke `app/src/main/java/com/mankeu/`.

---

### Task 1: Bottom sheet tidak tertutup karena geser isi dan tidak memantul saat keyboard

Penyebab (audit): `ModalBottomSheet` meneruskan sisa scroll isi ke seret sheet sehingga sheet tertutup; kolom isi memakai
`animateContentSize` yang ikut menganimasikan perubahan tinggi dari `imePadding()`, sehingga sheet memantul saat keyboard.
Komentar berkas menyebut `skipPartiallyExpanded = false`, padahal kodenya `true`.

**Files:**
- Modify: `app/src/main/java/com/mankeu/presentation/components/MankeuSheet.kt:35-130`
- Test: `app/src/androidTest/java/com/mankeu/presentation/components/MankeuSheetTest.kt` (baru)

**Interfaces:**
- Consumes: tidak ada.
- Produces: tanda tangan `MankeuSheet(...)` tidak berubah; semua pemanggil tetap dikompilasi.

- [ ] **Step 1: Tulis tes yang gagal**

```kotlin
package com.mankeu.presentation.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class MankeuSheetTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun swipingContentBackToTopDoesNotDismiss() {
        var dismissed = false
        rule.setContent {
            MankeuSheet(title = "Form", onDismiss = { dismissed = true }, onConfirm = {}) {
                repeat(30) { Text("Baris $it", Modifier.height(48.dp)) }
            }
        }
        rule.onNodeWithText("Baris 5").performTouchInput { swipeUp() }
        rule.onNodeWithText("Baris 10").performTouchInput { swipeDown() }
        rule.onNodeWithText("Baris 0").performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertFalse("sheet ditutup oleh geser isi", dismissed)
        rule.onNodeWithText("Form").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Jalankan dan pastikan gagal**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.presentation.components.MankeuSheetTest`
Expected: FAIL, `sheet ditutup oleh geser isi`.

- [ ] **Step 3: Implementasi minimal**

Di `MankeuSheet.kt`:
1. Ganti blok komentar poin kedua ("The sheet has snap points…") menjadi:
   `- **Only the buttons, back and the scrim close it.** Dragging is off (sheetGesturesEnabled = false), so scrolling a long form back to the top never drags the sheet away; there is no drag handle to suggest otherwise.`
2. Ganti poin ketiga menjadi:
   `- **The keyboard pushes, it does not cover.** imePadding() shrinks the sheet by the keyboard's height and the scrollable content brings the focused field into view. The content column is not animateContentSize'd: that animation chased every frame of the keyboard's own animation and made the sheet bounce.`
3. Panggilan `ModalBottomSheet`:

```kotlin
    ModalBottomSheet(
        onDismissRequest = { feedback(FeedbackEvent.TAP); onDismiss() },
        sheetState = sheetState,
        modifier = modifier,
        sheetGesturesEnabled = false,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
```

4. Kolom isi: hapus baris komentar "Sub-sections inside a sheet…" dan modifier `.animateContentSize(...)`, sehingga menjadi:

```kotlin
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
```

5. Tambah `padding(top = 20.dp)` pada kolom luar (menggantikan ruang drag handle): `.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 12.dp)`.
6. Hapus import yang tak terpakai: `animateContentSize`, `FastOutSlowInEasing`, `tween`, `LocalMotion`, dan `val motion`.

- [ ] **Step 4: Jalankan tes dan pastikan lulus**

Run: perintah Step 2. Expected: PASS.

- [ ] **Step 5: Uji emulator manual (bukti untuk laporan)**

Buka Rencana → Produk → Tambah. Ketuk "Nama produk", lalu ketuk dropdown di form biaya tetap saat keyboard terbuka.
Expected: sheet tidak memantul; menu dropdown menempel pada field. Geser isi ke atas lalu ke bawah melewati puncak:
sheet tetap terbuka. Catat hasil di `docs/audit/2026-10-29_UJI_ALUR_PENGUSAHA.md` bagian bug sheet ("diperbaiki, verified").
Kedipan keyboard Gboard asli: tandai "perlu HP nyata" bila tidak bisa diamati di emulator.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mankeu/presentation/components/MankeuSheet.kt app/src/androidTest/java/com/mankeu/presentation/components/MankeuSheetTest.kt docs/audit/2026-10-29_UJI_ALUR_PENGUSAHA.md logs/
git commit -m "fix(sheet): long forms no longer dismissed by scrolling, no bounce with keyboard"
```

---

### Task 2: Hapus migrasi schema 3→11 dan pembersih data lama

**Files:**
- Modify: `app/src/main/java/com/mankeu/data/AppDatabase.kt` (hapus `MIGRATION_3_4` … `MIGRATION_11_12`, baris 62–~400; ubah `create`)
- Modify: `app/src/main/java/com/mankeu/backup/EncryptedBackupManager.kt:3-5, 280-360, 415-425`
- Delete: `data/StrictCashCleanup.kt`, `data/ReversalCleanup.kt`, `data/LegacyPlanConversion.kt`
- Delete: `app/src/androidTest/java/com/mankeu/data/MigrationTest.kt`, `app/schemas/com.mankeu.data.AppDatabase/1.json` s.d. `11.json`

**Interfaces:**
- Produces: `AppDatabase.create(context)` membuka schema 12; database versi < 12 dibuang dan dibuat ulang kosong.
  `EncryptedBackupManager.OLDEST_RESTORABLE_SCHEMA = 12`.

- [ ] **Step 1: Pastikan tidak ada pemakai lain**

Run: `grep -rn "StrictCashCleanup\|ReversalCleanup\|LegacyPlanConversion\|MIGRATION_" app/src --include=*.kt`
Expected: hanya `AppDatabase.kt`, `EncryptedBackupManager.kt`, `MigrationTest.kt`, dan tiga berkas pembersih itu sendiri.

- [ ] **Step 2: Ubah `AppDatabase.create`**

Hapus semua `val MIGRATION_x_y`. Builder menjadi:

```kotlin
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
            .addCallback(SyncSchema.callback)
            .build()
```

Tambah satu komentar di atasnya: `// DEC-025 / spec #1: pre-0.1.0 installs start clean; only schema 12 is carried forward.`

- [ ] **Step 3: Ubah `EncryptedBackupManager`**

1. Hapus import `LegacyPlanConversion`, `ReversalCleanup`, `StrictCashCleanup`.
2. Hapus blok `if (prepared.snapshot.schemaVersion < NO_REVERSAL_SCHEMA) …`, `< STRICT_CASH_SCHEMA`, `< PLAN_SCHEMA` (baris ~286–306) dan fungsi `reversalPairIds`.
3. Validasi tabel: karena hanya schema 12 yang diterima, ganti ekspresi `required` menjadi `TABLES` penuh dan syaratnya `names.containsAll(TABLES) && names.size == TABLES.size`.
4. `const val OLDEST_RESTORABLE_SCHEMA = 12`; hapus konstanta `NO_REVERSAL_SCHEMA`, `STRICT_CASH_SCHEMA`, `PLAN_SCHEMA`, `STOCK_SCHEMA`, `STOCK_TABLES`, `PLAN_TABLES` bila tidak lagi dipakai, beserta paragraf KDoc yang menjelaskan migrasi lama (baris ~415–422).

- [ ] **Step 4: Hapus berkas**

```bash
git rm app/src/main/java/com/mankeu/data/StrictCashCleanup.kt app/src/main/java/com/mankeu/data/ReversalCleanup.kt app/src/main/java/com/mankeu/data/LegacyPlanConversion.kt app/src/androidTest/java/com/mankeu/data/MigrationTest.kt
git rm app/schemas/com.mankeu.data.AppDatabase/{1,2,3,4,5,6,7,8,9,10,11}.json
```

- [ ] **Step 5: Build dan tes**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Lalu `./gradlew.bat :app:connectedDebugAndroidTest`; tes backup yang memulihkan snapshot schema < 12 (jika ada) harus diubah untuk mengharapkan penolakan dengan pesan schema terlalu lama, bukan konversi.

- [ ] **Step 6: Uji pemasangan di atas v0.1.0**

Pasang APK v0.1.0 (`app-release` dari rilis GitHub v0.1.0) di emulator, isi satu transaksi, lalu pasang APK debug baru di atasnya.
Expected: data tetap ada (schema 12 tidak dimigrasi ulang).

- [ ] **Step 7: Commit**

```bash
git add -A app/src/main/java/com/mankeu/data app/src/main/java/com/mankeu/backup app/schemas app/src/androidTest logs/
git commit -m "refactor(data): drop schema 3-11 migrations and legacy cleanups (spec #1)"
```

---

### Task 3: Buang Firebase Auth, pakai akun Google langsung

`MankeuApp.kt:226-250` sudah punya jalur tanpa ID token yang membangun `AuthUser` langsung dari `GoogleSignInAccount`.
Jalur itu dijadikan satu-satunya jalur.

**Files:**
- Modify: `app/src/main/java/com/mankeu/auth/AuthModels.kt` (tambah `GoogleIdentity`)
- Delete: `app/src/main/java/com/mankeu/auth/FirebaseAuthManager.kt`, `app/google-services.json`
- Modify: `MankeuApplication.kt:23`, `presentation/AppViewModel.kt:49,56,150,940-1000`, `presentation/MankeuApp.kt:222-275,290-296`
- Modify: `app/build.gradle.kts:9,123-126`, `gradle/libs.versions.toml` (hapus `firebaseBom`, `firebase-bom`, `firebase-auth`, plugin `google-services`, versi `googleServices`), `build.gradle.kts` (hapus `alias(libs.plugins.google.services) apply false` bila ada)
- Modify: `app/src/androidTest/java/com/mankeu/presentation/ViewModelFlowsTest.kt:9,121`

**Interfaces:**
- Produces:
  - `class GoogleIdentity(context: Context)` dengan `val currentUser: StateFlow<AuthUser?>`, `fun refresh()`, `fun signOut()`.
  - `AppViewModel` menerima `identity: GoogleIdentity` menggantikan `authService: FirebaseAuthManager`;
    `fun onGoogleSignedIn()` menggantikan `signInWithGoogleIdToken`, `signInWithCredential`, `setAuthenticatedUser`.

- [ ] **Step 1: Tambah `GoogleIdentity` di `AuthModels.kt`**

```kotlin
/** The signed-in Google account, read straight from Google Sign-In (DEC-025: no Firebase). */
class GoogleIdentity(private val context: Context) {
    private val _currentUser = MutableStateFlow(lastAccount())
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    fun refresh() { _currentUser.value = lastAccount() }

    fun signOut() {
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        _currentUser.value = null
    }

    private fun lastAccount(): AuthUser? = GoogleSignIn.getLastSignedInAccount(context)?.let {
        AuthUser(uid = it.id ?: it.email ?: "google-user", email = it.email, displayName = it.displayName, photoUrl = it.photoUrl?.toString())
    }
}
```

Import: `android.content.Context`, `com.google.android.gms.auth.api.signin.GoogleSignIn`, `com.google.android.gms.auth.api.signin.GoogleSignInOptions`,
`kotlinx.coroutines.flow.MutableStateFlow`, `StateFlow`, `asStateFlow`.

- [ ] **Step 2: Sambungkan**

- `MankeuApplication.kt`: `val identity by lazy { GoogleIdentity(this) }` menggantikan `authService`.
- `AppViewModel`: parameter `private val identity: GoogleIdentity`; `val authUser = identity.currentUser`;
  hapus `signInWithGoogleIdToken`, `signInWithCredential`, `setAuthenticatedUser`, import `AuthCredential`, `FirebaseAuthManager`; tambah

```kotlin
    fun onGoogleSignedIn() {
        identity.refresh()
        val user = identity.currentUser.value ?: return
        message.value = UiText.of(R.string.msg_sign_in_ok, user.displayName ?: user.email ?: "")
    }
```

  `signOut()` memanggil `identity.signOut()`. Di `eraseAllData`, ganti `authService.deleteAccount()` dengan `true`
  (tidak ada lagi catatan akun di server; izin Google dicabut oleh `revokeAccess()` yang sudah ada di `MankeuApp.kt`), dan
  perbarui KDoc-nya: hapus "the Firebase user record".
- `MankeuApp.kt` launcher: setelah `task.getResult(ApiException::class.java)` cukup `viewModel.onGoogleSignedIn()`.
  Hapus blok `default_web_client_id` / `requestIdToken`. Pesan kode 10 diganti:
  `"Gagal masuk Google (Kode 10: DEVELOPER_ERROR). SHA-1 aplikasi belum terdaftar di OAuth client Google Cloud."`
- Factory ViewModel dan `ViewModelFlowsTest.kt:121`: kirim `GoogleIdentity(context)` menggantikan `FirebaseAuthManager()`.

- [ ] **Step 3: Buang dependensi**

`app/build.gradle.kts`: hapus `alias(libs.plugins.google.services)`, `implementation(platform(libs.firebase.bom))`,
`implementation(libs.firebase.auth)` dan komentar di atasnya. Bersihkan entri di `libs.versions.toml` dan root `build.gradle.kts`.
`git rm app/google-services.json`.

- [ ] **Step 4: Pastikan Firebase hilang total**

Run: `grep -rn "firebase\|google-services\|google.services" app gradle build.gradle.kts --include=*.kt --include=*.kts --include=*.toml`
Expected: tidak ada hasil.
Run: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Uji masuk Google**

Di emulator dengan akun Google (atau HP nyata): Pengaturan → Masuk dengan Google → pilih akun.
Expected: nama/email tampil; kartu sinkron bisa mengambil token Drive. Bila emulator tidak punya akun, catat
"perlu HP nyata" di laporan fase dan jangan klaim terverifikasi.

- [ ] **Step 6: Commit**

```bash
git add -A app/src gradle app/build.gradle.kts build.gradle.kts logs/
git commit -m "refactor(auth): drop Firebase Auth, use the Google account directly (spec #8)"
```

---

### Task 4: Hapus tur spotlight, pertahankan Pusat Bantuan

**Files:**
- Delete: `presentation/components/SpotlightTour.kt`, `presentation/guide/TourHost.kt`
- Modify: `presentation/guide/Guides.kt`, `presentation/guide/HelpUi.kt`, `presentation/AppPreferences.kt`, `presentation/MankeuApp.kt`,
  `presentation/screens/{AccountSettingsScreen,EntryScreen,HomeScreen,ManageScopeScreen,MoreScreen,PlanScreen,ReportsScreen,StockSection,SyncCard}.kt`,
  `test/.../guide/GuidesTest.kt`, `androidTest/.../HelpScreenshotTest.kt`, `androidTest/.../ModuleScreenshotTest.kt`, string ID/EN.

**Interfaces:**
- Produces: artikel bantuan tetap punya `ShowSpec` untuk membuka halaman yang tepat ("Tunjukkan di aplikasi"), tanpa sorotan.

- [ ] **Step 1: Petakan pemakai**

Run: `grep -rn "tourTarget\|TourTarget\|SpotlightTour\|TourHost\|activeTour\|tourFor\|TourTargetRegistry\|Tour\.\|seenTour\|tourVersion" app/src --include=*.kt`
Simpan daftar hasilnya di catatan kerja; setiap baris harus hilang atau diganti di langkah berikut.

- [ ] **Step 2: Hapus target dari layar**

Di setiap layar: hapus modifier `.tourTarget(TourTarget.X)` (biarkan modifier lain di rantai yang sama), hapus parameter
`LazyListState` yang hanya diteruskan untuk tur, dan import terkait.

- [ ] **Step 3: Hapus host dan preferensi**

`MankeuApp.kt`: hapus `tourTargets`, `activeTour`, `pageTour`, `tourFor(...)`, kartu tawaran "Lihat panduan", dan cabang
`activeTour != null` di `BackHandler`. `AppPreferences.kt`: hapus status tur yang dilihat dan switch tawaran tur.
`AccountSettingsScreen.kt`: hapus switch tur dan "Ulangi semua panduan". `HelpUi.kt`: hapus tombol "Putar ulang panduan halaman ini";
"Tunjukkan di aplikasi" tetap membuka halaman, tanpa sorotan. `Guides.kt`: hapus definisi `Tour`/langkah tur, pertahankan artikel dan `ShowSpec`.
Hapus string tur (`tour*_*`) di `values/strings.xml` dan `values-en/strings.xml`.

- [ ] **Step 4: Sesuaikan tes**

`GuidesTest.kt`: hapus tes yang memeriksa tur; pertahankan tes "setiap artikel punya screenshot".
`HelpScreenshotTest.kt` dan `ModuleScreenshotTest.kt`: hapus langkah yang menunggu sorotan; screenshot halaman tetap.

- [ ] **Step 5: Build, tes, dan cek sisa**

Run: `grep -rn "tourTarget\|TourTarget\|SpotlightTour\|TourHost" app/src` → Expected: kosong.
Run: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` → BUILD SUCCESSFUL.
Emulator: instal bersih, Beranda tidak menampilkan tawaran tur; tombol ? membuka Pusat Bantuan; "Tunjukkan di aplikasi" membuka halaman yang benar.

- [ ] **Step 6: Commit**

```bash
git add -A app/src logs/
git commit -m "refactor(help): remove spotlight tours, keep the help center (spec #16)"
```

---

### Task 5: Hapus kode mati skeleton

**Files:**
- Modify: `app/src/main/java/com/mankeu/presentation/components/Skeleton.kt:75-94`

- [ ] **Step 1:** Run `grep -rn "SkeletonMetricGrid" app/src` → hanya definisinya.
- [ ] **Step 2:** Hapus fungsi `SkeletonMetricGrid` beserta import yang tidak terpakai lagi.
- [ ] **Step 3:** Run `./gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `git commit -am "chore(ui): remove unused SkeletonMetricGrid"`

---

### Task 6: Tutup fase

**Files:**
- Modify: `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, `logs/CHANGELOG.md`, `docs/WORK_IN_PROGRESS.md`, `docs/audit/2026-10-29_UJI_ALUR_PENGUSAHA.md`

- [ ] **Step 1: Seluruh tes**

Run: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest`
Expected: semua hijau. Catat jumlah tes yang lulus.

- [ ] **Step 2: Uji ulang alur pengusaha**

Instal bersih di emulator, ulangi CP1–CP6 dari laporan uji. Expected: hasil sama seperti sebelumnya kecuali bug sheet
sudah hilang dan tidak ada tawaran tur.

- [ ] **Step 3: Log**

- `DECISION_LOG.md`: `[DEC-027] Fase 0 redesign: schema 12 minimum, tanpa Firebase Auth, tanpa tur spotlight`
  (supersedes: migrasi lama di DEC-006/DEC-011/DEC-017/DEC-019, bagian tur DEC-018, bagian akun Firebase DEC-020).
- `CHANGELOG.md` `[Unreleased]`: Fixed (sheet), Removed (tur, Firebase, data pra-0.1.0).
- `ACTIVITY_LOG.md`: entri sesi dengan bukti tes.

- [ ] **Step 4: Commit, push, pointer**

```bash
git add -A logs docs
git commit -m "docs(mankeu): close redesign phase 0"
git push origin main
cd .. && git add mankeu && git commit -m "chore(mankeu): bump pointer to redesign phase 0" && git push
```

Lalu tulis rencana rinci Fase 1 (`docs/superpowers/plans/YYYY-MM-DD-fase1-produk-hpp.md`) dari spec #5, #6, #9, #20.
