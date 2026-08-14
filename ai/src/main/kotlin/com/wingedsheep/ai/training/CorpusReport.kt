package com.wingedsheep.ai.training

import kotlinx.serialization.Serializable

@Serializable
data class CorpusReport(
    val valid: Boolean,
    val errors: List<String>,
    val games: Int,
    val decisions: Int,
    val gamesByGenerator: Map<String, Int>,
    val gamesBySet: Map<String, Int>,
    val decisionsByActionFamily: Map<String, Int>,
    val decisionsByGamePhase: Map<String, Int>,
    val decisionsByPlayerCount: Map<Int, Int>,
    val candidateCountHistogram: Map<Int, Int>,
)

object CorpusReporter {
    fun report(corpus: TrainingCorpus, minimumGeneratorCount: Int = 2): CorpusReport {
        val validation = TrainingCorpusValidator.validate(corpus, minimumGeneratorCount)
        return CorpusReport(
            valid = validation.valid,
            errors = validation.errors,
            games = corpus.games.size,
            decisions = corpus.records.size,
            gamesByGenerator = corpus.games.countBy { it.generator },
            gamesBySet = corpus.games.countBy { it.setCode },
            decisionsByActionFamily = corpus.records.countBy { it.actionFamily },
            decisionsByGamePhase = corpus.records.countBy { it.gamePhase },
            decisionsByPlayerCount = corpus.records.countBy { it.playerCount },
            candidateCountHistogram = corpus.records.countBy { it.candidates.size },
        )
    }

    private fun <T, K : Comparable<K>> Iterable<T>.countBy(key: (T) -> K): Map<K, Int> =
        groupingBy(key).eachCount().toSortedMap()
}
