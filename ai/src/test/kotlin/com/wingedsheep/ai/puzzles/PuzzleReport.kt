package com.wingedsheep.ai.puzzles

/**
 * Formatting for a puzzle run: the overall pass rate, the per-category breakdown, and the failing
 * moves.
 *
 * The per-category number is the point. A win rate says the AI got worse; "removal targeting fell
 * from 5/6 to 2/6" says where to look. This is the number quoted alongside arena win rate in
 * `docs/ai/baseline-metrics.md`.
 */
object PuzzleReport {

    fun summary(profileId: String, results: List<PuzzleResult>): String = buildString {
        val passed = results.count { it.passed }
        appendLine("=== PUZZLES: $profileId — $passed/${results.size} (${pct(passed, results.size)}) ===")
        appendLine()
        appendLine(categoryTable(results))
        val failures = results.filterNot { it.passed }
        if (failures.isNotEmpty()) {
            appendLine()
            appendLine("Failing:")
            for (result in failures) {
                appendLine("  ${result.puzzle.id}  ${result.puzzle.expectation}")
                appendLine("      ${result.failure}")
            }
        }
    }

    fun categoryTable(results: List<PuzzleResult>): String = buildString {
        appendLine("| Category | Pass | Rate | Catches |")
        appendLine("|---|---|---|---|")
        for (category in PuzzleCategory.entries) {
            val inCategory = results.filter { it.puzzle.category == category }
            if (inCategory.isEmpty()) continue
            val passed = inCategory.count { it.passed }
            appendLine(
                "| ${category.id} | $passed/${inCategory.size} | ${pct(passed, inCategory.size)} " +
                    "| ${category.catches} |"
            )
        }
        val total = results.count { it.passed }
        appendLine("| **total** | **$total/${results.size}** | **${pct(total, results.size)}** | |")
    }

    /** Side-by-side per-category pass counts, for comparing agents on the same suite. */
    fun comparison(runs: List<Pair<String, List<PuzzleResult>>>): String = buildString {
        appendLine("| Category | ${runs.joinToString(" | ") { it.first }} |")
        appendLine("|---|${runs.joinToString("|") { "---" }}|")
        for (category in PuzzleCategory.entries) {
            val cells = runs.map { (_, results) ->
                val inCategory = results.filter { it.puzzle.category == category }
                "${inCategory.count { it.passed }}/${inCategory.size}"
            }
            appendLine("| ${category.id} | ${cells.joinToString(" | ")} |")
        }
        val totals = runs.map { (_, results) -> "${results.count { it.passed }}/${results.size}" }
        appendLine("| **total** | ${totals.joinToString(" | ") { "**$it**" }} |")
    }

    private fun pct(passed: Int, total: Int): String =
        if (total == 0) "—" else "%.0f%%".format(100.0 * passed / total)
}
