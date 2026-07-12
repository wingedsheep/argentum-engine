package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Runebound Wolf (VOW #176) — {1}{R} Creature — Wolf, 2/2.
 *
 *   {3}{R}, {T}: This creature deals damage equal to the number of Wolves and Werewolves you
 *   control to target opponent.
 *
 * Exercises the DynamicAmount.Count(Wolves + Werewolves) damage ability: with two Wolves
 * (itself + one more) on the battlefield, the ability should deal 2 damage to the opponent.
 */
class RuneboundWolfScenarioTest : ScenarioTestBase() {

    init {
        context("Runebound Wolf damage ability") {

            test("deals damage equal to the number of Wolves and Werewolves you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Runebound Wolf", summoningSickness = false)
                    .withCardOnBattlefield(1, "Sporeback Wolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 4) // pays {3}{R}
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Runebound Wolf")!!
                val abilityId = cardRegistry.getCard("Runebound Wolf")!!.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wolf,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("Activating the {3}{R}, {T} ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("Two Wolves you control (Runebound Wolf + Sporeback Wolf) deal 2 damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("Runebound Wolf is tapped by its own cost") {
                    game.state.getEntity(wolf)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
