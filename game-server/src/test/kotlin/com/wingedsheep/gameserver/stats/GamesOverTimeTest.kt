package com.wingedsheep.gameserver.stats

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class GamesOverTimeTest : FunSpec({
    // A Thursday.
    val today = LocalDate.parse("2026-11-05")

    test("a short period is daily and gap-filled through today") {
        val chart = GamesOverTime.of(listOf(DailyCount("2026-11-02", 3, 2, 1)), days = 7, today = today)

        chart.bucket shouldBe AiBucket.DAY
        chart.points shouldHaveSize 8
        chart.points.first().day shouldBe "2026-10-29"
        chart.points.last().day shouldBe "2026-11-05"
        chart.points.single { it.count > 0 } shouldBe DailyCount("2026-11-02", 3, 2, 1)
    }

    test("a year is weekly, with days summed into their Monday") {
        val chart = GamesOverTime.of(
            listOf(DailyCount("2026-11-02", 3, 2, 1), DailyCount("2026-11-04", 1, 0, 1)),
            days = 365,
            today = today,
        )

        chart.bucket shouldBe AiBucket.WEEK
        chart.points.last() shouldBe DailyCount("2026-11-02", 4, 2, 2)
        chart.points.sumOf { it.count } shouldBe 4
    }

    test("all time starts at the first game and goes monthly past two years") {
        val chart = GamesOverTime.of(
            listOf(DailyCount("2024-01-15", 2, 2, 0), DailyCount("2026-11-01", 5, 1, 4)),
            days = null,
            today = today,
        )

        chart.bucket shouldBe AiBucket.MONTH
        chart.points.first() shouldBe DailyCount("2024-01-01", 2, 2, 0)
        chart.points.last() shouldBe DailyCount("2026-11-01", 5, 1, 4)
        chart.points shouldHaveSize 35
    }

    test("all time with no games is a single empty bucket") {
        GamesOverTime.of(emptyList(), days = null, today = today).points shouldBe listOf(DailyCount("2026-11-05", 0))
    }
})
