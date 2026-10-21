package ly.jawdat.alhaya.data

import android.content.Context
import android.content.SharedPreferences

/** Small settings that are not business records: language and the default list of production stages. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    var arabic: Boolean
        get() = sp.getBoolean("arabic", true)
        set(v) = sp.edit().putBoolean("arabic", v).apply()

    /**
     * Stage names a new batch starts with. Saved per language so switching language doesn't mix them.
     * Herbs are not a stage: they are added to some litres of the batch at its last stage.
     */
    var stageTemplate: List<String>
        get() {
            val saved = sp.getString(templateKey, null)
            return saved?.split('\n')?.filter { it.isNotBlank() } ?: defaultStages(arabic)
        }
        set(v) {
            sp.edit().putString(templateKey, v.filter { it.isNotBlank() }.joinToString("\n")).apply()
        }

    // v2: the stages as she actually makes the vinegar (2026-10-16).
    private val templateKey get() = if (arabic) "stages_v2_ar" else "stages_v2_en"

    /** Days until the usual check of a known stage, offered when moving on to it (0 = none). */
    fun checkDaysFor(name: String): Int = when (name) {
        "تغيير الغطاء لغطاء قماشي", "Cloth cover" -> 30
        "التصفية والترقيد", "Filtering & settling" -> 5
        "نقع الأعشاب", "Herb soaking" -> 5
        else -> 0
    }

    /**
     * The welcome tour shows on first launch, and once more after an update that changes it
     * (raise [TOUR_VERSION]). Phones that saw the first tour have only the old "seen_welcome" flag.
     */
    var tourSeen: Int
        get() = sp.getInt("tour_seen", if (sp.getBoolean("seen_welcome", false)) 1 else 0)
        set(v) = sp.edit().putInt("tour_seen", v).apply()

    const val TOUR_VERSION = 3

    var reminderHour: Int
        get() = sp.getInt("reminder_hour", 9)
        set(v) = sp.edit().putInt("reminder_hour", v).apply()

    fun defaultStages(arabic: Boolean): List<String> =
        if (arabic) listOf(
            "النقع",
            "تغيير الغطاء لغطاء قماشي",
            "التصفية والترقيد",
            "التعبئة",
        ) else listOf(
            "Soaking",
            "Cloth cover",
            "Filtering & settling",
            "Bottling",
        )

    /** A short reminder of what happens in a known stage, shown under its name. */
    fun stageHint(name: String): String? = when (name) {
        "النقع" -> "نقع التمر في الماء"
        "Soaking" -> "Dates soaking in water"
        "تغيير الغطاء لغطاء قماشي" -> "يبقى بالغطاء القماشي حوالي شهر"
        "Cloth cover" -> "Stays under a cloth cover for about a month"
        "التصفية والترقيد" -> "بعد التصفية يُترك بضعة أيام حتى تترسب الرواسب الرقيقة"
        "Filtering & settling" -> "After filtering, left a few days for fine sediment to settle"
        "نقع الأعشاب" -> "للأنواع بالأعشاب فقط — يأخذ بضعة أيام"
        "Herb soaking" -> "Herb types only — takes a few days"
        "التعبئة" -> "التعبئة في القوارير وتسجيل عددها"
        "Bottling" -> "Fill the bottles and record how many"
        else -> null
    }
}
