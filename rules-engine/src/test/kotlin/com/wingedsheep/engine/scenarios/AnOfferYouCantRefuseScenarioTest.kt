package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for An Offer You Can't Refuse (SNC).
 *
 * Oracle: "Counter target noncreature spell. Its controller creates two Treasure tokens."
 *
 * Composes [com.wingedsheep.sdk.dsl.Effects.CreateTreasure] with a `TargetController`
 * redirect + [com.wingedsheep.sdk.dsl.Effects.CounterSpell]; the Treasures resolve while
 * the countered spell is still on the stack (Undermine pattern) so "its controller" is
 * readable.
 */
class AnOfferYouCantRefuseScenarioTest : ScenarioTestBase() {

    init {
        context("An Offer You Can't Refuse") {
            test("counters a noncreature spell; its controller creates two Treasures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "An Offer You Can't Refuse")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.passPriority()

                val offer = game.castSpellTargetingStackSpell(1, "An Offer You Can't Refuse", "Lightning Bolt")
                withClue("Casting An Offer You Can't Refuse should succeed: ${offer.error}") {
                    offer.error shouldBe null
                }

                game.resolveStack()

                withClue("Lightning Bolt should be countered") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                    game.getLifeTotal(1) shouldBe lifeBefore
                }

                val treasures = game.findAllPermanents("Treasure")
                withClue("the countered spell's controller (Player 2) creates two Treasures") {
                    treasures.size shouldBe 2
                    treasures.forEach { id ->
                        game.state.getEntity(id)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                    }
                }
            }

            test("cannot target a creature spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "An Offer You Can't Refuse")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                val offer = game.castSpellTargetingStackSpell(1, "An Offer You Can't Refuse", "Grizzly Bears")
                withClue("targeting a creature spell must be rejected") {
                    offer.error shouldNotBe null
                }
            }
        }
    }
}
