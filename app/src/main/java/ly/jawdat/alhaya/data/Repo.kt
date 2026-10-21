package ly.jawdat.alhaya.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ly.jawdat.alhaya.ui.title
import java.io.File
import java.time.LocalDate

/**
 * Every read and write of the database goes through here.
 *
 * The data is small (one small business on one phone), so queries run directly and screens re-read
 * whenever [changes] ticks after a write; that keeps every screen in sync without extra machinery.
 */
object Repo {
    private lateinit var helper: Db
    private lateinit var appContext: Context
    private val db: SQLiteDatabase get() = helper.writableDatabase

    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes

    fun init(context: Context) {
        appContext = context.applicationContext
        helper = Db(appContext)
    }

    private fun changed() {
        _changes.value++
    }

    val photosDir: File get() = File(appContext.filesDir, "photos").apply { mkdirs() }

    /** Closes the database so a backup can replace it; it reopens on the next query. */
    fun closeForRestore() = helper.close()

    fun reopenAfterRestore() {
        helper = Db(appContext)
        changed()
    }

    fun databaseFile(): File = appContext.getDatabasePath(Db.NAME)

    // ---------- Products ----------

    fun products(includeArchived: Boolean = false): List<Product> =
        db.rawQuery(
            "SELECT * FROM products ${if (includeArchived) "" else "WHERE archived = 0"} ORDER BY archived, name",
            null,
        ).list { it.product() }

    fun product(id: Long): Product? =
        db.rawQuery("SELECT * FROM products WHERE id = ?", arrayOf(id.toString())).first { it.product() }

    fun saveProduct(p: Product): Long {
        val v = ContentValues().apply {
            put("name", p.name.trim())
            put("herb", p.herb)
            put("size", p.size.trim())
            put("price", p.price)
            put("stock", p.stock)
            put("low_stock", p.lowStock)
            put("archived", if (p.archived) 1 else 0)
        }
        val id = upsert("products", p.id, v)
        changed()
        return id
    }

    fun productIsUsed(id: Long): Boolean =
        count("SELECT COUNT(*) FROM sales WHERE product_id = ?", id) +
            count("SELECT COUNT(*) FROM batch_outputs WHERE product_id = ?", id) > 0

    fun deleteProduct(id: Long) {
        db.delete("products", "id = ?", arrayOf(id.toString()))
        changed()
    }

    // ---------- Markets ----------

    fun markets(includeArchived: Boolean = false): List<Market> =
        db.rawQuery(
            "SELECT * FROM markets ${if (includeArchived) "" else "WHERE archived = 0"} ORDER BY archived, name",
            null,
        ).list { it.market() }

    fun market(id: Long): Market? =
        db.rawQuery("SELECT * FROM markets WHERE id = ?", arrayOf(id.toString())).first { it.market() }

    fun saveMarket(m: Market): Long {
        val v = ContentValues().apply {
            put("name", m.name.trim())
            put("phone", m.phone.trim())
            put("address", m.address.trim())
            put("notes", m.notes.trim())
            put("archived", if (m.archived) 1 else 0)
        }
        val id = upsert("markets", m.id, v)
        changed()
        return id
    }

    fun marketIsUsed(id: Long): Boolean = count("SELECT COUNT(*) FROM sales WHERE market_id = ?", id) > 0

    fun deleteMarket(id: Long) {
        db.delete("payments", "market_id = ?", arrayOf(id.toString()))
        db.delete("markets", "id = ?", arrayOf(id.toString()))
        changed()
    }

    // ---------- Materials (raw stock) ----------

    fun materials(): List<Material> =
        db.rawQuery("SELECT * FROM materials ORDER BY name", null).list { it.material() }

    fun saveMaterial(m: Material): Long {
        val v = ContentValues().apply {
            put("name", m.name.trim())
            put("unit", m.unit.trim())
            put("stock", m.stock)
            put("low_level", m.lowLevel)
        }
        val id = upsert("materials", m.id, v)
        changed()
        return id
    }

