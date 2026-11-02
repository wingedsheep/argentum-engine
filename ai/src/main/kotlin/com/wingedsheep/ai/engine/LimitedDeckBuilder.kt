package com.wingedsheep.ai.engine

import com.wingedsheep.ai.assist.DeckBuildRequest
import com.wingedsheep.ai.assist.DraftsimDeckBuildAdvisor
import com.wingedsheep.sdk.model.CardDefinition
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.wingedsheep.ai.engine.LimitedDeckBuilder")

/**
 * The engine AI's 40-card limited build from a sealed or drafted [pool]: the one every AI seat uses,
 * whether it opened its own boosters ([SealedDeckGenerator]), drafted or opened a pool in a lobby,
 * or sits in the dev LLM tournament without an LLM build.
 *
 * It is the Draftsim autobuilder ([DraftsimDeckBuildAdvisor]) — the same engine as the deckbuild
 * "Auto-build" button's default — scoped to [setCodes] so it loads those sets' ratings, removal and
 * archetype tables (a set without a ratings file still builds, rated off its card definitions). The
 * colour-and-curve [buildHeuristicSealedDeck] is only the safety net for a pool Draftsim throws on or
 * builds nothing from, because it always yields a legal 40-card deck.
 *
 * Plain card names, plain basic-land names: callers pin printings themselves.
 */
fun buildLimitedDeck(pool: List<CardDefinition>, setCodes: List<String>): Map<String, Int> {
    val result = runCatching {
        DraftsimDeckBuildAdvisor.buildDeck(DeckBuildRequest(pool = pool, setCodes = setCodes))
    }.getOrElse { error ->
        logger.warn("Draftsim build failed for sets {}; falling back to heuristic", setCodes, error)
        null
    }
    val build = result?.builds?.getOrNull(result.recommended)
    if (build != null && build.deckList.isNotEmpty()) return build.deckList

    if (result != null) {
        logger.warn("Draftsim produced no build for sets {}; falling back to heuristic", setCodes)
    }
    return buildHeuristicSealedDeck(pool)
}
