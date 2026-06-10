package com.wingedsheep.ai.assist

import com.wingedsheep.ai.engine.LimitedCardRater
import com.wingedsheep.ai.engine.buildHeuristicSealedDeck
import com.wingedsheep.sdk.model.CardDefinition

/**
 * Default deckbuild engine. Wraps [buildHeuristicSealedDeck]: picks the two strongest colors, takes
 * the best on-color spells with curve awareness, and lays a basic-land mana base. Honors
 * [DeckBuildRequest.locked] — an empty deck builds fresh, a partial deck is completed without
 * dropping the player's existing picks.
 */
object HeuristicDeckBuildAdvisor : DeckBuildAdvisor {
    override val id = "heuristic"
    override val displayName = "Heuristic"

    override fun buildDeck(request: DeckBuildRequest): DeckBuildResult {
        val byName: Map<String, CardDefinition> =
            (request.pool + request.availableBasics).associateBy { it.name }

        val deckList = buildHeuristicSealedDeck(
            pool = request.pool,
            locked = request.locked,
            targetSize = request.targetSize,
            resolveCard = { name -> byName[name] },
        )

        // Intrinsic score: sum of card ratings over the non-land cards in the built deck.
        val score = deckList.entries.sumOf { (name, count) ->
            val def = byName[name]
            if (def == null || def.typeLine.isLand) 0.0 else LimitedCardRater.rate(def) * count
        }

        // The heuristic explores no archetype alternatives, so it returns a single candidate build.
        return DeckBuildResult(advisorId = id, builds = listOf(DeckBuildOption(deckList = deckList, score = score)))
    }
}
