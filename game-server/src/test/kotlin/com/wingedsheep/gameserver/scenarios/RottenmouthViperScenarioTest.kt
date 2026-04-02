package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Rottenmouth Viper.
 *
 * Tests the new RepeatDynamicTimesEffect (repeats body N times based on DynamicAmount)
 * and blight counter mechanics.
 */
class RottenmouthViperScenarioTest : ScenarioTestBase() {

    init {
        context("Rottenmouth Viper ETB trigger") {
            test("puts a blight counter and opponent auto-loses 4 life when no other options") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Rottenmouth Viper")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(2)

                val castResult = game.castSpell(1, "Rottenmouth Viper")
                withClue("Should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve spell → ETB trigger → adds blight counter →
                // opponent has no permanents or cards, "Lose 4 life" auto-executes
                game.resolveStack()

                game.getLifeTotal(2) shouldBe initialLife - 4

                // Verify blight counter
                val viper = game.findAllPermanents("Rottenmouth Viper").first()
                val counters = game.state.getEntity(viper)?.get<CountersComponent>()
                counters?.counters?.get(CounterType.BLIGHT) shouldBe 1
            }

            test("opponent can choose to discard a card instead of losing life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Rottenmouth Viper")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInHand(2, "Swamp") // opponent has a card to discard
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(2)

                game.castSpell(1, "Rottenmouth Viper")
                game.resolveStack()

                // Opponent gets choice: discard or lose 4 life (no nonland permanents)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                val options = (decision as ChooseOptionDecision).options
                options.size shouldBe 2

                // Choose "Discard a card"
                val discardIndex = options.indexOf("Discard a card")
                game.submitDecision(OptionChosenResponse(decision.id, discardIndex))

                // Resolve the discard selection
                val selectDecision = game.getPendingDecision()
                if (selectDecision != null) {
                    val handCards = game.findCardsInHand(2, "Swamp")
                    game.selectCards(handCards.take(1))
                }

                // Opponent should NOT have lost life
                game.getLifeTotal(2) shouldBe initialLife
                game.graveyardSize(2) shouldBe 1
            }

            test("with existing blight counter, attack trigger deals 4 life per counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rottenmouth Viper")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Pre-set 1 blight counter (simulates a prior ETB)
                val viper = game.findAllPermanents("Rottenmouth Viper").first()
                game.state = game.state.updateEntity(viper) { container ->
                    container.with(CountersComponent(mapOf(CounterType.BLIGHT to 1)))
                }

                val initialLife = game.getLifeTotal(2)

                // Declare Rottenmouth Viper as attacker
                game.declareAttackers(mapOf("Rottenmouth Viper" to 2))

                // Attack trigger goes on stack, resolve it
                // +1 blight counter (now 2 total)
                // For each of 2 counters, opponent has no cards/permanents → auto-loses 4 life each
                game.resolveStack()

                // Total: 8 life lost
                game.getLifeTotal(2) shouldBe initialLife - 8

                // Viper should now have 2 blight counters
                val counters = game.state.getEntity(viper)?.get<CountersComponent>()
                counters?.counters?.get(CounterType.BLIGHT) shouldBe 2
            }
        }
    }
}
