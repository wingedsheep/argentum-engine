package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bubble Snare (ZNR #47) — {U} Enchantment — Aura.
 *
 *   Kicker {2}{U}
 *   Enchant creature
 *   When this Aura enters, if it was kicked, tap enchanted creature.
 *   Enchanted creature doesn't untap during its controller's untap step.
 */
class BubbleSnareScenarioTest : ScenarioTestBase() {

    private fun game(bearsTapped: Boolean = false) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Bubble Snare")
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = bearsTapped)
        .withLandsOnBattlefield(1, "Island", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Bubble Snare") {

            test("kicked, it taps the enchanted creature, which then stays tapped through its untap step") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!
                val snare = game.findCardsInHand(1, "Bubble Snare").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = snare,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        declaredCostSlot = ChoiceSlot.KICKED,
                        paymentStrategy = PaymentStrategy.AutoPay,
                    )
                ).error shouldBe null
                game.resolveStack()

                val snareId = game.findPermanent("Bubble Snare")!!
                withClue("the Aura is attached to the Bears") {
                    game.state.getEntity(snareId)!!.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                withClue("kicked: the enter trigger taps the enchanted creature") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the Bears don't untap during their controller's untap step") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
            }

            test("unkicked, the enchanted creature is not tapped") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Bubble Snare", bears).error shouldBe null
                game.resolveStack()

                withClue("no kicker paid, so the intervening-if trigger does nothing") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
                }
            }

            test("unkicked on an already-tapped creature, it still keeps it from untapping") {
                val game = game(bearsTapped = true)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Bubble Snare", bears).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the static lock applies whether or not it was kicked") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