    fun adjustMaterial(id: Long, delta: Double) {
        db.execSQL("UPDATE materials SET stock = stock + ? WHERE id = ?", arrayOf<Any>(delta, id))
        changed()
    }

    fun deleteMaterial(id: Long) {
        db.delete("materials", "id = ?", arrayOf(id.toString()))
        changed()
    }

    // ---------- Batches & stages ----------

    fun nextBatchCode(date: LocalDate): String {
        val n = count("SELECT COUNT(*) FROM batches WHERE substr(start_date, 1, 4) = ?", date.year.toString()) + 1
        return "%d-%03d".format(date.year, n)
    }

    fun createBatch(batch: Batch, stageNames: List<String>): Long {
        var id = 0L
        db.transaction {
            id = db.insert("batches", null, ContentValues().apply {
                put("code", batch.code)
                put("name", batch.name.trim())
                put("herb", batch.herb)
                put("start_date", batch.startDate.toString())
                put("status", BatchStatus.ACTIVE)
                put("notes", batch.notes)
                put("liters", batch.liters)
            })
            stageNames.filter { it.isNotBlank() }.forEachIndexed { i, name ->
                db.insert("stages", null, ContentValues().apply {
                    put("batch_id", id)
                    put("position", i)
                    put("name", name.trim())
                    put("status", if (i == 0) StageStatus.ACTIVE else StageStatus.PENDING)
                    if (i == 0) put("started_date", batch.startDate.toString())
                })
            }
        }
        changed()
        return id
    }

    fun updateBatch(b: Batch) {
        db.update("batches", ContentValues().apply {
            put("code", b.code)
            put("name", b.name.trim())
            put("herb", b.herb)
            put("start_date", b.startDate.toString())
            put("status", b.status)
            put("finished_date", b.finishedDate?.toString())
            put("notes", b.notes)
            put("liters", b.liters)
        }, "id = ?", arrayOf(b.id.toString()))
        changed()
    }

    fun batch(id: Long): Batch? =
        db.rawQuery("SELECT * FROM batches WHERE id = ?", arrayOf(id.toString())).first { it.batch() }

    fun batches(): List<Batch> =
        db.rawQuery("SELECT * FROM batches ORDER BY start_date DESC, id DESC", null).list { it.batch() }

    fun batchSummaries(): List<BatchSummary> {
        val stagesByBatch = db.rawQuery("SELECT * FROM stages ORDER BY batch_id, position", null)
            .list { it.stage() }.groupBy { it.batchId }
        val costs = db.rawQuery("SELECT batch_id, SUM(cost) FROM stage_items GROUP BY batch_id", null)
            .list { it.getLong(0) to it.getDouble(1) }.toMap()
        val bottles = db.rawQuery("SELECT batch_id, SUM(qty) FROM batch_outputs GROUP BY batch_id", null)
            .list { it.getLong(0) to it.getInt(1) }.toMap()
        val herbs = allBatchHerbs().groupBy { it.batchId }
        return batches().map { BatchSummary(it, stagesByBatch[it.id].orEmpty(), costs[it.id] ?: 0.0, bottles[it.id] ?: 0, herbs[it.id].orEmpty()) }
    }

    fun batchSummary(id: Long): BatchSummary? = batchSummaries().firstOrNull { it.batch.id == id }

    fun deleteBatch(id: Long) {
        db.transaction {
            // Give back stock the batch used or made, so deleting a mistake leaves the counts right.
            stageItemsForBatch(id).forEach { item ->
                item.materialId?.let { db.execSQL("UPDATE materials SET stock = stock + ? WHERE id = ?", arrayOf<Any>(item.qty, it)) }
            }
            outputs(id).forEach { o ->
                db.execSQL("UPDATE products SET stock = stock - ? WHERE id = ?", arrayOf<Any>(o.qty, o.productId))
            }
            photosForBatch(id).forEach { File(photosDir, it.file).delete() }
            db.delete("batches", "id = ?", arrayOf(id.toString()))
        }
        changed()
    }

