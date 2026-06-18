package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Nexus of Becoming (BIG #25).
 *
 * {6} Artifact.
 *   At the beginning of combat on your turn, draw a card. Then you may exile an artifact or
 *   creature card from your hand. If you do, create a token that's a copy of the exiled card,
 *   except it's a 3/3 Golem artifact creature in addition to its other types.
 */
class NexusOfBecomingScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    /** Advance from precombat main to the begin-of-combat trigger pausing for the exile choice. */
    private fun TestGame.advanceToNexusChoice(): SelectCardsDecision {
        var safety = 0
        while (getPendingDecision() == null && state.step != Step.BEGIN_COMBAT && safety++ < 30) {
            passPriority()
        }
        // The begin-combat trigger is on the stack; resolve it (draw, then pause for the exile choice).
        var safety2 = 0
        while (getPendingDecision() == null && safety2++ < 10) {
            passPriority()
        }
        return getPendingDecision() as SelectCardsDecision
    }

    init {
        test("exiling a creature card creates a 3/3 Golem artifact creature copy of it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Nexus of Becoming")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val decision = game.advanceToNexusChoice()

            withClue("the card was drawn before the exile choice (Hill Giant now in hand)") {
                game.isInHand(1, "Hill Giant") shouldBe true
            }

            val bearsInHand = game.findCardsInHand(1, "Grizzly Bears").first()
            withClue("Grizzly Bears (a creature card in hand) is an option to exile") {
                decision.options shouldContain bearsInHand
            }
            game.selectCards(listOf(bearsInHand))
            game.resolveStack()

            withClue("a token copy of the exiled Grizzly Bears is created") {
                game.findPermanents("Grizzly Bears").size shouldBe 1
            }
            val token = game.findPermanents("Grizzly Bears").first()
            val tokenCard = game.state.getEntity(token)!!.get<CardComponent>()!!
            withClue("token is an artifact creature with the Golem subtype, in addition to its own types") {
                tokenCard.typeLine.cardTypes shouldContain CardType.ARTIFACT
                tokenCard.typeLine.cardTypes shouldContain CardType.CREATURE
                tokenCard.typeLine.subtypes shouldContain Subtype("Golem")
                tokenCard.typeLine.subtypes shouldContain Subtype("Bear")
            }
            val projected = projector.project(game.state)
            withClue("token's P/T is overridden to 3/3") {
                projected.getPower(token) shouldBe 3
                projected.getToughness(token) shouldBe 3
            }
        }

        test("declining the exile draws but creates no token") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Nexus of Becoming")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val decision = game.advanceToNexusChoice()
            withClue("the optional exile can be skipped (minimum 0 selections)") {
                decision.minSelections shouldBe 0
            }
            game.skipSelection()
            game.resolveStack()

            withClue("no token copy is created when the exile is declined") {
                game.findPermanents("Grizzly Bears").size shouldBe 0
            }
            withClue("the draw still happened (Hill Giant in hand, Grizzly Bears still in hand)") {
                game.isInHand(1, "Hill Giant") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
