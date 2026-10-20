package ly.jawdat.alhaya.ui.screens

import androidx.annotation.DrawableRes
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.ui.t

/**
 * The user guide, one section per area of the app. Screens open it at their own section through
 * the "?" button (AppScreen's `help`), so keep the ids in step with the calls to AppScreen.
 */
data class GuideSection(
    val id: String,
    @DrawableRes val icon: Int,
    val title: String,
    val intro: String,
    val steps: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
)

object GuideIds {
    const val START = "start"
    const val HOME = "home"
    const val BATCHES = "batches"
    const val STAGES = "stages"
    const val BOTTLING = "bottling"
    const val SALES = "sales"
    const val MARKETS = "markets"
    const val PRODUCTS = "products"
    const val STOCK = "stock"
    const val EXPENSES = "expenses"
    const val REPORTS = "reports"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
    const val FAQ = "faq"
}

fun guideSections(): List<GuideSection> = listOf(
    GuideSection(
        GuideIds.START, R.drawable.ic_check,
        t("البدء السريع", "Quick start"),
        t("خمس خطوات لتجهيز التطبيق في أول يوم:", "Five steps to set the app up on day one:"),
        steps = listOf(
            t("المزيد ← المنتجات والأسعار: أضف كل نوع وحجم بسعره، مثل «خل التمر بالزعتر 330 مل». الحجم اختيار: 330 مل أو 1 لتر.", "More → Products & prices: add each type and size with its price, e.g. \"Date vinegar with thyme 330 ml\". The size is a choice: 330 ml or 1 L."),
            t("المزيد ← المحلات: أضف المحلات التي تبيع لها مع رقم الهاتف.", "More → Markets: add the markets you sell to, with their phone number."),
            t("عند نقع التمر اضغط «دفعة جديدة» واختر النوع.", "When you soak the dates, tap \"New batch\" and choose the type."),
            t("عند كل بيع اضغط «بيع جديد» من الرئيسية.", "For every sale, tap \"New sale\" on Home."),
            t("مرة كل أسبوع: الإعدادات ← حفظ نسخة احتياطية.", "Once a week: Settings → Save backup."),
        ),
        tips = listOf(
            t("زر «؟» أعلى كل شاشة يفتح هذا الدليل عند الجزء الخاص بها.", "The \"?\" button at the top of each screen opens this guide at its section."),
        ),
    ),
    GuideSection(
        GuideIds.HOME, R.drawable.ic_home,
        t("الرئيسية", "Home"),
        t("ملخص يومي لكل شيء في مكان واحد.", "A daily summary of everything in one place."),
        steps = listOf(
            t("زرا «بيع جديد» و«دفعة جديدة» في الأعلى للعمل السريع.", "\"New sale\" and \"New batch\" at the top for quick work."),
            t("«يحتاج انتباهك»: مراحل حان موعد متابعتها (برتقالي اليوم، أحمر متأخر)، ومخزون منخفض.", "\"Needs your attention\": stages whose check date has come (orange today, red late), and low stock."),
            t("«هذا الأسبوع»: المبيعات، القوارير المباعة، صافي الربح، والمبلغ المستلم نقداً. يبدأ الأسبوع يوم السبت.", "\"This week\": sales, bottles sold, net profit and cash received. The week starts on Saturday."),
            t("«الوضع الآن»: القوارير الجاهزة، الدفعات الجارية، ومجموع الديون على المحلات.", "\"Right now\": bottles ready, batches in progress, and total owed by markets."),
            t("«المراحل القادمة»: أقرب مواعيد المتابعة. اضغط على أي بطاقة لفتحها.", "\"Coming up\": the next check dates. Tap any card to open it."),
        ),
    ),
    GuideSection(
        GuideIds.BATCHES, R.drawable.ic_flask,
        t("الإنتاج والدفعات", "Production & batches"),
        t("كل كمية تمر تُنقع معاً هي «دفعة». تتابع فيها المراحل والتكاليف حتى التعبئة.", "Each lot of dates soaked together is a \"batch\". In it you follow the stages and costs until bottling."),
        steps = listOf(
            t("اضغط «دفعة جديدة». رقم الدفعة يُكتب تلقائياً (مثل 2026-001) ويمكن تغييره.", "Tap \"New batch\". The batch number is filled in (e.g. 2026-001) and can be changed."),
            t("اختر النوع. الأنواع بالأعشاب تُضاف لها مرحلة «نقع الأعشاب» قبل التعبئة تلقائياً.", "Choose the type. Herb types get the \"Herb soaking\" stage before bottling automatically."),
            t("المراحل الجاهزة: النقع ← تغيير الغطاء لغطاء قماشي ← التصفية والترقيد ← التعبئة. أزل العلامة عن أي مرحلة لا تحتاجها، أو أضف مرحلة.", "Ready stages: Soaking → Cloth cover → Filtering & settling → Bottling. Untick any you don't need, or add one."),
            t("اضغط «بدء الدفعة». تبدأ المرحلة الأولى في تاريخ البدء.", "Tap \"Start batch\". The first stage starts on the start date."),
            t("صفحة الدفعة تعرض: التكلفة، عدد القوارير، تكلفة القارورة، والمراحل بالترتيب.", "The batch page shows the cost, number of bottles, cost per bottle and the stages in order."),
        ),
        tips = listOf(
            t("زر الإعدادات ⚙ في صفحة الدفعة: تعديل البيانات، إلغاء الدفعة إذا فشلت، إعادة فتحها، أو حذفها.", "The ⚙ button on the batch page: edit details, cancel the batch if it failed, reopen it, or delete it."),
            t("زر QR يصنع ملصقاً فيه رقم الدفعة والنوع والتواريخ، للطباعة على القوارير.", "The QR button makes a label with the batch number, type and dates, to print for the bottles."),
            t("قائمة الإنتاج تعرض الدفعات الجارية، ويمكن عرض المنتهية أو الكل.", "The Production list shows batches in progress; you can also show finished ones or all."),
        ),
    ),
    GuideSection(
        GuideIds.STAGES, R.drawable.ic_calendar,
        t("المراحل والتذكيرات", "Stages & reminders"),
        t("اضغط على أي مرحلة في صفحة الدفعة لفتحها. كل ما تغيّره يُحفظ مباشرة.", "Tap any stage on the batch page to open it. Everything you change is saved at once."),
        steps = listOf(
            t("الحالة: لم تبدأ، جارية، تمت، أو تم تخطيها.", "Status: waiting, in progress, done or skipped."),
            t("«موعد المتابعة»: متى تتوقع أن تكون المرحلة جاهزة. أزرار ‎+3 ‎+7 ‎+14 ‎+21 ‎+30 تضيف أياماً من تاريخ البدء (للغطاء القماشي استعمل ‎+30).", "\"Check on\": when you expect the stage to be ready. The +3 +7 +14 +21 +30 buttons add days from the start date (use +30 for the cloth cover)."),
            t("مع تشغيل «تذكير في موعد المتابعة» يصلك إشعار صباح ذلك اليوم، ويتكرر كل يوم حتى تنتهي المرحلة.", "With \"Remind me\" on, a notification arrives that morning, and every day after until the stage is done."),
            t("اكتب ملاحظاتك: اللون، الرائحة، الحرارة، الرواسب...", "Write notes: colour, smell, temperature, sediment..."),
            t("«تمت المرحلة — ابدأ التالية» ينهي هذه المرحلة ويبدأ التي بعدها بتاريخ اليوم.", "\"Stage done — start the next\" finishes this stage and starts the next one today."),
        ),
        tips = listOf(
            t("«الكميات والتكاليف»: أضف كل ما استُخدم مع سعره الإجمالي، مثل «تمر 20 كجم = 150 د.ل». سجّل التمر بصنف «تمر» ووحدة «كجم» حتى يُحسب عدد القوارير لكل كجم.", "\"Quantities & costs\": add everything used with its total price, e.g. \"Dates 20 kg = 150 LYD\". Record dates with kind \"Dates\" and unit \"kg\" so bottles per kg can be worked out."),
            t("يمكن خصم البند من المخزون (المواد الخام) مباشرة.", "An item can be taken straight from stock (raw materials)."),
            t("الصور: «تصوير» أو «من المعرض». اضغط على صورة لتكبيرها أو حذفها.", "Photos: \"Camera\" or \"From gallery\". Tap a photo to enlarge or delete it."),
            t("زر ⚙ في المرحلة: نقلها للأعلى أو للأسفل، أو حذفها.", "The ⚙ button on a stage: move it up or down, or delete it."),
        ),
    ),
    GuideSection(
        GuideIds.BOTTLING, R.drawable.ic_box,
        t("التعبئة وإنهاء الدفعة", "Bottling & finishing a batch"),
        t("عند التعبئة تسجّلن عدد القوارير، فتدخل المخزون وتُعرف تكلفة القارورة.", "When bottling you record how many bottles, so they go into stock and the cost per bottle is known."),
        steps = listOf(
            t("في صفحة الدفعة ← «القوارير المنتجة» ← «+ تسجيل قوارير».", "On the batch page → \"Bottles made\" → \"+ Record bottles\"."),
            t("اختر المنتج واكتب العدد. يمكن تقسيم الدفعة على أكثر من منتج أو حجم (سجّل كل واحد على حدة).", "Choose the product and type the number. A batch can be split across several products or sizes (record each one)."),
            t("اضغط «إنهاء الدفعة (جاهزة للبيع)». تُعلَّم المراحل المتبقية كمنتهية.", "Tap \"Finish batch (ready to sell)\". Remaining stages are marked done."),
        ),
        tips = listOf(
            t("تكلفة القارورة = مجموع تكاليف الدفعة ÷ عدد القوارير.", "Cost per bottle = total batch cost ÷ number of bottles."),
            t("حذف سطر قوارير يطرحها من المخزون.", "Deleting a bottles line removes them from stock."),
        ),
    ),
    GuideSection(
        GuideIds.SALES, R.drawable.ic_cart,
        t("المبيعات", "Sales"),
        t("سجّل كل بيع لمحل أو لزبون مباشر. المخزون ينقص تلقائياً.", "Record every sale to a market or a direct customer. Stock goes down automatically."),
        steps = listOf(
            t("اضغط «بيع جديد».", "Tap \"New sale\"."),
            t("اختر المحل (أو «+ محل جديد» من القائمة)، ثم المنتج. يظهر بجانبه المتوفر في المخزون.", "Choose the market (or \"+ New market\" in the list), then the product. Its stock is shown next to it."),
            t("منتج غير موجود في القائمة؟ اختر «+ منتج جديد» من قائمة المنتجات وأضفه دون مغادرة البيع.", "Product not in the list? Choose \"+ New product\" in the product list and add it without leaving the sale."),
            t("الكمية بالزرين − و+، والسعر يُكتب تلقائياً من سعر المنتج ويمكن تغييره.", "Set the quantity with − and +; the price fills in from the product and can be changed."),
            t("الدفع: «مدفوع بالكامل»، «يدفع لاحقاً»، أو «دفع جزء» مع المبلغ المدفوع الآن.", "Payment: \"Paid in full\", \"Pays later\", or \"Paid part\" with the amount paid now."),
            t("«حفظ»، أو «حفظ وإضافة منتج آخر» لبيع عدة منتجات لنفس المحل.", "\"Save\", or \"Save & add another product\" to sell several products to the same market."),
        ),
        tips = listOf(
            t("قائمة المبيعات: اليوم، هذا الأسبوع، هذا الشهر، غير مدفوعة، أو الكل.", "Sales list: today, this week, this month, unpaid, or all."),
            t("اضغط على أي بيع لتعديله أو حذفه (🗑). الحذف يعيد القوارير للمخزون.", "Tap any sale to edit or delete it (🗑). Deleting puts the bottles back in stock."),
            t("«يدفع لاحقاً» يحتاج اختيار محل حتى يُسجّل الدين عليه.", "\"Pays later\" needs a market so the debt is recorded against it."),
            t("إذا بعت أكثر من المخزون المسجّل، تُضاف القوارير الناقصة إلى المخزون تلقائياً مع البيع، فلا تحتاج لتعديل المخزون أولاً.", "If you sell more than the recorded stock, the missing bottles are added to stock automatically with the sale, so there's no need to adjust stock first."),
            t("🎁 الهدايا: اختر «هدية» أعلى شاشة البيع لقوارير مجانية، أو «قوارير هدية مع البيع» لإضافة قوارير مجانية إلى بيع. تنقص من المخزون وتُحسب تكلفتها في التقارير دون أن تُحسب كمبيعات.", "🎁 Gifts: choose \"Gift\" at the top of the sale screen for free bottles, or \"Gift bottles with this sale\" to add free bottles to a sale. They leave stock and their cost shows in reports without counting as sales."),
        ),
    ),
    GuideSection(
        GuideIds.MARKETS, R.drawable.ic_store,
        t("المحلات والديون", "Markets & debts"),
        t("لكل محل صفحة فيها مشترياته وما عليه والدفعات المستلمة.", "Each market has a page with what it bought, what it owes and the payments received."),
        steps = listOf(
            t("المزيد ← المحلات ← «محل جديد»: الاسم، الهاتف، المنطقة، ملاحظات.", "More → Markets → \"New market\": name, phone, area, notes."),
            t("في صفحة المحل: «اتصال» أو «واتساب» مباشرة.", "On the market page: \"Call\" or \"WhatsApp\" directly."),
            t("عندما يدفع المحل ما عليه: «تسجيل دفعة من المحل». المبلغ يُخصم من أقدم المبيعات أولاً.", "When a market pays what it owes: \"Record a payment\". It pays off the oldest sales first."),
            t("«إرسال كشف حساب» يرسل للمحل قائمة المبيعات غير المدفوعة والمجموع.", "\"Send statement\" sends the market its unpaid sales and the total."),
            t("«بيع لهذا المحل» يفتح بيعاً جديداً والمحل مختار.", "\"Sell to this market\" opens a new sale with the market already chosen."),
        ),
        tips = listOf(
            t("المحل الذي توقف عن الشراء يمكن أرشفته من زر التعديل ✎ دون حذف سجله.", "A market that stopped buying can be archived from the edit button ✎ without losing its history."),
        ),
    ),
    GuideSection(
        GuideIds.PRODUCTS, R.drawable.ic_money,
        t("المنتجات والأسعار", "Products & prices"),
        t("كل نوع وحجم منتج منفصل بسعره ومخزونه.", "Each type and size is a separate product with its own price and stock."),
        steps = listOf(
            t("«منتج جديد»: اختر النوع فيُكتب الاسم تلقائياً، ثم الحجم (قارورة صغيرة 330 مل أو كبيرة 1 لتر) والسعر.", "\"New product\": choose the type and the name fills in, then the size (small 330 ml or large 1 L bottle) and the price."),
            t("«المخزون الحالي» للقوارير الموجودة الآن، و«نبّهني عند» للتنبيه عند انخفاضه.", "\"Current stock\" for the bottles there now, and \"Warn me at\" for a low-stock alert."),
            t("«تعديل المخزون» لتصحيح العدد بعد عدّ يدوي أو قوارير مكسورة.", "\"Adjust stock\" to correct the count after a manual count or broken bottles."),
        ),
        tips = listOf(
            t("زر المشاركة أعلى الشاشة يرسل قائمة الأسعار عبر واتساب أو تيليجرام.", "The share button at the top sends the price list by WhatsApp or Telegram."),
            t("بعد أول دفعة منتهية تظهر تكلفة القارورة والربح لكل منتج.", "After the first finished batch, each product shows its cost and profit per bottle."),
            t("تغيير السعر لا يغيّر المبيعات القديمة.", "Changing a price doesn't change past sales."),
        ),
    ),
    GuideSection(
        GuideIds.STOCK, R.drawable.ic_box,
        t("المخزون", "Stock"),
        t("نوعان: القوارير الجاهزة للبيع، والمواد الخام والتغليف.", "Two kinds: bottles ready to sell, and raw materials & packaging."),
        steps = listOf(
            t("القوارير الجاهزة تزيد عند التعبئة وتنقص عند البيع أو الإهداء تلقائياً. بيع أكثر من المسجّل يضيف الفرق إلى المخزون مع البيع.", "Bottles ready go up when bottling and down when selling or gifting, automatically. Selling more than recorded adds the difference to stock with the sale."),
            t("«مادة جديدة» للتمر، الأعشاب، القوارير الفارغة، الملصقات... مع الوحدة وحد التنبيه.", "\"New material\" for dates, herbs, empty bottles, labels... with a unit and a warning level."),
            t("«تعديل» لكتابة الكمية الموجودة الآن بعد الشراء أو العدّ.", "\"Adjust\" to type the amount there now, after buying or counting."),
        ),
    ),
    GuideSection(
        GuideIds.EXPENSES, R.drawable.ic_chart,
        t("المصاريف", "Expenses"),
        t("مصاريف عامة غير مرتبطة بدفعة: نقل، معدات، إعلانات، فواتير...", "General costs not tied to a batch: transport, equipment, ads, bills..."),
        steps = listOf(
            t("«مصروف جديد»: الفئة، الوصف، المبلغ، التاريخ.", "\"New expense\": category, description, amount, date."),
        ),
        tips = listOf(
            t("تكاليف التمر والأعشاب تُسجَّل داخل مراحل الدفعة، لا هنا، حتى لا تُحسب مرتين.", "Dates and herbs go inside the batch stages, not here, so they aren't counted twice."),
        ),
    ),
    GuideSection(
        GuideIds.REPORTS, R.drawable.ic_chart,
        t("التقارير", "Reports"),
        t("اختر الفترة: هذا الأسبوع، الأسبوع الماضي، هذا الشهر، الشهر الماضي، هذه السنة، أو فترة مخصصة.", "Choose the period: this week, last week, this month, last month, this year, or custom."),
        steps = listOf(
            t("المبيعات: الإيرادات، القوارير، عدد عمليات البيع، متوسط السعر، والمقارنة مع الفترة السابقة.", "Sales: revenue, bottles, number of sales, average price, and the change from the previous period."),
            t("الربح: صافي الربح = المبيعات − تكاليف الإنتاج في الفترة − المصاريف.", "Profit: net profit = sales − production costs in the period − expenses."),
            t("«الربح على المبيعات» يستعمل تكلفة القارورة من الدفعات المنتهية، و«التدفق النقدي» = ما استلمته فعلاً − ما صرفته.", "\"Profit on sales\" uses the cost per bottle from finished batches; \"cash flow\" = what you actually received − what you spent."),
            t("🎁 الهدايا: عدد القوارير المهداة في الفترة، تكلفتها التقديرية، وقيمتها بسعر البيع. تكلفتها داخلة في «الربح على المبيعات».", "🎁 Gifts: bottles given free in the period, their estimated cost and their value at selling price. Their cost is included in \"Profit on sales\"."),
            t("حسب المنتج، النوع (الأعشاب)، المحل، ويوم الأسبوع.", "By product, type (herb), market and day of the week."),
            t("«يحتاج متابعة»: منتجات لم تُبع، ومحلات لم تشترِ منذ 30 يوماً.", "\"Worth a look\": products not sold, and markets with no purchase for 30 days."),
            t("الديون، الإنتاج (مدة كل مرحلة، قوارير لكل كجم تمر)، والمخزون.", "Debts, production (time per stage, bottles per kg of dates) and stock."),
        ),
        tips = listOf(
            t("PDF: تقرير كامل للطباعة أو الإرسال. Excel: كل مبيعات الفترة في جدول. «ملخص نصي»: رسالة قصيرة للواتساب.", "PDF: a full report to print or send. Excel: every sale in the period as a table. \"Text summary\": a short message for WhatsApp."),
        ),
    ),
    GuideSection(
        GuideIds.BACKUP, R.drawable.ic_backup,
        t("النسخ الاحتياطي", "Backup"),
        t("كل البيانات محفوظة على هذا الهاتف فقط. النسخة الاحتياطية تحميها إذا ضاع الهاتف أو تعطّل.", "All data is kept on this phone only. A backup protects it if the phone is lost or broken."),
        steps = listOf(
            t("المزيد ← الإعدادات ← «حفظ نسخة احتياطية» واختر Google Drive أو مجلد في الهاتف.", "More → Settings → \"Save backup\" and choose Google Drive or a folder on the phone."),
            t("أو «إرسال النسخة» لإرسالها لنفسك عبر واتساب أو تيليجرام.", "Or \"Send backup\" to send it to yourself by WhatsApp or Telegram."),
            t("لنقل البيانات لهاتف جديد: ثبّت التطبيق ثم «استرجاع من نسخة احتياطية» واختر الملف.", "To move to a new phone: install the app, then \"Restore from a backup\" and pick the file."),
        ),
        tips = listOf(
            t("احفظ نسخة مرة كل أسبوع على الأقل.", "Save a backup at least once a week."),
            t("الاسترجاع يستبدل كل البيانات الحالية بالنسخة.", "Restoring replaces all current data with the backup."),
        ),
    ),
    GuideSection(
        GuideIds.SETTINGS, R.drawable.ic_settings,
        t("الإعدادات", "Settings"),
        t("من المزيد ← الإعدادات والنسخ الاحتياطي.", "From More → Settings & backup."),
        steps = listOf(
            t("اللغة: العربية أو English.", "Language: Arabic or English."),
            t("«تعديل المراحل الافتراضية»: غيّر أسماء المراحل أو ترتيبها للدفعات الجديدة.", "\"Edit the default stages\": rename or reorder the stages for new batches."),
            t("ساعة التذكير اليومي: 7، 9، 12، 4 عصراً، أو 8 مساءً.", "Daily reminder time: 7, 9, 12, 4 pm or 8 pm."),
        ),
    ),
    GuideSection(
        GuideIds.FAQ, R.drawable.ic_info,
        t("أسئلة شائعة", "Common questions"),
        t("حلول سريعة لأكثر المواقف تكراراً.", "Quick answers to the most common situations."),
        steps = listOf(
            t("سجّلتُ بيعاً بالخطأ؟ افتحه من قائمة المبيعات واضغط 🗑. تعود القوارير للمخزون.", "Recorded a sale by mistake? Open it from the Sales list and tap 🗑. The bottles go back to stock."),
            t("المحل دفع جزءاً مما عليه؟ صفحة المحل ← «تسجيل دفعة» واكتب المبلغ.", "A market paid part of what it owes? Market page → \"Record a payment\" and type the amount."),
            t("فشلت دفعة؟ صفحة الدفعة ← ⚙ ← «إلغاء الدفعة». تبقى تكاليفها في التقارير.", "A batch failed? Batch page → ⚙ → \"Cancel batch\". Its costs stay in the reports."),
            t("لا تصل التذكيرات؟ تأكد من السماح بالإشعارات للتطبيق، ومن تشغيل «تذكير» في المرحلة ووضع موعد متابعة.", "Reminders not arriving? Check notifications are allowed for the app, and the stage has \"Remind me\" on and a check date."),
            t("تغيّر السعر؟ عدّل المنتج. المبيعات القديمة تبقى بسعرها.", "Price changed? Edit the product. Past sales keep their price."),
            t("هاتف جديد؟ احفظ نسخة احتياطية في الهاتف القديم ثم استرجعها في الجديد.", "New phone? Save a backup on the old phone, then restore it on the new one."),
        ),
    ),
)