    fun stages(batchId: Long): List<Stage> =
        db.rawQuery("SELECT * FROM stages WHERE batch_id = ? ORDER BY position", arrayOf(batchId.toString())).list { it.stage() }

    fun allStages(): List<Stage> = db.rawQuery("SELECT * FROM stages", null).list { it.stage() }

    fun stage(id: Long): Stage? =
        db.rawQuery("SELECT * FROM stages WHERE id = ?", arrayOf(id.toString())).first { it.stage() }

    fun saveStage(s: Stage): Long {
        val v = ContentValues().apply {
            put("batch_id", s.batchId)
            put("position", s.position)
            put("name", s.name.trim())
            put("status", s.status)
            put("started_date", s.startedDate?.toString())
            put("check_date", s.checkDate?.toString())
            put("done_date", s.doneDate?.toString())
            put("remind", if (s.remind) 1 else 0)
            put("notes", s.notes)
        }
        val id = upsert("stages", s.id, v)
        changed()
        return id
    }

    /** Marks a stage done and starts the next one that is still waiting. */
    fun completeStage(stage: Stage, date: LocalDate) {
        db.transaction {
            saveStage(stage.copy(status = StageStatus.DONE, doneDate = date, startedDate = stage.startedDate ?: date))
            stages(stage.batchId).firstOrNull { it.position > stage.position && it.status == StageStatus.PENDING }?.let {
                saveStage(it.copy(status = StageStatus.ACTIVE, startedDate = date))
            }
        }
        changed()
    }

    /**
     * Moves a batch on by one stage: [current] (if any) is done on [date], and [next] becomes the active
     * stage right after it, with an optional check date. [next] may be a new stage (id 0) typed by hand.
     */
    fun advanceStage(batchId: Long, current: Stage?, next: Stage, date: LocalDate, checkDate: LocalDate?) {
        db.transaction {
            current?.let { saveStage(it.copy(status = StageStatus.DONE, doneDate = date, startedDate = it.startedDate ?: date)) }
            // The chosen stage goes straight after the current one; the others keep their order behind it.
            val others = stages(batchId).filter { it.id != next.id || next.id == 0L }
            val at = (current?.let { c -> others.indexOfFirst { it.id == c.id } } ?: -1) + 1
            val ordered: MutableList<Stage?> = others.toMutableList<Stage?>().apply { add(at.coerceIn(0, size), null) }
            ordered.forEachIndexed { i, s ->
                when {
                    s == null -> saveStage(
                        next.copy(batchId = batchId, position = i, status = StageStatus.ACTIVE, startedDate = date, checkDate = checkDate, doneDate = null)
                    )
                    s.position != i -> saveStage(s.copy(position = i))
                }
            }
        }
        changed()
    }

    fun deleteStage(id: Long) {
        stageItems(id).forEach { deleteStageItem(it) }
        photos(id).forEach { File(photosDir, it.file).delete() }
        db.delete("stages", "id = ?", arrayOf(id.toString()))
        changed()
    }

    fun moveStage(stage: Stage, up: Boolean) {
        val list = stages(stage.batchId)
        val i = list.indexOfFirst { it.id == stage.id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) return
        db.transaction {
            saveStage(list[i].copy(position = list[j].position))
            saveStage(list[j].copy(position = list[i].position))
        }
        changed()
    }

    // ---------- Herbs added at the last stage ----------

    fun herbsForBatch(batchId: Long): List<BatchHerb> =
        db.rawQuery("SELECT * FROM batch_herbs WHERE batch_id = ? ORDER BY id", arrayOf(batchId.toString())).list { it.batchHerb() }

    fun allBatchHerbs(): List<BatchHerb> = db.rawQuery("SELECT * FROM batch_herbs ORDER BY id", null).list { it.batchHerb() }

