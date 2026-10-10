package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feature test for the **shadow** keyword counter (CR 122.1b lists shadow among the keywords a
 * keyword counter can be). Granted through `StateProjector.KEYWORD_COUNTER_MAP` like the other
 * keyword counters, so the evasion check in `BlockEvasionRules` (CR 702.28b) sees it with no
 * static ability on the creature.
 *
 * Backs Minas Morgul, Dark Fortress [LTC 514].
 */
class ShadowCounterScenarioTest : ScenarioTestBase() {

    private val markShadow = card("Mark Shadow") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Put a shadow counter on target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.SHADOW, 1, t)
        }
    }

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInHand(1, "Mark Shadow")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(markShadow)

        test("a shadow counter grants the Shadow keyword via projection") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            withClue("no shadow before the counter") {
                game.state.projectedState.hasKeyword(giant, Keyword.SHADOW) shouldBe false
            }

            game.castSpell(1, "Mark Shadow", giant).error shouldBe null
            game.resolveStack()

            withClue("a shadow counter projects the Shadow keyword") {
                game.state.projectedState.hasKeyword(giant, Keyword.SHADOW) shouldBe true
            }
        }

        test("a creature with a shadow counter can't be blocked by a creature without shadow") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Mark Shadow", giant).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("shadow (CR 702.28b): only creatures with shadow can block it") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
            }
        }

        test("a creature with a shadow counter can't block a creature without shadow") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Mark Shadow", bears).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("shadow (CR 702.28b): a shadow creature blocks only creatures with shadow") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
            }
        }
    }
}
