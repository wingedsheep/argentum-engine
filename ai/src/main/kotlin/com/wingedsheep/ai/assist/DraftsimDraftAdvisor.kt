package com.wingedsheep.ai.assist

import com.wingedsheep.ai.draftsim.DraftsimCardScore
import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimScorer
import com.wingedsheep.ai.draftsim.toScorerCard

/**
 * Draft engine backed by the ported Draftsim scorer ([DraftsimScorer]). Loads the lobby set's
 * ratings/removal/archetype tables ([DraftsimData]) and scores the pack with the bundle's `xs`
 * dispatch (color-bias `aX` for untagged sets, archetype-aware `jm` for tagged ones). Falls back to
 * the rarity ladder for any set we have no table for, so it always returns a ranking.
 */
object DraftsimDraftAdvisor : DraftAdvisor {
    override val id = "draftsim"
    override val displayName = "Draftsim"

    override fun suggestPick(request: DraftPickAdviceRequest): DraftPickAdvice {
        val scorer = DraftsimScorer(DraftsimData.tablesFor(request.setCodes))
        val pack = request.pack.map { it.toScorerCard() }
        val picks = request.pickedSoFar.map { it.toScorerCard() }
        val scores = scorer.scoreBoosterAuto(pack, picks)

        // Display normalization: scale totals to 0–100 relative to the pack's best pick.
        val maxTotal = scores.values.maxOfOrNull { it.total }?.takeIf { it > 0 } ?: 1.0
        val ranked = request.pack.sortedWith(
            compareByDescending<com.wingedsheep.ai.llm.CardSummary> { scores[it.name]?.total ?: 0.0 }
                .thenByDescending { scores[it.name]?.rawRating ?: 0.0 },
        )

        val cardScores = ranked.map { card ->
            val s = scores[card.name]
            CardScore(
                cardName = card.name,
                score = (((s?.total ?: 0.0) / maxTotal) * 100.0).coerceIn(0.0, 100.0).toInt(),
                reason = reasonFor(scorer, s),
            )
        }
        val recommended = ranked.take(request.picksRequired.coerceAtLeast(1)).map { it.name }
        return DraftPickAdvice(advisorId = id, scores = cardScores, recommended = recommended)
    }

    private fun reasonFor(scorer: DraftsimScorer, score: DraftsimCardScore?): String {
        if (score == null) return "No rating"
        return score.summary ?: scorer.dominantReason(score) ?: "Playable"
    }
}