    fun saveBatchHerb(h: BatchHerb) {
        upsert("batch_herbs", h.id, ContentValues().apply {
            put("batch_id", h.batchId)
            put("herb", h.herb)
            put("liters", h.liters)
            put("date", h.date.toString())
            put("notes", h.notes.trim())
        })
        changed()
    }

    fun deleteBatchHerb(id: Long) {
        db.delete("batch_herbs", "id = ?", arrayOf(id.toString()))
        changed()
    }

    // ---------- Stage items (quantities and costs) ----------

    fun stageItems(stageId: Long): List<StageItem> =
        db.rawQuery("SELECT * FROM stage_items WHERE stage_id = ? ORDER BY id", arrayOf(stageId.toString())).list { it.stageItem() }

    fun stageItemsForBatch(batchId: Long): List<StageItem> =
        db.rawQuery("SELECT * FROM stage_items WHERE batch_id = ? ORDER BY id", arrayOf(batchId.toString())).list { it.stageItem() }

    fun allStageItems(): List<StageItem> = db.rawQuery("SELECT * FROM stage_items", null).list { it.stageItem() }

    /** Saves an item; when it is taken from a stock material, the material's stock goes down by its quantity. */
    fun saveStageItem(item: StageItem) {
        db.transaction {
            if (item.id != 0L) {
                db.rawQuery("SELECT * FROM stage_items WHERE id = ?", arrayOf(item.id.toString())).first { it.stageItem() }?.let { old ->
                    old.materialId?.let { db.execSQL("UPDATE materials SET stock = stock + ? WHERE id = ?", arrayOf<Any>(old.qty, it)) }
                }
            }
            upsert("stage_items", item.id, ContentValues().apply {
                put("stage_id", item.stageId)
                put("batch_id", item.batchId)
                put("date", item.date.toString())
                put("kind", item.kind)
                put("item", item.item.trim())
                put("qty", item.qty)
                put("unit", item.unit.trim())
                put("cost", item.cost)
                if (item.materialId != null) put("material_id", item.materialId) else putNull("material_id")
            })
            item.materialId?.let { db.execSQL("UPDATE materials SET stock = stock - ? WHERE id = ?", arrayOf<Any>(item.qty, it)) }
        }
        changed()
    }

    fun deleteStageItem(item: StageItem) {
        db.transaction {
            item.materialId?.let { db.execSQL("UPDATE materials SET stock = stock + ? WHERE id = ?", arrayOf<Any>(item.qty, it)) }
            db.delete("stage_items", "id = ?", arrayOf(item.id.toString()))
        }
        changed()
    }

    /** Item names typed before, so the next batch can pick them instead of typing again. */
    fun knownItemNames(): List<String> =
        db.rawQuery("SELECT item FROM stage_items GROUP BY item ORDER BY COUNT(*) DESC LIMIT 30", null).list { it.getString(0) }

    // ---------- Photos ----------

    fun photos(stageId: Long): List<StagePhoto> =
        db.rawQuery("SELECT * FROM stage_photos WHERE stage_id = ? ORDER BY id", arrayOf(stageId.toString()))
            .list { StagePhoto(it.long("id"), it.long("stage_id"), it.str("file")) }

    private fun photosForBatch(batchId: Long): List<StagePhoto> =
        db.rawQuery(
            "SELECT p.* FROM stage_photos p JOIN stages s ON s.id = p.stage_id WHERE s.batch_id = ?",
            arrayOf(batchId.toString()),
        ).list { StagePhoto(it.long("id"), it.long("stage_id"), it.str("file")) }

    fun addPhoto(stageId: Long, file: String) {
        db.insert("stage_photos", null, ContentValues().apply {
            put("stage_id", stageId)
            put("file", file)
        })
        changed()
    }

    fun deletePhoto(photo: StagePhoto) {
        File(photosDir, photo.file).delete()
        db.delete("stage_photos", "id = ?", arrayOf(photo.id.toString()))
        changed()
    }

