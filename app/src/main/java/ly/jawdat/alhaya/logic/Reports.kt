package ly.jawdat.alhaya.logic

import ly.jawdat.alhaya.data.BatchStatus
import ly.jawdat.alhaya.data.BatchSummary
import ly.jawdat.alhaya.data.ItemKind
import ly.jawdat.alhaya.data.Market
import ly.jawdat.alhaya.data.Product
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.data.SaleView
import ly.jawdat.alhaya.data.StageStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class Period(val from: LocalDate, val to: LocalDate) {
    val days: Long get() = ChronoUnit.DAYS.between(from, to) + 1

    /** The same number of days just before this period, for "compared with last time". */
    fun previous(): Period = Period(from.minusDays(days), from.minusDays(1))

    operator fun contains(d: LocalDate): Boolean = !d.isBefore(from) && !d.isAfter(to)

    companion object {
        /** Weeks start on Saturday, as in Libya. */
        fun thisWeek(today: LocalDate = LocalDate.now()) =
            Period(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)), today)

        fun lastWeek(today: LocalDate = LocalDate.now()): Period {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)).minusWeeks(1)
            return Period(start, start.plusDays(6))
        }

        fun thisMonth(today: LocalDate = LocalDate.now()) = Period(today.withDayOfMonth(1), today)

        fun lastMonth(today: LocalDate = LocalDate.now()): Period {
            val start = today.withDayOfMonth(1).minusMonths(1)
            return Period(start, start.with(TemporalAdjusters.lastDayOfMonth()))
        }

        fun thisYear(today: LocalDate = LocalDate.now()) = Period(today.withDayOfYear(1), today)
    }
}

data class Row(val label: String, val value: Double, val units: Int = 0, val extra: Double = 0.0)

data class StageTiming(val name: String, val avgDays: Double, val count: Int)

data class Report(
    val period: Period,
    // Sales
    val revenue: Double,
    val units: Int,
    val salesCount: Int,
    val avgUnitPrice: Double,
    val avgSale: Double,
    val cashReceived: Double,
    val unpaidFromPeriod: Double,
    // Costs & profit
    val productionCost: Double,
    val expenses: Double,
    val expensesByCategory: List<Row>,
    val productionByKind: List<Row>,
    val costOfGoodsSold: Double,
    val grossProfit: Double,
    val netProfit: Double,
    val cashFlow: Double,
    // Comparison
    val prevRevenue: Double,
    val prevUnits: Int,
    val prevNetProfit: Double,
    // Breakdowns
    val byProduct: List<Row>,
    val byMarket: List<Row>,
    val byHerb: List<Row>,
    val byWeekday: List<Pair<DayOfWeek, Double>>,
    val daily: List<Pair<LocalDate, Double>>,
    val slowProducts: List<Product>,
    val quietMarkets: List<Pair<Market, LocalDate?>>,
    // Money owed (now, not just this period)
    val owedTotal: Double,
    val owedByMarket: List<Row>,
    val oldestUnpaid: List<SaleView>,
    // Production
    val batchesStarted: Int,
    val batchesFinished: Int,
    val batchesActive: Int,
    val bottlesMade: Int,
    val avgBatchDays: Double?,
    val stageTimings: List<StageTiming>,
    val yieldPerKg: Double?,
    val costPerBottle: Map<Long, Double>,
    // Stock
    val bottlesInStock: Int,
    val stockValue: Double,
    val lowProducts: List<Product>,
    val lowMaterials: List<ly.jawdat.alhaya.data.Material>,
) {
    fun change(now: Double, before: Double): Double? = if (before > 0.005) (now - before) / before * 100 else null
}

object Reports {

    private val kgUnits = setOf("كجم", "كغ", "كيلو", "kg", "kgs", "كيلوجرام")

