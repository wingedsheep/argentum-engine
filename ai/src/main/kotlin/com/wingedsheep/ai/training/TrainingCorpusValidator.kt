package com.wingedsheep.ai.training

data class TrainingCorpus(val games: List<TrainingGameMetadata>, val records: List<DecisionTrainingRecord>)
data class CorpusValidation(val valid: Boolean, val errors: List<String>)

object TrainingCorpusValidator {
    fun validate(corpus: TrainingCorpus, minimumGeneratorCount: Int = 2): CorpusValidation {
        val errors = mutableListOf<String>()
        val ids = corpus.games.map { it.globallyUniqueId }
        if (ids.distinct().size != ids.size) errors += "duplicate runId/gameId"
        val schemas = (corpus.games.map { it.schemaVersion } + corpus.records.map { it.schemaVersion }).toSet()
        if (schemas.size > 1) errors += "mixed schema versions: $schemas"
        corpus.games.filterNot { it.completedCleanly }.forEach { errors += "invalid game ${it.globallyUniqueId}" }
        corpus.games.forEach { game ->
            if (game.deckHashes.size != game.profilesBySeat.size) errors += "seat metadata differs: ${game.globallyUniqueId}"
            if (game.actionPropensities.any { !it.isFinite() || it <= 0.0 || it > 1.0 }) {
                errors += "invalid action propensity: ${game.globallyUniqueId}"
            }
        }
        val gameIds = ids.toSet()
        corpus.records.filter { "${it.identity.runId}/${it.identity.gameId}" !in gameIds }
            .forEach { errors += "record has no game metadata: ${it.identity}" }
        val decisionIds = corpus.records.map { it.identity }
        if (decisionIds.distinct().size != decisionIds.size) errors += "duplicate decision identity"
        if (corpus.games.map { it.generator }.toSet().size < minimumGeneratorCount) errors += "missing generator diversity"
        corpus.records.forEach { record ->
            if (record.candidates.size < 2) errors += "non-meaningful decision at ${record.identity}"
            if (record.candidates.any { it.descriptor.encodedAction.isBlank() }) {
                errors += "candidate without structured action at ${record.identity}"
            }
            finite(record.eventualResult, "eventual result", record.identity, errors)
            record.utilityBySeat.forEach { finite(it, "utility", record.identity, errors) }
            record.candidates.forEach { candidate ->
                candidate.featureDelta.values.forEach { finite(it, "feature delta", record.identity, errors) }
                finite(candidate.staticScore, "static score", record.identity, errors)
                finite(candidate.rolloutMean, "rollout mean", record.identity, errors)
                finite(candidate.rolloutVariance, "rollout variance", record.identity, errors)
                finite(candidate.terminalResult, "terminal result", record.identity, errors)
            }
        }
        return CorpusValidation(errors.isEmpty(), errors)
    }

    private fun finite(value: Double?, label: String, id: DecisionIdentity, errors: MutableList<String>) {
        if (value != null && !value.isFinite()) errors += "non-finite $label at $id"
    }
}