    // ---------- Batch outputs (bottles made) ----------

    fun outputs(batchId: Long): List<BatchOutput> =
        db.rawQuery("SELECT * FROM batch_outputs WHERE batch_id = ? ORDER BY id", arrayOf(batchId.toString())).list { it.output() }

    fun allOutputs(): List<BatchOutput> = db.rawQuery("SELECT * FROM batch_outputs", null).list { it.output() }

    fun addOutput(o: BatchOutput) {
        db.transaction {
            db.insert("batch_outputs", null, ContentValues().apply {
                put("batch_id", o.batchId)
                put("product_id", o.productId)
                put("qty", o.qty)
                put("date", o.date.toString())
            })
            db.execSQL("UPDATE products SET stock = stock + ? WHERE id = ?", arrayOf<Any>(o.qty, o.productId))
        }
        changed()
    }

    fun deleteOutput(o: BatchOutput) {
        db.transaction {
            db.delete("batch_outputs", "id = ?", arrayOf(o.id.toString()))
            db.execSQL("UPDATE products SET stock = stock - ? WHERE id = ?", arrayOf<Any>(o.qty, o.productId))
        }
        changed()
    }

    fun adjustProductStock(id: Long, delta: Int) {
        db.execSQL("UPDATE products SET stock = stock + ? WHERE id = ?", arrayOf<Any>(delta, id))
        changed()
    }

    // ---------- Sales & payments ----------

    fun sales(from: LocalDate? = null, to: LocalDate? = null): List<Sale> {
        val where = if (from != null && to != null) "WHERE date BETWEEN ? AND ?" else ""
        val args = if (from != null && to != null) arrayOf(from.toString(), to.toString()) else null
        return db.rawQuery("SELECT * FROM sales $where ORDER BY date DESC, id DESC", args).list { it.sale() }
    }

    fun sale(id: Long): Sale? =
        db.rawQuery("SELECT * FROM sales WHERE id = ?", arrayOf(id.toString())).first { it.sale() }

    fun paymentForSale(saleId: Long): Double =
        db.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE sale_id = ?", arrayOf(saleId.toString()))
            .first { it.getDouble(0) } ?: 0.0

    /**
     * Saves a sale and the money paid with it, and keeps the product's stock right.
     * [paidNow] replaces whatever was paid with this sale before (when editing).
     *
     * Selling more bottles than the recorded stock is allowed: the missing bottles are added to stock
     * in the same step (and remembered in stock_added), so stock never goes below zero and deleting
     * the sale puts stock back exactly as it was.
     */
    fun saveSale(s: Sale, paidNow: Double): Long {
        var id = s.id
        db.transaction {
            if (s.id != 0L) {
                sale(s.id)?.let { old -> db.execSQL("UPDATE products SET stock = stock + ? WHERE id = ?", arrayOf<Any>(old.bottlesOut - old.stockAdded, old.productId)) }
                db.delete("payments", "sale_id = ?", arrayOf(s.id.toString()))
            }
            val inStock = product(s.productId)?.stock ?: 0
            val added = (s.bottlesOut - inStock).coerceAtLeast(0)
            id = upsert("sales", s.id, ContentValues().apply {
                put("date", s.date.toString())
                if (s.marketId != null) put("market_id", s.marketId) else putNull("market_id")
                put("product_id", s.productId)
                if (s.batchId != null) put("batch_id", s.batchId) else putNull("batch_id")
                put("qty", s.qty)
                put("unit_price", s.unitPrice)
                put("total", s.total)
                put("notes", s.notes)
                put("gift_qty", s.giftQty)
                put("stock_added", added)
            })
            db.execSQL("UPDATE products SET stock = stock + ? WHERE id = ?", arrayOf<Any>(added - s.bottlesOut, s.productId))
            if (paidNow > 0) {
                db.insert("payments", null, ContentValues().apply {
                    put("date", s.date.toString())
                    if (s.marketId != null) put("market_id", s.marketId) else putNull("market_id")
                    put("sale_id", id)
                    put("amount", paidNow)
                })
            }
        }
        changed()
        return id
    }

