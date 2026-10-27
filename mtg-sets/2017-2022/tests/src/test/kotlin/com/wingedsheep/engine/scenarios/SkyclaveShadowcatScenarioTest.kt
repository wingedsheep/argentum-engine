package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Skyclave Shadowcat (ZNR #126) — {3}{B} Creature — Cat Horror, 3/3.
 *
 *   {1}{B}, Sacrifice another creature: Put a +1/+1 counter on this creature.
 *   Whenever a creature you control with a +1/+1 counter on it dies, draw a card.
 *
 * Pins the "another" on the sacrifice cost, that a counterless creature dying draws nothing, and
 * that the Shadowcat's own death counts once it carries a counter (the dies filter reads the
 * creature's last-known counters).
 */
class SkyclaveShadowcatScenarioTest : ScenarioTestBase() {

    init {
        context("Skyclave Shadowcat") {

            test("sacrificing a counterless creature grows it without drawing; its own death with a counter draws") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Skyclave Shadowcat")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shadowcat = game.findPermanent("Skyclave Shadowcat")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Skyclave Shadowcat")!!.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = shadowcat,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
                    )
                )
                withClue("Activating with the Bears as the sacrifice should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("the Shadowcat got a +1/+1 counter") {
                    game.state.getEntity(shadowcat)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                withClue("the counterless Bears dying draws nothing (Murder is the only card in hand)") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.handSize(1) shouldBe 1
                }

                game.castSpell(1, "Murder", shadowcat).error shouldBe null
                game.resolveStack()

                withClue("the Shadowcat died with a +1/+1 counter on it and drew a card") {
                    game.isInGraveyard(1, "Skyclave Shadowcat") shouldBe true
                    game.handSize(1) shouldBe 1
                    game.isInHand(1, "Island") shouldBe true
                }
            }

            test("it cannot sacrifice itself to pay its own cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Skyclave Shadowcat")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shadowcat = game.findPermanent("Skyclave Shadowcat")!!
                val abilityId = cardRegistry.getCard("Skyclave Shadowcat")!!.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = shadowcat,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(shadowcat))
                    )
                )
                withClue("\"another creature\" excludes the Shadowcat itself") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
