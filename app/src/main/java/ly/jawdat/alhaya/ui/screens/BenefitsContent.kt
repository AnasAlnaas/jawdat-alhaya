package ly.jawdat.alhaya.ui.screens

import ly.jawdat.alhaya.ui.t

/**
 * One card of "benefits & uses", shown in the app and shared as a picture.
 *
 * The wording follows the business's own posters, kept to what date vinegar can fairly claim
 * ("helps", "may help"); every benefit picture carries the note that it is general information,
 * not medical advice. A use can have a [recipe]: ingredients joined by "+" and "=" like the posters,
 * where [BOTTLE] stands for the drawn vinegar bottle.
 */
data class BenefitCard(
    val emoji: String,
    val tag: String,
    val title: String,
    val accent: String,
    val points: List<String>,
    val how: String? = null,
    val recipe: List<Pair<String, String>> = emptyList(),
)

object BenefitKind {
    const val BENEFIT = "benefit"
    const val USE = "use"
}

const val BOTTLE = "BOTTLE"

/** The phone number and social name printed on shared pictures and price lists. */
object Brand {
    const val PHONE = "0913390662"
    const val HANDLE = "jwdtalhya"
}

fun benefitCards(kind: String): List<BenefitCard> = if (kind == BenefitKind.USE) uses() else benefits()

private fun benefits() = listOf(
    BenefitCard(
        "🍽️", t("سكر الدم", "Blood sugar"),
        t("ملعقة قبل الوجبة", "A spoon before the meal"),
        t("تُغيّر ما بعد الوجبة", "changes what comes after"),
        listOf(
            t("حمض الخليك يبطئ هضم النشويات، فيصعد السكر بعد الأكل بهدوء أكبر", "Acetic acid slows the digestion of starch, so sugar rises more gently after eating"),
            t("طاقة أكثر استقراراً وخمول أقل بعد الوجبة", "Steadier energy and less sleepiness after the meal"),
            t("نكهة مميزة تساعد على ضبط الرغبة في الحلويات دون سكر مضاف", "A distinct taste that helps curb the urge for sweets, with no added sugar"),
        ),
        how = t("الجرعة: ملعقة إلى ملعقتين في كوب ماء قبل الوجبة أو خلالها.", "Dose: one to two spoons in a glass of water before or with the meal."),
    ),
    BenefitCard(
        "🌿", t("الهضم", "Digestion"),
        t("راحة بعد الأكل", "Comfort after eating"),
        t("وهضم أخف", "and lighter digestion"),
        listOf(
            t("يساعد على تخفيف النفخة والغازات", "Helps ease bloating and gas"),
            t("يدعم الهضم بعد الوجبات الثقيلة", "Supports digestion after heavy meals"),
            t("مشروب يومي بسيط: ماء وخل تمر", "A simple daily drink: water and date vinegar"),
        ),
        how = t("ملعقتان من خل التمر في كوب ماء.", "Two spoons of date vinegar in a glass of water."),
    ),
    BenefitCard(
        "⚖️", t("الوزن", "Weight"),
        t("شعور أطول", "Feeling full"),
        t("بالشبع", "for longer"),
        listOf(
            t("يساعد على الشعور بالشبع فتقلّ الكمية المأكولة", "Helps you feel full, so you eat a little less"),
            t("بديل خفيف للمشروبات المحلّاة", "A light swap for sweet drinks"),
            t("يفيد ضمن نظام غذائي متوازن وحركة يومية", "Works best with a balanced diet and daily movement"),
        ),
        how = t("ملعقة إلى ملعقتين في كوب ماء قبل الوجبة.", "One to two spoons in a glass of water before the meal."),
    ),
    BenefitCard(
        "❤️", t("القلب", "Heart"),
        t("عادة صغيرة", "A small habit"),
        t("لقلب مرتاح", "for a happy heart"),
        listOf(
            t("قد يساعد على تحسين مستوى الدهون في الدم ضمن نظام صحي", "May help improve blood fats as part of a healthy diet"),
            t("فيه من معادن التمر مثل البوتاسيوم", "Carries minerals from the dates, such as potassium"),
            t("بديل طبيعي للصلصات المالحة والدهنية", "A natural swap for salty, fatty sauces"),
        ),
    ),
    BenefitCard(
        "🌴", t("خل التمر الطبيعي", "Natural date vinegar"),
        t("من التمر فقط", "Made from dates only"),
        t("مع أم الخل", "with the mother"),
        listOf(
            t("تخمير طبيعي بطيء دون مواد حافظة أو إضافات", "Slow natural fermentation, no preservatives or additives"),
            t("«أم الخل» في القارورة علامة الخل الطبيعي الحي", "The \"mother\" in the bottle is the sign of living natural vinegar"),
            t("بدون سكر مضاف ومناسب لنظام الكيتو", "No added sugar and keto friendly"),
        ),
        how = t("رجّ القارورة قبل الاستعمال، والرواسب في القاع طبيعية.", "Shake before use; the sediment at the bottom is natural."),
    ),
)