    fun deleteSale(s: Sale) {
        val saved = sale(s.id) ?: s
        db.transaction {
            db.execSQL("UPDATE products SET stock = stock + ? WHERE id = ?", arrayOf<Any>(saved.bottlesOut - saved.stockAdded, saved.productId))
            db.delete("payments", "sale_id = ?", arrayOf(saved.id.toString()))
            db.delete("sales", "id = ?", arrayOf(saved.id.toString()))
        }
        changed()
    }

    fun payments(from: LocalDate? = null, to: LocalDate? = null): List<Payment> {
        val where = if (from != null && to != null) "WHERE date BETWEEN ? AND ?" else ""
        val args = if (from != null && to != null) arrayOf(from.toString(), to.toString()) else null
        return db.rawQuery("SELECT * FROM payments $where ORDER BY date DESC, id DESC", args).list { it.payment() }
    }

    fun addPayment(p: Payment) {
        db.insert("payments", null, ContentValues().apply {
            put("date", p.date.toString())
            if (p.marketId != null) put("market_id", p.marketId) else putNull("market_id")
            put("amount", p.amount)
            put("notes", p.notes)
        })
        changed()
    }

    fun deletePayment(p: Payment) {
        db.delete("payments", "id = ?", arrayOf(p.id.toString()))
        changed()
    }

    /**
     * All sales with how much of each is paid. Each market's payments are applied to its sales
     * oldest first, so "she paid 200 of what she owes" clears the oldest debts first.
     */
    fun saleViews(): List<SaleView> {
        val products = products(true).associateBy { it.id }
        val markets = markets(true).associateBy { it.id }
        val allSales = db.rawQuery("SELECT * FROM sales ORDER BY date, id", null).list { it.sale() }
        val paidTotals = db.rawQuery("SELECT market_id, SUM(amount) FROM payments GROUP BY market_id", null)
            .list { (if (it.isNull(0)) null else it.getLong(0)) to it.getDouble(1) }.toMap()
        val views = mutableListOf<SaleView>()
        allSales.groupBy { it.marketId }.forEach { (marketId, list) ->
            var pool = paidTotals[marketId] ?: 0.0
            list.forEach { s ->
                val paid = minOf(pool, s.total).coerceAtLeast(0.0)
                pool -= paid
                views += SaleView(s, products[s.productId]?.title ?: "?", marketId?.let { markets[it]?.name }, paid)
            }
        }
        return views.sortedWith(compareByDescending<SaleView> { it.sale.date }.thenByDescending { it.sale.id })
    }

    /** What each market owes now (sales minus payments), only markets that owe something. */
    fun balances(): Map<Long?, Double> {
        val sold = db.rawQuery("SELECT market_id, SUM(total) FROM sales GROUP BY market_id", null)
            .list { (if (it.isNull(0)) null else it.getLong(0)) to it.getDouble(1) }.toMap()
        val paid = db.rawQuery("SELECT market_id, SUM(amount) FROM payments GROUP BY market_id", null)
            .list { (if (it.isNull(0)) null else it.getLong(0)) to it.getDouble(1) }.toMap()
        return sold.mapValues { (k, v) -> v - (paid[k] ?: 0.0) }.filterValues { it > 0.005 }
    }

    // ---------- Expenses ----------

    fun expenses(from: LocalDate? = null, to: LocalDate? = null): List<Expense> {
        val where = if (from != null && to != null) "WHERE date BETWEEN ? AND ?" else ""
        val args = if (from != null && to != null) arrayOf(from.toString(), to.toString()) else null
        return db.rawQuery("SELECT * FROM expenses $where ORDER BY date DESC, id DESC", args).list { it.expense() }
    }

