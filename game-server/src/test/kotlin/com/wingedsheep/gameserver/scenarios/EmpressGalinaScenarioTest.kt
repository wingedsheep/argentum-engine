package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Empress Galina.
 *
 * {3}{U}{U} Legendary Creature — Merfolk Noble 1/3
 * "{U}{U}, {T}: Gain control of target legendary permanent. (This effect lasts indefinitely.)"
 */
class EmpressGalinaScenarioTest : ScenarioTestBase() {

    init {
        context("Empress Galina steals a legendary permanent") {
            test("activating the ability gives the controller control of a target legendary") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Empress Galina")
                    .withCardOnBattlefield(2, "Blind Seer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Give the controller {U}{U} for the activation cost.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(ManaPoolComponent(blue = 2))
                }

                val galina = game.findPermanent("Empress Galina")!!
                val blindSeer = game.findPermanent("Blind Seer")!!

                // Sanity: Blind Seer starts under the opponent's control.
                withClue("Blind Seer starts controlled by the opponent") {
                    game.state.projectedState.getController(blindSeer) shouldBe game.player2Id
                }

                val ability = cardRegistry.getCard("Empress Galina")!!.script.activatedAbilities[0]
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = galina,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(blindSeer))
                    )
                )
                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Blind Seer should now be controlled by Empress Galina's controller") {
                    game.state.projectedState.getController(blindSeer) shouldBe game.player1Id
                }
            }
        }
    }
}
