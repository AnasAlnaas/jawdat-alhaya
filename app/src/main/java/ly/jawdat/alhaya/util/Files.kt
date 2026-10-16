package ly.jawdat.alhaya.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Db
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.logic.Report
import ly.jawdat.alhaya.ui.Lang
import ly.jawdat.alhaya.ui.date
import ly.jawdat.alhaya.ui.dayName
import ly.jawdat.alhaya.ui.days
import ly.jawdat.alhaya.ui.expenseCategoryName
import ly.jawdat.alhaya.ui.herbName
import ly.jawdat.alhaya.ui.itemKindName
import ly.jawdat.alhaya.ui.money
import ly.jawdat.alhaya.ui.num
import ly.jawdat.alhaya.ui.pct
import ly.jawdat.alhaya.ui.t
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Files {

    private fun shareDir(context: Context) = File(context.cacheDir, "share").apply { mkdirs() }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, t("مشاركة", "Share")))
    }

    fun shareFile(context: Context, file: File, mime: String) {
        val send = Intent(Intent.ACTION_SEND).setType(mime)
            .putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, t("مشاركة", "Share")))
    }

    fun call(context: Context, phone: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }

    /** Opens a WhatsApp chat; Libyan numbers like 091xxxxxxx become 21891xxxxxxx. */
    fun whatsapp(context: Context, phone: String) {
        var digits = phone.filter { it.isDigit() }
        if (digits.startsWith("0")) digits = "218" + digits.drop(1)
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))) }
    }

    // ---------- QR ----------

    fun qrBitmap(text: String, size: Int = 600): Bitmap {
        val matrix = QRCodeWriter().encode(
            text, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 1),
        )
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        return bmp
    }

    /** A printable label: the QR code with the batch code and text under it, saved as PNG and shared. */
    fun shareQrLabel(context: Context, qrText: String, caption: List<String>) {
        val qr = qrBitmap(qrText, 600)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 34f
            color = Color.BLACK
            typeface = context.resources.getFont(R.font.readex_pro)
        }
        val lineH = 46
        val out = Bitmap.createBitmap(640, 640 + caption.size * lineH + 20, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        c.drawBitmap(qr, 20f, 20f, null)
        caption.forEachIndexed { i, line ->
            val w = paint.measureText(line)
            c.drawText(line, (640 - w) / 2, 660f + i * lineH, paint)
        }
        val file = File(shareDir(context), "qr.png")
        FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shareFile(context, file, "image/png")
    }

    // ---------- Photos ----------

    /** Copies a picked photo into the app, shrunk to at most 1600 px so backups stay small. */
    fun importPhoto(context: Context, uri: Uri): String? = runCatching {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        val bmp = ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            val max = maxOf(info.size.width, info.size.height)
            if (max > 1600) {
                val scale = 1600f / max
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val name = "p_${System.currentTimeMillis()}.jpg"
        FileOutputStream(File(Repo.photosDir, name)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        name
    }.getOrNull()

    // ---------- CSV (opens in Excel) ----------

    fun shareSalesCsv(context: Context, from: LocalDate, to: LocalDate) {
        val views = Repo.saleViews().filter { !it.sale.date.isBefore(from) && !it.sale.date.isAfter(to) }.sortedBy { it.sale.date }
        val products = Repo.products(true).associateBy { it.id }
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val header = listOf(
            t("التاريخ", "Date"), t("المحل", "Market"), t("المنتج", "Product"), t("النوع", "Type"),
            t("الكمية", "Qty"), t("سعر الوحدة", "Unit price"), t("الإجمالي", "Total"),
            t("المدفوع", "Paid"), t("المتبقي", "Owed"), t("ملاحظات", "Notes"),
        )
        val rows = views.map { v ->
            listOf(
                v.sale.date.toString(), v.marketName ?: "", v.productName,
                herbName(products[v.sale.productId]?.herb ?: ""),
                v.sale.qty.toString(), num(v.sale.unitPrice), num(v.sale.total), num(v.paid), num(v.owed), v.sale.notes,
            )
        }
        val csv = buildString {
            append('﻿') // lets Excel read Arabic correctly
            append(header.joinToString(",") { esc(it) }).append("\r\n")
            rows.forEach { append(it.joinToString(",") { c -> esc(c) }).append("\r\n") }
        }
        val file = File(shareDir(context), "sales_${from}_$to.csv")
        file.writeText(csv, Charsets.UTF_8)
        shareFile(context, file, "text/csv")
    }

    // ---------- PDF report ----------

    fun shareReportPdf(context: Context, r: Report, businessName: String) {
        val pdf = PdfWriter(context)
        pdf.title(businessName)
        pdf.subtitle(t("تقرير من ${date(r.period.from)} إلى ${date(r.period.to)}", "Report ${date(r.period.from)} – ${date(r.period.to)}"))

        pdf.heading(t("المبيعات", "Sales"))
        pdf.row(t("الإيرادات", "Revenue"), money(r.revenue))
        pdf.row(t("القوارير المباعة", "Bottles sold"), r.units.toString())
        pdf.row(t("عدد عمليات البيع", "Number of sales"), r.salesCount.toString())
        pdf.row(t("متوسط سعر القارورة", "Average price per bottle"), money(r.avgUnitPrice))
        pdf.row(t("المبلغ المستلم نقداً", "Cash received"), money(r.cashReceived))
        pdf.row(t("غير مدفوع من مبيعات الفترة", "Unpaid from this period"), money(r.unpaidFromPeriod))
        r.change(r.revenue, r.prevRevenue)?.let { pdf.row(t("التغير عن الفترة السابقة", "Change vs previous period"), pct(it)) }

        pdf.heading(t("التكاليف والربح", "Costs & profit"))
        pdf.row(t("تكاليف الإنتاج", "Production costs"), money(r.productionCost))
        pdf.row(t("مصاريف أخرى", "Other expenses"), money(r.expenses))
        pdf.row(t("صافي الربح", "Net profit"), money(r.netProfit))
        pdf.row(t("تكلفة البضاعة المباعة (تقديري)", "Cost of bottles sold (est.)"), money(r.costOfGoodsSold))
        pdf.row(t("الربح الإجمالي على المبيعات", "Gross profit on sales"), money(r.grossProfit))
        r.productionByKind.forEach { pdf.row("  " + itemKindName(it.label), money(it.value)) }
        r.expensesByCategory.forEach { pdf.row("  " + expenseCategoryName(it.label), money(it.value)) }

        if (r.byProduct.isNotEmpty()) {
            pdf.heading(t("حسب المنتج", "By product"))
            r.byProduct.forEach { pdf.row(it.label, t("${it.units} قارورة · ${money(it.value)}", "${it.units} btl · ${money(it.value)}")) }
        }
        if (r.byHerb.isNotEmpty()) {
            pdf.heading(t("حسب النوع", "By type"))
            r.byHerb.forEach { pdf.row(herbName(it.label), t("${it.units} قارورة · ${money(it.value)}", "${it.units} btl · ${money(it.value)}")) }
        }
        if (r.byMarket.isNotEmpty()) {
            pdf.heading(t("حسب المحل", "By market"))
            r.byMarket.forEach { pdf.row(it.label, t("${it.units} قارورة · ${money(it.value)}", "${it.units} btl · ${money(it.value)}")) }
        }
        if (r.revenue > 0) {
            pdf.heading(t("حسب يوم الأسبوع", "By day of week"))
            r.byWeekday.filter { it.second > 0 }.forEach { pdf.row(dayName(it.first), money(it.second)) }
        }

        pdf.heading(t("الديون المستحقة (الآن)", "Money owed (now)"))
        pdf.row(t("الإجمالي", "Total"), money(r.owedTotal))
        r.owedByMarket.forEach { pdf.row(it.label, money(it.value)) }

        pdf.heading(t("الإنتاج", "Production"))
        pdf.row(t("دفعات بدأت", "Batches started"), r.batchesStarted.toString())
        pdf.row(t("دفعات انتهت", "Batches finished"), r.batchesFinished.toString())
        pdf.row(t("دفعات جارية الآن", "Batches in progress now"), r.batchesActive.toString())
        pdf.row(t("قوارير أُنتجت", "Bottles made"), r.bottlesMade.toString())
        r.avgBatchDays?.let { pdf.row(t("متوسط مدة الدفعة", "Average batch length"), days(it.toLong())) }
        r.yieldPerKg?.let { pdf.row(t("قوارير لكل كجم تمر", "Bottles per kg of dates"), num(it)) }
        r.stageTimings.forEach { pdf.row("  " + it.name, t("${num(it.avgDays)} يوم", "${num(it.avgDays)} days")) }

        pdf.heading(t("المخزون", "Stock"))
        pdf.row(t("قوارير جاهزة", "Bottles ready"), r.bottlesInStock.toString())
        pdf.row(t("قيمتها بسعر البيع", "Value at selling price"), money(r.stockValue))
        r.lowProducts.forEach { pdf.row("⚠ " + it.name, it.stock.toString()) }
        r.lowMaterials.forEach { pdf.row("⚠ " + it.name, "${num(it.stock)} ${it.unit}") }

        val file = File(shareDir(context), "report_${r.period.from}_${r.period.to}.pdf")
        pdf.save(file)
        shareFile(context, file, "application/pdf")
    }

    /** Writes simple A4 pages; text is laid out with Android's own shaping so Arabic joins and runs right to left. */
    private class PdfWriter(context: Context) {
        private val doc = PdfDocument()
        private val w = 595
        private val h = 842
        private val margin = 40f
        private var pageNo = 0
        private var page: PdfDocument.Page? = null
        private var y = 0f
        private val font = context.resources.getFont(R.font.readex_pro)
        private val dir = if (Lang.arabic) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR
        private val brand = Color.rgb(63, 111, 124)
        private val logo = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo)

        private fun paint(size: Float, color: Int = Color.BLACK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = font
        }

        private fun ensure(space: Float) {
            if (page == null || y + space > h - margin) {
                page?.let { doc.finishPage(it) }
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
                y = margin
            }
        }

        private fun draw(text: String, p: TextPaint, align: Layout.Alignment, width: Float = w - 2 * margin, x: Float = margin): Float {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, p, width.toInt())
                .setAlignment(align).setTextDirection(dir).build()
            val c = page!!.canvas
            c.save()
            c.translate(x, y)
            layout.draw(c)
            c.restore()
            return layout.height.toFloat()
        }

        /** Business name with the logo on the opposite side of the page. */
        fun title(s: String) {
            ensure(70f)
            val logoH = 64f
            val logoW = logoH * logo.width / logo.height
            val x = if (Lang.arabic) margin else w - margin - logoW
            page!!.canvas.drawBitmap(logo, null, android.graphics.RectF(x, y, x + logoW, y + logoH), Paint(Paint.FILTER_BITMAP_FLAG))
            y += 14
            y += draw(s, paint(22f, brand), Layout.Alignment.ALIGN_NORMAL) + 4
        }

        fun subtitle(s: String) {
            ensure(24f)
            y += draw(s, paint(12f, Color.DKGRAY), Layout.Alignment.ALIGN_NORMAL) + 24
        }

        fun heading(s: String) {
            ensure(50f)
            y += 10
            y += draw(s, paint(15f, brand), Layout.Alignment.ALIGN_NORMAL) + 2
            page!!.canvas.drawLine(margin, y, w - margin, y, Paint().apply { color = brand; strokeWidth = 0.8f })
            y += 6
        }

        fun row(label: String, value: String) {
            ensure(20f)
            val p = paint(11f)
            val hl = draw(label, p, Layout.Alignment.ALIGN_NORMAL)
            val hv = draw(value, p, Layout.Alignment.ALIGN_OPPOSITE)
            y += maxOf(hl, hv) + 3
        }

        fun save(file: File) {
            page?.let { doc.finishPage(it) }
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
        }
    }

    // ---------- Backup & restore ----------

    /** Writes the database and photos into one .zip at the place the user picked (Drive, phone, ...). */
    fun writeBackup(context: Context, target: Uri) {
        Repo.closeForRestore() // flushes everything into the main database file
        context.contentResolver.openOutputStream(target)!!.use { os ->
            ZipOutputStream(os).use { zip ->
                zip.putNextEntry(ZipEntry(Db.NAME))
                Repo.databaseFile().inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                Repo.photosDir.listFiles()?.forEach { f ->
                    zip.putNextEntry(ZipEntry("photos/${f.name}"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        Repo.reopenAfterRestore()
    }

    /** A ready-to-share backup file (e.g. send it to yourself on WhatsApp or Telegram). */
    fun shareBackup(context: Context) {
        val file = File(shareDir(context), "jawdat-backup-${LocalDate.now()}.zip")
        writeBackup(context, Uri.fromFile(file))
        shareFile(context, file, "application/zip")
    }

    /** Replaces all data with a backup. Returns false (and changes nothing) if the file isn't a backup. */
    fun restoreBackup(context: Context, source: Uri): Boolean {
        val tmp = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var hasDb = false
        context.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { e ->
                    val name = e.name.substringAfterLast('/')
                    if (e.isDirectory || name.isBlank() || name.contains("..")) return@forEach
                    val dest = when {
                        e.name == Db.NAME -> File(tmp, Db.NAME).also { hasDb = true }
                        e.name.startsWith("photos/") -> File(tmp, "photos").apply { mkdirs() }.let { File(it, name) }
                        else -> return@forEach
                    }
                    FileOutputStream(dest).use { zip.copyTo(it) }
                }
            }
        } ?: return false
        val dbFile = File(tmp, Db.NAME)
        if (!hasDb || !dbFile.readBytes().take(15).toByteArray().contentEquals("SQLite format 3".toByteArray())) return false

        Repo.closeForRestore()
        val live = Repo.databaseFile()
        File(live.path + "-wal").delete()
        File(live.path + "-shm").delete()
        dbFile.copyTo(live, overwrite = true)
        Repo.photosDir.deleteRecursively()
        File(tmp, "photos").listFiles()?.forEach { it.copyTo(File(Repo.photosDir, it.name), overwrite = true) }
        tmp.deleteRecursively()
        Repo.reopenAfterRestore()
        return true
    }
}
