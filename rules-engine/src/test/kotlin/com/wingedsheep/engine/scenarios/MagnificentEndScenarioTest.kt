package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Magnificent End (HOB) — {4}{W} Instant.
 *
 * "This spell costs {3} less to cast if it targets a tapped creature.
 *  Magnificent End deals 5 damage to target creature."
 *
 * The conditional cost reduction is the interesting half, so it is measured directly off the
 * [CostCalculator] for an untapped target (no discount) and a tapped one ({3} off), and the
 * discounted cast is then actually paid for and resolved.
 */
class MagnificentEndScenarioTest : ScenarioTestBase() {

    private val calculator = CostCalculator(cardRegistry)

    init {
        context("Magnificent End") {

            test("with no target chosen it costs the printed {4}{W} = 5") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Magnificent End")
                    .build()

                calculator.calculateEffectiveCost(
                    game.state, cardRegistry.requireCard("Magnificent End"), game.player1Id
                ).cmc shouldBe 5
            }

            test("targeting an untapped creature does not discount it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Magnificent End")
                    .withCardOnBattlefield(2, "Centaur Courser", tapped = false)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                withClue("the rider requires a *tapped* creature") {
                    calculator.calculateEffectiveCost(
                        game.state, cardRegistry.requireCard("Magnificent End"),
                        game.player1Id, listOf(courser)
                    ).cmc shouldBe 5
                }
            }

            test("targeting a tapped creature takes {3} off, leaving {1}{W} = 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Magnificent End")
                    .withCardOnBattlefield(2, "Centaur Courser", tapped = true)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                calculator.calculateEffectiveCost(
                    game.state, cardRegistry.requireCard("Magnificent End"),
                    game.player1Id, listOf(courser)
                ).cmc shouldBe 2
            }

            test("the discounted spell is castable on two lands and deals 5 damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Magnificent End")
                    // Only two lands — enough only if the {3} discount really applies.
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Old Fat Spider", tapped = true)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spider = game.findPermanent("Old Fat Spider")!!
                game.castSpell(1, "Magnificent End", targetId = spider).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                // Old Fat Spider's own "targeted by an opponent" trigger draws it a card.
                if (game.state.stack.isNotEmpty()) game.resolveStack()

                withClue("5 damage marked on the 6/7") {
                    game.state.getEntity(spider)?.get<DamageComponent>()?.amount shouldBe 5
                }
                withClue("5 damage does not kill a 6/7") {
                    game.isOnBattlefield("Old Fat Spider") shouldBe true
                }
            }

            test("5 damage kills a creature with toughness 5 or less") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Magnificent End")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Ordinary Bear", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Ordinary Bear")!!
                game.castSpell(1, "Magnificent End", targetId = bear).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("a 4/5 taking 5 dies to lethal damage") {
                    game.isOnBattlefield("Ordinary Bear") shouldBe false
                    game.isInGraveyard(2, "Ordinary Bear") shouldBe true
                }
            }
        }
    }
}
