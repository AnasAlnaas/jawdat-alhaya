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
     * The herb-soaking stage is not part of it: it is added before the last stage for herb batches.
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

    val herbStage: String get() = if (arabic) "نقع الأعشاب" else "Herb soaking"

    /** The stages for a new batch of this type. */
    fun stagesFor(herb: String): List<String> {
        val base = stageTemplate
        if (herb == Herb.PLAIN || base.isEmpty()) return base
        return base.dropLast(1) + herbStage + base.last()
    }

    /** The welcome tour shows once, on first launch. */
    var seenWelcome: Boolean
        get() = sp.getBoolean("seen_welcome", false)
        set(v) = sp.edit().putBoolean("seen_welcome", v).apply()

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
