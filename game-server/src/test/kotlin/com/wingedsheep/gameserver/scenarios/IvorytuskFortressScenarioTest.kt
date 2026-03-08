package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ivorytusk Fortress.
 *
 * Ivorytusk Fortress: {2}{W}{B}{G}
 * Creature — Elephant 5/7
 * Untap each creature you control with a +1/+1 counter on it during each other player's untap step.
 */
class IvorytuskFortressScenarioTest : ScenarioTestBase() {

    init {
        context("Ivorytusk Fortress untap during opponent's untap step") {

            test("untaps tapped creatures with +1/+1 counters during opponent's untap step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ivorytusk Fortress")
                    .withCardOnBattlefield(1, "Glory Seeker", tapped = true)  // will get +1/+1 counter
                    .withCardOnBattlefield(1, "Forest", tapped = true)        // land, no counter
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                // Add a +1/+1 counter to Glory Seeker
                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                game.state = game.state.updateEntity(glorySeekerId) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }

                val forestId = game.findPermanent("Forest")!!

                // Verify initial state
                game.state.getEntity(glorySeekerId)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(forestId)!!.has<TappedComponent>() shouldBe true

                // Advance through turn transition into Player 2's upkeep
                // This triggers Player 2's untap step, which should untap P1's creatures with counters
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Glory Seeker (creature with +1/+1 counter) should be untapped
                game.state.getEntity(glorySeekerId)!!.has<TappedComponent>() shouldBe false

                // Forest (land, no counter) should still be tapped
                game.state.getEntity(forestId)!!.has<TappedComponent>() shouldBe true
            }

            test("does not untap creatures without +1/+1 counters during opponent's untap step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ivorytusk Fortress")
                    .withCardOnBattlefield(1, "Glory Seeker", tapped = true)  // no counter
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                game.state.getEntity(glorySeekerId)!!.has<TappedComponent>() shouldBe true

                // Advance through opponent's untap step
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Glory Seeker without counter should stay tapped
                game.state.getEntity(glorySeekerId)!!.has<TappedComponent>() shouldBe true
            }

            test("untaps normally during controller's own untap step regardless of counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ivorytusk Fortress")
                    .withCardOnBattlefield(1, "Glory Seeker", tapped = true)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                game.state.getEntity(glorySeekerId)!!.has<TappedComponent>() shouldBe true

                // Advance through Player 1's own untap step
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Glory Seeker untaps normally during own untap step (no counter needed)
                game.state.getEntity(glorySeekerId)!!.has<TappedComponent>() shouldBe false
            }
        }
    }
}
