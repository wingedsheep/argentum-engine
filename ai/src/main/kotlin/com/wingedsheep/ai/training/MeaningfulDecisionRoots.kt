package com.wingedsheep.ai.training

import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.MeaningfulActionFilter

enum class RootSkipReason { FORCED, BOOKKEEPING_ONLY, SEMANTIC_DUPLICATE }

data class MeaningfulRoot(
    val candidates: List<LegalAction>,
    val skipReason: RootSkipReason? = null,
) {
    val shouldCapture: Boolean get() = skipReason == null
}

/** Auditable, content-neutral filtering for offline collection. */
object MeaningfulDecisionRoots {
    fun classify(actions: List<LegalAction>): MeaningfulRoot {
        val affordable = MeaningfulActionFilter.filterMeaningful(actions).filter { it.affordable && !it.isManaAbility }
        if (affordable.isEmpty()) return MeaningfulRoot(emptyList(), RootSkipReason.BOOKKEEPING_ONLY)
        val distinct = affordable.distinctBy { semanticKey(it.action) }
        // Combat currently arrives as one placeholder action; it is not a training root until the
        // attacker/blocker plan expander emits the actual alternatives.
        if (distinct.size == 1) {
            return MeaningfulRoot(distinct, RootSkipReason.FORCED)
        }
        return if (distinct.size < affordable.size && distinct.size < 2) {
            MeaningfulRoot(distinct, RootSkipReason.SEMANTIC_DUPLICATE)
        } else {
            MeaningfulRoot(distinct)
        }
    }

    private fun semanticKey(action: GameAction): String = when (action) {
        is PassPriority -> "pass"
        else -> TrainingRecordEncoding.action(action).actionDigest
    }
}