    /** Average cost of one bottle of each product, from all batches that made it (cost split by bottle count). */
    fun costPerBottle(summaries: List<BatchSummary>): Map<Long, Double> {
        val outputs = Repo.allOutputs().groupBy { it.batchId }
        val cost = HashMap<Long, Double>()
        val bottles = HashMap<Long, Int>()
        summaries.forEach { s ->
            val outs = outputs[s.batch.id].orEmpty()
            val total = outs.sumOf { it.qty }
            if (total == 0) return@forEach
            outs.forEach { o ->
                cost[o.productId] = (cost[o.productId] ?: 0.0) + s.totalCost * o.qty / total
                bottles[o.productId] = (bottles[o.productId] ?: 0) + o.qty
            }
        }
        return cost.mapValues { (id, c) -> c / (bottles[id] ?: 1) }
    }

    fun build(period: Period): Report {
        val products = Repo.products(true)
        val productById = products.associateBy { it.id }
        val markets = Repo.markets(true)
        val marketById = markets.associateBy { it.id }
        val views = Repo.saleViews()
        val inPeriod = views.filter { it.sale.date in period }
        val prev = period.previous()
        val inPrev = views.filter { it.sale.date in prev }
        val summaries = Repo.batchSummaries()
        val cpb = costPerBottle(summaries)
        val items = Repo.allStageItems()
        val expensesList = Repo.expenses(period.from, period.to)
        val prevExpenses = Repo.expenses(prev.from, prev.to).sumOf { it.amount }

        val revenue = inPeriod.sumOf { it.sale.total }
        val units = inPeriod.sumOf { it.sale.qty }
        val productionItems = items.filter { it.date in period }
        val productionCost = productionItems.sumOf { it.cost }
        val expenses = expensesList.sumOf { it.amount }
        val cogs = inPeriod.sumOf { v -> v.sale.qty * (cpb[v.sale.productId] ?: 0.0) }
        val cash = Repo.payments(period.from, period.to).sumOf { it.amount }

        fun netOf(sales: List<SaleView>, p: Period, exp: Double): Double =
            sales.sumOf { it.sale.total } - items.filter { it.date in p }.sumOf { it.cost } - exp

        val byProduct = inPeriod.groupBy { it.sale.productId }.map { (id, list) ->
            val rev = list.sumOf { it.sale.total }
            val u = list.sumOf { it.sale.qty }
            Row(productById[id]?.name ?: "?", rev, u, rev - u * (cpb[id] ?: 0.0))
        }.sortedByDescending { it.value }

        val byMarket = inPeriod.groupBy { it.sale.marketId }.map { (id, list) ->
            Row(id?.let { marketById[it]?.name } ?: "—", list.sumOf { it.sale.total }, list.sumOf { it.sale.qty })
        }.sortedByDescending { it.value }

        val byHerb = inPeriod.groupBy { productById[it.sale.productId]?.herb ?: "other" }.map { (herb, list) ->
            Row(herb, list.sumOf { it.sale.total }, list.sumOf { it.sale.qty })
        }.sortedByDescending { it.value }

        val byWeekday = DayOfWeek.entries.map { d -> d to inPeriod.filter { it.sale.date.dayOfWeek == d }.sumOf { it.sale.total } }
            .let { list -> list.drop(5) + list.take(5) } // Saturday first

        val daily = generateSequence(period.from) { it.plusDays(1) }.takeWhile { !it.isAfter(period.to) }
            .map { d -> d to inPeriod.filter { it.sale.date == d }.sumOf { it.sale.total } }.toList()

        val soldIds = inPeriod.map { it.sale.productId }.toSet()
        val slow = products.filter { !it.archived && it.id !in soldIds }

        val lastSale = views.groupBy { it.sale.marketId }.mapValues { (_, l) -> l.maxOf { it.sale.date } }
        val quiet = markets.filter { !it.archived }.map { it to lastSale[it.id] }
            .filter { (_, last) -> last == null || ChronoUnit.DAYS.between(last, LocalDate.now()) > 30 }

        val unpaid = views.filter { !it.isPaid }
        val owedByMarket = unpaid.groupBy { it.sale.marketId }.map { (id, l) ->
            Row(id?.let { marketById[it]?.name } ?: "—", l.sumOf { it.owed }, l.size)
        }.sortedByDescending { it.value }

        // Production
        val finished = summaries.filter { it.batch.status == BatchStatus.DONE && it.batch.finishedDate != null }
        val avgBatchDays = finished.map { ChronoUnit.DAYS.between(it.batch.startDate, it.batch.finishedDate).toDouble() }
            .takeIf { it.isNotEmpty() }?.average()
        val timings = Repo.allStages()
            .filter { it.status == StageStatus.DONE && it.startedDate != null && it.doneDate != null }
            .groupBy { it.name }
            .map { (name, l) -> StageTiming(name, l.map { ChronoUnit.DAYS.between(it.startedDate, it.doneDate).toDouble() }.average(), l.size) }
        val stageOrder = Repo.allStages().groupBy { it.name }.mapValues { (_, l) -> l.map { it.position }.average() }
        val yields = summaries.filter { it.bottles > 0 }.mapNotNull { s ->
            val kg = items.filter { it.batchId == s.batch.id && it.kind == ItemKind.DATES && it.unit.lowercase() in kgUnits }.sumOf { it.qty }
            if (kg > 0) s.bottles / kg else null
        }
        val outputsInPeriod = Repo.allOutputs().filter { it.date in period }

        return Report(
            period = period,
            revenue = revenue,
            units = units,
            salesCount = inPeriod.size,
            avgUnitPrice = if (units > 0) revenue / units else 0.0,
            avgSale = if (inPeriod.isNotEmpty()) revenue / inPeriod.size else 0.0,
            cashReceived = cash,
            unpaidFromPeriod = inPeriod.sumOf { it.owed },
            productionCost = productionCost,
            expenses = expenses,
            expensesByCategory = expensesList.groupBy { it.category }.map { (c, l) -> Row(c, l.sumOf { it.amount }) }.sortedByDescending { it.value },
            productionByKind = productionItems.groupBy { it.kind }.map { (k, l) -> Row(k, l.sumOf { it.cost }) }.sortedByDescending { it.value },
            costOfGoodsSold = cogs,
            grossProfit = revenue - cogs,
            netProfit = revenue - productionCost - expenses,
            cashFlow = cash - productionCost - expenses,
            prevRevenue = inPrev.sumOf { it.sale.total },
            prevUnits = inPrev.sumOf { it.sale.qty },
            prevNetProfit = netOf(inPrev, prev, prevExpenses),
            byProduct = byProduct,
            byMarket = byMarket,
            byHerb = byHerb,
            byWeekday = byWeekday,
            daily = daily,
            slowProducts = slow,
            quietMarkets = quiet,
            owedTotal = unpaid.sumOf { it.owed },
            owedByMarket = owedByMarket,
            oldestUnpaid = unpaid.sortedBy { it.sale.date }.take(5),
            batchesStarted = summaries.count { it.batch.startDate in period },
            batchesFinished = summaries.count { s -> s.batch.finishedDate?.let { it in period } == true },
            batchesActive = summaries.count { it.batch.status == BatchStatus.ACTIVE },
            bottlesMade = outputsInPeriod.sumOf { it.qty },
            avgBatchDays = avgBatchDays,
            stageTimings = timings.sortedBy { stageOrder[it.name] ?: 0.0 },
            yieldPerKg = yields.takeIf { it.isNotEmpty() }?.average(),
            costPerBottle = cpb,
            bottlesInStock = products.filter { !it.archived }.sumOf { it.stock },
            stockValue = products.filter { !it.archived }.sumOf { it.stock * it.price },
            lowProducts = products.filter { !it.archived && it.stock <= it.lowStock },
            lowMaterials = Repo.materials().filter { it.stock <= it.lowLevel },
        )
    }
}