private fun uses() = listOf(
    BenefitCard(
        "🍞", t("المطبخ", "Kitchen"),
        t("خميرة طبيعية", "Natural leaven"),
        t("للخبز", "for bread"),
        listOf(
            t("أضف ملعقتين من خل التمر إلى العجين مع الماء", "Add two spoons of date vinegar to the dough with the water"),
            t("يساعد العجين على الانتفاخ ويعطي الخبز طراوة", "Helps the dough rise and keeps the bread soft"),
            t("للخبز والفطائر والمعجنات", "For bread, pies and pastries"),
        ),
        recipe = listOf("🥣" to t("عجين", "Dough"), BOTTLE to t("ملعقتان من خل التمر", "2 spoons of date vinegar"), "🍞" to t("خميرة طبيعية للخبز", "Natural leaven")),
    ),
    BenefitCard(
        "🥛", t("مشروب يومي", "Daily drink"),
        t("كوب قبل الوجبة", "A glass before meals"),
        t("لهضم أريح", "for easier digestion"),
        listOf(
            t("كوب ماء مع ملعقتين من خل التمر", "A glass of water with two spoons of date vinegar"),
            t("يمكن إضافة ملعقة عسل أو شريحة ليمون", "Add a spoon of honey or a slice of lemon if you like"),
            t("ماء فاتر في الشتاء وبارد في الصيف", "Warm water in winter, cool in summer"),
        ),
        recipe = listOf("💧" to t("كوب ماء", "Glass of water"), BOTTLE to t("ملعقتان من خل التمر", "2 spoons of date vinegar"), "🥛" to t("مشروب للهضم", "A drink for digestion")),
    ),
    BenefitCard(
        "🥗", t("السلطة", "Salad"),
        t("تتبيلة سلطة", "Salad dressing"),
        t("في دقيقة", "in a minute"),
        listOf(
            t("3 ملاعق زيت زيتون وملعقة خل تمر ورشة ملح وثوم", "3 spoons olive oil, 1 spoon date vinegar, a pinch of salt and garlic"),
            t("نكهة منعشة دون صلصات جاهزة", "A fresh taste without ready-made sauces"),
        ),
        recipe = listOf("🫒" to t("زيت زيتون", "Olive oil"), BOTTLE to t("ملعقة خل تمر", "1 spoon date vinegar"), "🥗" to t("تتبيلة سلطة", "Salad dressing")),
    ),
    BenefitCard(
        "🍗", t("التتبيل", "Marinade"),
        t("لحم ودجاج", "Meat and chicken"),
        t("أطرى وألذ", "more tender and tasty"),
        listOf(
            t("انقع اللحم أو الدجاج في خل التمر مع البهارات ساعة على الأقل", "Soak meat or chicken in date vinegar with spices for at least an hour"),
            t("يطري اللحم ويخفف رائحته", "Makes the meat tender and softens its smell"),
        ),
        recipe = listOf("🍗" to t("لحم أو دجاج", "Meat or chicken"), BOTTLE to t("خل التمر والبهارات", "Date vinegar and spices"), "😋" to t("تتبيلة طرية", "Tender marinade")),
    ),
    BenefitCard(
        "🍎", t("التنظيف", "Cleaning"),
        t("غسل الخضار", "Washing fruit"),
        t("والفواكه", "and vegetables"),
        listOf(
            t("ربع كوب خل تمر في وعاء ماء، انقع الخضار 10 دقائق ثم اشطفها", "A quarter cup of date vinegar in a bowl of water; soak 10 minutes, then rinse"),
            t("يساعد على إزالة الأتربة وبقايا الرش عن السطح", "Helps lift dirt and spray residue off the surface"),
        ),
        recipe = listOf("💧" to t("وعاء ماء", "Bowl of water"), BOTTLE to t("ربع كوب خل تمر", "¼ cup date vinegar"), "🍎" to t("خضار نظيفة", "Clean produce")),
    ),
    BenefitCard(
        "💡", t("نصائح", "Tips"),
        t("الطريقة الصحيحة", "The right way"),
        t("لاستعمال الخل", "to take vinegar"),
        listOf(
            t("يُخفف دائماً بالماء، ولا يُشرب صافياً", "Always dilute it in water; never drink it neat"),
            t("اشطف فمك بالماء بعده لحماية مينا الأسنان", "Rinse your mouth with water after, to protect tooth enamel"),
            t("من يعاني من قرحة المعدة أو يتناول أدوية السكر يستشير الطبيب أولاً", "With a stomach ulcer or diabetes medicine, ask your doctor first"),
            t("يُحفظ بعيداً عن الشمس، والرواسب في القاع طبيعية", "Keep it out of the sun; sediment at the bottom is natural"),
        ),
    ),
)
