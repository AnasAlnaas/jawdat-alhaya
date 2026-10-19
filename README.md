# جودة الحياة — Jawdat Al-Hayah

Android app (Android 13+) for running a small date-vinegar business: production batches and their
stages, sales to markets, debts, stock, expenses and weekly/monthly reports. Arabic (default) and English.

Everything is stored on the phone (SQLite). No internet permission. Backups are .zip files the user saves
to Google Drive or sends to themselves.

## Production stages

Default stages for a new batch (editable in Settings):

1. النقع — soaking the dates
2. تغيير الغطاء لغطاء قماشي — cloth cover, about a month
3. التصفية والترقيد — filtering, then a few days for fine sediment to settle
4. التعبئة — bottling

Herb types (زعتر، إكليل الجبل، زنجبيل، ثوم، شمر، خليط) also get **نقع الأعشاب** (herb soaking, a few days)
just before bottling. Durations are not fixed: each stage has a "check on" date that she sets.

## Build

```bash
./gradlew assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/app-release.apk`. Signing reads
`keystore.properties` (ignored by git), which points at the key in `D:\code\jawdat_alhaya-keys\`.
**Every update must be signed with that same key**, or the phone refuses to install it over the old
version. Keep a copy of the keys folder somewhere safe.

Before each release, raise `versionCode` (and `versionName`) in `app/build.gradle.kts`.

`./gradlew assembleDebug` builds an unsigned test build (installs separately; can't update the release).

## Installing on the phone

Send the APK (WhatsApp or USB), open it, and allow "install unknown apps" for that app once.
To update later, open the new APK the same way; her data stays.

## Code map

| Path | What it holds |
|---|---|
| `data/Db.kt` | Database tables |
| `data/Repo.kt` | All reads and writes; stock and payment bookkeeping |
| `data/Prefs.kt` | Language, reminder hour, default stages and their hints |
| `logic/Reports.kt` | Every number on the Reports screen |
| `reminders/` | Daily check (09:00–10:00 by default) for stages whose "check on" date has come |
| `util/Files.kt` | QR labels, PDF report, Excel (CSV) export, backup/restore, photos |
| `ui/Lang.kt` | Arabic/English text helper `t("عربي", "English")` |
| `ui/screens/GuideContent.kt` | The user guide text (one section per area; ids used by the "?" buttons) |
| `ui/screens/` | The screens |

## Money rules

- A sale's payment is recorded as a payment row. Later "pays what they owe" payments are applied to that
  market's oldest unpaid sales first.
- Production costs are the prices typed on each stage item. Cost per bottle = batch cost ÷ bottles made.
- Net profit for a period = sales − stage-item costs dated in the period − other expenses.

## Versions

- **1.1.0** (2026-10-16) — in-app user guide (searchable, one section per screen, "?" button on every screen), first-launch welcome tour, user guide in the More menu.
- **1.0.0** (2026-10-16) — first release.
