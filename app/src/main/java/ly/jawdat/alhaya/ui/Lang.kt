package ly.jawdat.alhaya.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ly.jawdat.alhaya.data.Batch
import ly.jawdat.alhaya.data.ExpenseCategory
import ly.jawdat.alhaya.data.Herb
import ly.jawdat.alhaya.data.ItemKind
import ly.jawdat.alhaya.data.Prefs
import ly.jawdat.alhaya.data.Product
import ly.jawdat.alhaya.data.ProductSize
import ly.jawdat.alhaya.data.StageStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Arabic / English text. Every label is written as t("عربي", "English") right where it is used,
 * so the two languages never drift apart. Reading [Lang.arabic] inside Compose makes the whole
 * screen redraw when the language changes.
 */
object Lang {
    var arabic by mutableStateOf(true)

    fun set(arabic: Boolean) {
        this.arabic = arabic
        Prefs.arabic = arabic
    }
}

fun t(ar: String, en: String): String = if (Lang.arabic) ar else en

val currency: String get() = t("د.ل", "LYD")

/**
 * Keeps a left-to-right run (a number like "-75" or a code like "2026-001") in its own order
 * inside Arabic text; without this, right-to-left layout shows "75-" and "001-2026".
 */
fun ltr(s: String): String = "⁦$s⁩"

/** Money with up to 2 decimals and the currency: "12.5 د.ل". */
fun money(v: Double): String = "${ltr(num(v))} $currency"

/** "Batch 2026-001" / "دفعة 2026-001". */
val Batch.label: String get() = t("دفعة ", "Batch ") + ltr(code)

/** "330 مل" / "1 لتر"; older hand-typed sizes are shown as typed. */
fun sizeName(size: String): String = when (size) {
    ProductSize.TINY -> t("100 مل", "100 ml")
    ProductSize.SMALL -> t("330 مل", "330 ml")
    ProductSize.LARGE -> t("1 لتر", "1 L")
    else -> size
}

/** "خل التمر بالزعتر 330 مل": the name with its bottle size, used wherever a product is listed. */
val Product.title: String get() = "$name ${sizeName(size)}".trim()

/** A number with at most 2 decimals and no trailing zeros, always with Latin digits as used in Libya. */
fun num(v: Double): String {
    val r = (v * 100).roundToLong() / 100.0
    return if (abs(r - r.toLong()) < 0.001) r.toLong().toString()
    else String.format(Locale.US, "%.2f", r).trimEnd('0').trimEnd('.')
}

fun pct(v: Double): String = "${num(v)}%"

/** Reads a number typed with Arabic or Latin digits and either decimal sign. */
fun parseNum(s: String): Double? {
    val latin = buildString {
        s.trim().forEach { c ->
            append(
                when (c) {
                    in '٠'..'٩' -> '0' + (c - '٠')
                    in '۰'..'۹' -> '0' + (c - '۰')
                    '٫', ',' -> '.'
                    else -> c
                }
            )
        }
    }
    return latin.toDoubleOrNull()
}

private val dateFmt = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.US)

fun date(d: LocalDate?): String = d?.format(dateFmt) ?: "—"

fun dayName(d: DayOfWeek): String = when (d) {
    DayOfWeek.SATURDAY -> t("السبت", "Saturday")
    DayOfWeek.SUNDAY -> t("الأحد", "Sunday")
    DayOfWeek.MONDAY -> t("الإثنين", "Monday")
    DayOfWeek.TUESDAY -> t("الثلاثاء", "Tuesday")
    DayOfWeek.WEDNESDAY -> t("الأربعاء", "Wednesday")
    DayOfWeek.THURSDAY -> t("الخميس", "Thursday")
    DayOfWeek.FRIDAY -> t("الجمعة", "Friday")
}

/** "today", "in 3 days", "2 days ago". */
fun relative(d: LocalDate): String {
    val n = ChronoUnit.DAYS.between(LocalDate.now(), d)
    return when {
        n == 0L -> t("اليوم", "today")
        n == 1L -> t("غداً", "tomorrow")
        n == -1L -> t("أمس", "yesterday")
        n > 0 -> t("بعد ${arabicDays(n)}", "in $n days")
        else -> t("منذ ${arabicDays(-n)}", "${-n} days ago")
    }
}

/** Arabic counts change the noun: يوم واحد، يومان، 3 أيام، 11 يوماً. */
fun days(n: Long): String = t(arabicDays(n), if (n == 1L) "1 day" else "$n days")

private fun arabicDays(n: Long): String = when {
    n == 0L -> "0 يوم"
    n == 1L -> "يوم واحد"
    n == 2L -> "يومان"
    n % 100 in 3..10 -> "$n أيام"
    else -> "$n يوماً"
}

fun herbName(h: String): String = when (h) {
    Herb.PLAIN -> t("طبيعي (بدون أعشاب)", "Plain")
    Herb.THYME -> t("زعتر", "Thyme")
    Herb.ROSEMARY -> t("إكليل الجبل", "Rosemary")
    Herb.GINGER -> t("زنجبيل", "Ginger")
    Herb.GARLIC -> t("ثوم", "Garlic")
    Herb.FENNEL -> t("شمر", "Fennel")
    Herb.MIXED -> t("خليط أعشاب", "Mixed herbs")
    else -> t("أخرى", "Other")
}

fun itemKindName(k: String): String = when (k) {
    ItemKind.DATES -> t("تمر", "Dates")
    ItemKind.WATER -> t("ماء", "Water")
    ItemKind.HERB -> t("أعشاب", "Herbs")
    ItemKind.CONTAINER -> t("أوعية / براميل", "Containers")
    ItemKind.BOTTLES -> t("قوارير", "Bottles")
    ItemKind.LABELS -> t("ملصقات", "Labels")
    ItemKind.ENERGY -> t("غاز / كهرباء", "Gas / power")
    ItemKind.LABOR -> t("عمالة", "Labour")
    else -> t("أخرى", "Other")
}

fun expenseCategoryName(c: String): String = when (c) {
    ExpenseCategory.TRANSPORT -> t("نقل وتوصيل", "Transport")
    ExpenseCategory.PACKAGING -> t("تغليف", "Packaging")
    ExpenseCategory.EQUIPMENT -> t("معدات وأدوات", "Equipment")
    ExpenseCategory.MARKETING -> t("تسويق وإعلان", "Marketing")
    ExpenseCategory.UTILITIES -> t("فواتير", "Utilities")
    else -> t("أخرى", "Other")
}

fun stageStatusName(s: String): String = when (s) {
    StageStatus.PENDING -> t("لم تبدأ", "Waiting")
    StageStatus.ACTIVE -> t("جارية", "In progress")
    StageStatus.DONE -> t("تمت", "Done")
    else -> t("تم تخطيها", "Skipped")
}

val commonUnits: List<String> get() = listOf(t("كجم", "kg"), t("لتر", "L"), t("جرام", "g"), t("قطعة", "pcs"), t("قارورة", "bottle"))
