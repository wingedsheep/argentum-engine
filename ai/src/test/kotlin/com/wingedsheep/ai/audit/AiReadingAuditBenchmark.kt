package com.wingedsheep.ai.audit

import com.wingedsheep.ai.arena.harnessRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import java.io.File

/**
 * The full-corpus AI card-reading audit (`just ai-reading-audit`). Skipped in the normal `:ai:test`
 * suite; enabled by `-DaiReadingAudit=true`. Writes `report.md` (grouped, sorted by card count),
 * `summary.tsv` (baseline-comparable) and `cards.tsv` (each flagged card's flags) under `ai/build/reports/ai-reading-audit/`.
 *
 * See `docs/ai/reading-audit.md` for how to read the output.
 */
class AiReadingAuditBenchmark : FunSpec({

    test("AI card-reading audit over the whole corpus").config(enabled = System.getProperty("aiReadingAudit") == "true") {
        val registry = harnessRegistry(MtgSetCatalog.all)
        val cards = MtgSetCatalog.all.flatMap { it.cards }
        val report = AiReadingAudit.report(cards) { name -> registry.getCard(name) }

        val dir = File(System.getProperty("aiReadingAuditDir") ?: "build/reports/ai-reading-audit").apply { mkdirs() }
        File(dir, "report.md").writeText(report.markdown())
        File(dir, "summary.tsv").writeText(report.summaryTsv())
        File(dir, "cards.tsv").writeText(report.cardsTsv())

        println()
        print(report.headline())
        println("Full report: ${File(dir, "report.md").absolutePath}")
        println("Baseline-comparable summary: ${File(dir, "summary.tsv").absolutePath}")
    }
})
