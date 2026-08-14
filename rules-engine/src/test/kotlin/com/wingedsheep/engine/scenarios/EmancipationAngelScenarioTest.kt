package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Emancipation Angel (AVR #19) — the ETB bounce is a resolution-time choice, not a target, so the
 * pipeline surfaces a [SelectCardsDecision] over the permanents you control.
 */
class EmancipationAngelScenarioTest : ScenarioTestBase() {
    init {
        test("ETB returns a chosen permanent you control to its owner's hand") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Emancipation Angel")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Emancipation Angel").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision as? SelectCardsDecision
            decision shouldNotBe null
            withClue("the choice is offered over permanents you control, including the Angel itself") {
                decision!!.options.contains(bears) shouldBe true
            }
            game.selectCards(listOf(bears))
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            withClue("the chosen permanent goes to hand, the Angel stays") {
                game.findPermanent("Grizzly Bears") shouldBe null
                game.findPermanent("Emancipation Angel") shouldNotBe null
                game.state.getZone(game.player1Id, Zone.HAND).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                } shouldBe listOf("Grizzly Bears")
            }
        }

        test("the Angel can bounce itself when it is your only permanent") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Emancipation Angel")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Emancipation Angel").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision as? SelectCardsDecision
            decision shouldNotBe null
            val angel = game.findPermanent("Emancipation Angel")!!
            withClue("the bounce is mandatory, so the Angel is a legal (and here forced) pick") {
                decision!!.options.contains(angel) shouldBe true
            }
            game.selectCards(listOf(angel))
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            game.findPermanent("Emancipation Angel") shouldBe null
        }
    }
}