    fun saveExpense(e: Expense) {
        upsert("expenses", e.id, ContentValues().apply {
            put("date", e.date.toString())
            put("category", e.category)
            put("description", e.description.trim())
            put("amount", e.amount)
        })
        changed()
    }

    fun deleteExpense(id: Long) {
        db.delete("expenses", "id = ?", arrayOf(id.toString()))
        changed()
    }

    // ---------- Helpers ----------

    private fun upsert(table: String, id: Long, v: ContentValues): Long =
        if (id == 0L) db.insert(table, null, v)
        else {
            db.update(table, v, "id = ?", arrayOf(id.toString()))
            id
        }

    private fun count(sql: String, vararg args: Any): Int =
        db.rawQuery(sql, args.map { it.toString() }.toTypedArray()).first { it.getInt(0) } ?: 0

    private inline fun SQLiteDatabase.transaction(block: () -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }
}

// ---------- Cursor mapping ----------

private inline fun <T> Cursor.list(map: (Cursor) -> T): List<T> = use {
    val out = ArrayList<T>(count)
    while (moveToNext()) out += map(this)
    out
}

private inline fun <T> Cursor.first(map: (Cursor) -> T): T? = use { if (moveToFirst()) map(this) else null }

private fun Cursor.str(col: String): String = getString(getColumnIndexOrThrow(col)) ?: ""
private fun Cursor.strOrNull(col: String): String? = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getString(it) }
private fun Cursor.long(col: String): Long = getLong(getColumnIndexOrThrow(col))
private fun Cursor.longOrNull(col: String): Long? = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getLong(it) }
private fun Cursor.int(col: String): Int = getInt(getColumnIndexOrThrow(col))
private fun Cursor.dbl(col: String): Double = getDouble(getColumnIndexOrThrow(col))
private fun Cursor.date(col: String): LocalDate = LocalDate.parse(str(col))
private fun Cursor.dateOrNull(col: String): LocalDate? = strOrNull(col)?.let { LocalDate.parse(it) }

private fun Cursor.product() = Product(
    long("id"), str("name"), str("herb"), str("size"), dbl("price"), int("stock"), int("low_stock"), int("archived") == 1,
)

private fun Cursor.market() = Market(long("id"), str("name"), str("phone"), str("address"), str("notes"), int("archived") == 1)

private fun Cursor.material() = Material(long("id"), str("name"), str("unit"), dbl("stock"), dbl("low_level"))

private fun Cursor.batch() = Batch(
    long("id"), str("code"), str("name"), str("herb"), date("start_date"), str("status"), dateOrNull("finished_date"), str("notes"), dbl("liters"),
)

private fun Cursor.batchHerb() = BatchHerb(long("id"), long("batch_id"), str("herb"), dbl("liters"), date("date"), str("notes"))

private fun Cursor.stage() = Stage(
    long("id"), long("batch_id"), int("position"), str("name"), str("status"),
    dateOrNull("started_date"), dateOrNull("check_date"), dateOrNull("done_date"), int("remind") == 1, str("notes"),
)

private fun Cursor.stageItem() = StageItem(
    long("id"), long("stage_id"), long("batch_id"), date("date"), str("kind"), str("item"),
    dbl("qty"), str("unit"), dbl("cost"), longOrNull("material_id"),
)

private fun Cursor.output() = BatchOutput(long("id"), long("batch_id"), long("product_id"), int("qty"), date("date"))

private fun Cursor.sale() = Sale(
    long("id"), date("date"), longOrNull("market_id"), long("product_id"), longOrNull("batch_id"),
    int("qty"), dbl("unit_price"), dbl("total"), str("notes"), int("gift_qty"), int("stock_added"),
)

private fun Cursor.payment() = Payment(long("id"), date("date"), longOrNull("market_id"), longOrNull("sale_id"), dbl("amount"), str("notes"))

private fun Cursor.expense() = Expense(long("id"), date("date"), str("category"), str("description"), dbl("amount"))
