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
| `ui/Lang.kt` | Arabic/English text helper `t("عربي", "English")`, size names |
| `ui/Motion.kt` | Drawn vinegar bottle, size and type pickers, quantity stepper, entrance/pop animations |
| `ui/Illustrations.kt` | The animated pictures of the welcome tour |
| `ui/screens/GuideContent.kt` | The user guide text (one section per area; ids used by the "?" buttons) |
| `ui/screens/` | The screens |

## Money rules

- A sale's payment is recorded as a payment row. Later "pays what they owe" payments are applied to that
  market's oldest unpaid sales first.
- Production costs are the prices typed on each stage item. Cost per bottle = batch cost ÷ bottles made.
- Net profit for a period = sales − stage-item costs dated in the period − other expenses.
- Bottle sizes are a fixed choice stored as ml: `330` (small) or `1000` (1 L).
- Selling (or gifting) more bottles than the recorded stock adds the shortfall to stock in the same step; the
  sale remembers it (`stock_added`) so editing or deleting the sale puts stock back exactly.
- Gift bottles (`gift_qty`) leave stock but add no revenue. Their estimated cost counts in "cost of bottles"
  and "profit on sales"; the Reports screen shows how many were given, their cost and their value at price.

## Versions

- **1.2.0** (2026-10-19) — bottle size is a choice (330 ml / 1 L); add a product from inside a sale or bottling;
  selling more than recorded stock adds the difference automatically; gift bottles (alone or with a sale) counted
  in reports, PDF and Excel; animated welcome tour (shown once more after updating), screen transitions, drawn
  bottles, animated charts and numbers. Database version 2.
- **1.1.0** (2026-10-16) — in-app user guide (searchable, one section per screen, "?" button on every screen), first-launch welcome tour, user guide in the More menu.
- **1.0.0** (2026-10-16) — first release.
