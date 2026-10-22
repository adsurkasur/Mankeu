**Effective:** 22 October 2026 · **App:** Mankeu (`com.mankeu`) · **Developer:** Ade Surya Ananda (AdsurKasur)

> **Core principle.** Mankeu works offline and keeps your data on your own phone. Your cash records, business plan, stock and reports are never sent to our servers. The app has no ads, no trackers and no telemetry.

## 1. Overview

Mankeu is a cash book and business planner for micro, small and medium businesses. This policy explains what data the app holds, where it is stored, and how you delete it.

## 2. Data stored on your phone

Everything you enter is stored in the local database (SQLite/Room) on your phone, including:

- businesses, branches and units;
- money in and out, the business plan (products, costs, capital) and stock;
- import history and app settings.

This data is **never sent to, stored on, or analysed by developer-owned servers**. Android's automatic app-data backup is turned off.

## 3. Google Sign-In, Google Drive backup and sync (optional)

- **Google Sign-In** is used only if you choose to back up or sync through your own Google Drive. Your name and email are shown as the account status in Settings. Signing in creates a sign-in account in the developer's Firebase Authentication project; it holds only your Google identity, no financial data.
- **Encrypted on your phone.** Backup files (`.aabk`) are encrypted on your phone with AES-256-GCM under a password you choose, before upload. The password is not stored by the app and cannot be recovered.
- **Sync between phones (when you turn it on).** Changes are sent to your Google Drive and picked up by your other phones signed in with the same Google account. Every upload is encrypted on the phone with AES-256-GCM under a sync passphrase you choose. The passphrase is kept on the phone encrypted by the Android Keystore, is never sent, and cannot be recovered. Each phone gets a random identifier used only to name its sync files in your Drive.
- **Separate app folder (`drive.appdata`).** Backups and sync files are stored in a private app folder on your Google Drive. The app cannot read your other Drive files, and neither the developer nor anyone else can access your backups or their password.

## 4. Excel (.xlsx) and PDF files

- Excel import and export and PDF export run **entirely on your phone**, without uploading files.
- The app only opens files you pick yourself through the Android file picker.

## 5. No ads and no trackers

- The app is ad-free.
- The app **contains no analytics or tracking SDKs** (such as Google Analytics, Firebase Analytics or the Facebook SDK).
- Your usage data is never sold or shared with anyone.

## 6. Device permissions

| Permission | Purpose |
| --- | --- |
| `INTERNET` | Only when you sign in with Google, create and restore a Google Drive backup, or while sync between phones is on. |
| `ACCESS_NETWORK_STATE` | Used by the Google libraries (Sign-In and Firebase) to check the connection. |
| Android file picker | Picks Excel or backup files for import, export and restore. No storage permission is needed. |

## 7. Deleting your data

- **Delete one record:** tap Delete on the record card.
- **Delete all data:** Settings › *Delete all data*, then type DELETE. Every business, unit, record, business plan, stock item, import history, automatic recovery copy and local name on the phone is deleted. If you signed in with Google, the same option also deletes every Mankeu backup and sync file in your Google Drive app folder, deletes the Mankeu sign-in account (Firebase Authentication), and revokes Google Drive access. Other files in your Google Drive are not touched.
- **Without opening the app:** clear the app's data in Android Settings › Apps › Mankeu › Storage, or uninstall it. Google Drive backups can be removed on Google Drive web: Settings › Manage apps › Mankeu › Delete hidden app data.
- **Request by email:** if none of the above is possible, email the contact address below from the Google address you signed in with. We will delete your sign-in account (Firebase Authentication). Your financial data never reaches our servers, so there is nothing else for us to delete.
- **Revoking Google access:** sign out in Settings at any time, or revoke Mankeu's access on your Google Account security page.

## 8. Children's privacy

Mankeu is meant for business owners and adult users (18+). It does not knowingly collect information from children.

## 9. Changes to this policy

Changes are announced by updating the date at the top of this page. The full history is kept in the project repository.

## 10. Contact

- **Email:** [adsurkasur.dev@gmail.com](mailto:adsurkasur.dev@gmail.com)
- **Developer:** Ade Surya Ananda (AdsurKasur)
- **Repository:** [github.com/adsurkasur/Mankeu](https://github.com/adsurkasur/Mankeu)
