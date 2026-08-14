package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Topplegeist (DDQ #21) — ETB tap, plus a Delirium-gated tap at the beginning of each opponent's
 * upkeep. The delirium clause is an intervening-if `triggerCondition`, so it is checked when the
 * trigger would go on the stack.
 */
class TopplegeistScenarioTest : ScenarioTestBase() {
    init {
        test("ETB taps target creature an opponent controls") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Topplegeist")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Topplegeist").error shouldBe null
            game.resolveStack()
            if (game.state.pendingDecision is ChooseTargetsDecision) {
                game.selectTargets(listOf(bears))
            }
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            game.findPermanent("Topplegeist") shouldNotBe null
        }

        test("delirium taps a creature at the beginning of the opponent's upkeep") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Topplegeist")
                .withCardOnBattlefield(2, "Grizzly Bears")
                // Four card types in P1's graveyard: creature, instant, sorcery, land.
                .withCardInGraveyard(1, "Glory Seeker")
                .withCardInGraveyard(1, "Shock")
                .withCardInGraveyard(1, "Demonic Counsel")
                .withCardInGraveyard(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false

            // Walk into the opponent's upkeep.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id

            if (game.state.pendingDecision is ChooseTargetsDecision) {
                game.selectTargets(listOf(bears))
            }
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            withClue("delirium is satisfied, so the upkeep trigger taps that player's creature") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }
        }

        test("without delirium the upkeep trigger does nothing") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Topplegeist")
                .withCardOnBattlefield(2, "Grizzly Bears")
                // Only two card types — creature and land.
                .withCardInGraveyard(1, "Glory Seeker")
                .withCardInGraveyard(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            withClue("two card types is short of delirium, so nothing taps") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
            }
        }
    }
}
