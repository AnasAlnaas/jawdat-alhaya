package ly.jawdat.alhaya.data

import java.time.LocalDate

data class Product(
    val id: Long = 0,
    val name: String,
    val herb: String = Herb.PLAIN,
    /** [ProductSize.SMALL] or [ProductSize.LARGE]; older products may still hold hand-typed text. */
    val size: String = ProductSize.SMALL,
    val price: Double = 0.0,
    val stock: Int = 0,
    val lowStock: Int = 0,
    val archived: Boolean = false,
)

data class Market(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val archived: Boolean = false,
)

data class Material(
    val id: Long = 0,
    val name: String,
    val unit: String = "",
    val stock: Double = 0.0,
    val lowLevel: Double = 0.0,
)

data class Batch(
    val id: Long = 0,
    val code: String,
    val name: String = "",
    val herb: String = Herb.PLAIN,
    val startDate: LocalDate,
    val status: String = BatchStatus.ACTIVE,
    val finishedDate: LocalDate? = null,
    val notes: String = "",
)

data class Stage(
    val id: Long = 0,
    val batchId: Long,
    val position: Int,
    val name: String,
    val status: String = StageStatus.PENDING,
    val startedDate: LocalDate? = null,
    val checkDate: LocalDate? = null,
    val doneDate: LocalDate? = null,
    val remind: Boolean = true,
    val notes: String = "",
)

data class StageItem(
    val id: Long = 0,
    val stageId: Long,
    val batchId: Long,
    val date: LocalDate,
    val kind: String = ItemKind.OTHER,
    val item: String,
    val qty: Double = 0.0,
    val unit: String = "",
    val cost: Double = 0.0,
    val materialId: Long? = null,
)

data class StagePhoto(val id: Long, val stageId: Long, val file: String)

data class BatchOutput(
    val id: Long = 0,
    val batchId: Long,
    val productId: Long,
    val qty: Int,
    val date: LocalDate,
)

data class Sale(
    val id: Long = 0,
    val date: LocalDate,
    val marketId: Long?,
    val productId: Long,
    val batchId: Long? = null,
    val qty: Int,
    val unitPrice: Double,
    val total: Double,
    val notes: String = "",
    /** Bottles given free: with the sale, or on their own when [qty] is 0 (a gift). */
    val giftQty: Int = 0,
    /** Bottles this sale added to stock because fewer were recorded than it took out. */
    val stockAdded: Int = 0,
) {
    val isGiftOnly: Boolean get() = qty == 0 && giftQty > 0

    /** Bottles that left stock with this sale, sold and free. */
    val bottlesOut: Int get() = qty + giftQty
}

data class Payment(
    val id: Long = 0,
    val date: LocalDate,
    val marketId: Long?,
    val saleId: Long? = null,
    val amount: Double,
    val notes: String = "",
)

data class Expense(
    val id: Long = 0,
    val date: LocalDate,
    val category: String = ExpenseCategory.OTHER,
    val description: String = "",
    val amount: Double,
)

/** A sale with how much of it is paid, worked out by applying the market's payments oldest-first. */
data class SaleView(
    val sale: Sale,
    val productName: String,
    val marketName: String?,
    val paid: Double,
) {
    val owed: Double get() = (sale.total - paid).coerceAtLeast(0.0)
    val isPaid: Boolean get() = owed < 0.005
}

data class BatchSummary(
    val batch: Batch,
    val stages: List<Stage>,
    val totalCost: Double,
    val bottles: Int,
) {
    val current: Stage? get() = stages.firstOrNull { it.status == StageStatus.ACTIVE }
        ?: stages.firstOrNull { it.status == StageStatus.PENDING }
    val doneCount: Int get() = stages.count { it.status == StageStatus.DONE }
    val costPerBottle: Double? get() = if (bottles > 0) totalCost / bottles else null
}

/** The two bottle sizes, stored by their volume in ml. */
object ProductSize {
    const val SMALL = "330"
    const val LARGE = "1000"
    val all = listOf(SMALL, LARGE)
}

object BatchStatus {
    const val ACTIVE = "active"
    const val DONE = "done"
    const val CANCELLED = "cancelled"
}

object StageStatus {
    const val PENDING = "pending"
    const val ACTIVE = "active"
    const val DONE = "done"
    const val SKIPPED = "skipped"
}

object Herb {
    const val PLAIN = "plain"
    const val THYME = "thyme"
    const val ROSEMARY = "rosemary"
    const val GINGER = "ginger"
    const val GARLIC = "garlic"
    const val FENNEL = "fennel"
    const val MIXED = "mixed"
    const val OTHER = "other"
    val all = listOf(PLAIN, THYME, ROSEMARY, GINGER, GARLIC, FENNEL, MIXED, OTHER)
}

object ItemKind {
    const val DATES = "dates"
    const val WATER = "water"
    const val HERB = "herb"
    const val CONTAINER = "container"
    const val BOTTLES = "bottles"
    const val LABELS = "labels"
    const val ENERGY = "energy"
    const val LABOR = "labor"
    const val OTHER = "other"
    val all = listOf(DATES, WATER, HERB, CONTAINER, BOTTLES, LABELS, ENERGY, LABOR, OTHER)
}

object ExpenseCategory {
    const val TRANSPORT = "transport"
    const val PACKAGING = "packaging"
    const val EQUIPMENT = "equipment"
    const val MARKETING = "marketing"
    const val UTILITIES = "utilities"
    const val OTHER = "other"
    val all = listOf(TRANSPORT, PACKAGING, EQUIPMENT, MARKETING, UTILITIES, OTHER)
}
