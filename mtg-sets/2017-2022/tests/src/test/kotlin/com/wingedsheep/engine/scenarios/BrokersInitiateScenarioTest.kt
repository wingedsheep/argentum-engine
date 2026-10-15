package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Brokers Initiate (SNC #5) — {W} Creature — Cat Citizen 0/4
 *
 *   {4}{G/U}: This creature has base power and toughness 5/5 until end of turn.
 *
 * The ability sets *base* P/T (layer 7b), so it replaces the printed 0/4 and lasts only until
 * end of turn.
 */
class BrokersInitiateScenarioTest : ScenarioTestBase() {

    init {
        context("Brokers Initiate") {

            test("activation makes it a 5/5 until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Brokers Initiate")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initiate = game.findPermanent("Brokers Initiate")!!
                val ability = cardRegistry.getCard("Brokers Initiate")!!.script.activatedAbilities[0]

                game.state.projectedState.getPower(initiate) shouldBe 0
                game.state.projectedState.getToughness(initiate) shouldBe 4

                val result = game.execute(ActivateAbility(game.player1Id, initiate, ability.id))
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("base P/T is now 5/5") {
                    game.state.projectedState.getPower(initiate) shouldBe 5
                    game.state.projectedState.getToughness(initiate) shouldBe 5
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the effect ends at end of turn") {
                    game.state.projectedState.getPower(initiate) shouldBe 0
                    game.state.projectedState.getToughness(initiate) shouldBe 4
                }
            }
        }
    }
}
