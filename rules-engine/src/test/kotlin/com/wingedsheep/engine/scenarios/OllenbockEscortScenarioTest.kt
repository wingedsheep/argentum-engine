package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Ollenbock Escort (VOW #27) — {W} Creature — Human Cleric, 1/1.
 *
 *   Vigilance
 *   Sacrifice this creature: Target creature you control with a +1/+1 counter on it gains
 *   lifelink and indestructible until end of turn.
 *
 * Exercises the sacrifice-cost activated ability: sacrificing Ollenbock Escort to target a
 * counter-bearing creature you control grants it lifelink and indestructible.
 */
class OllenbockEscortScenarioTest : ScenarioTestBase() {

    private fun seedCounters(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    init {
        context("Ollenbock Escort — sacrifice to grant lifelink and indestructible") {

            test("sacrificing Ollenbock Escort grants lifelink and indestructible to a +1/+1-countered creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ollenbock Escort", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val escort = game.findPermanent("Ollenbock Escort")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                seedCounters(game, bears, 1)

                val abilityId = cardRegistry.getCard("Ollenbock Escort")!!.activatedAbilities.first().id

                withClue("Grizzly Bears does not start with lifelink or indestructible") {
                    game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
                    game.state.projectedState.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false
                }

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = escort,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Activating the ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                withClue("Ollenbock Escort is sacrificed as a cost") {
                    game.isOnBattlefield("Ollenbock Escort") shouldBe false
                }
                game.resolveStack()

                withClue("Grizzly Bears gains lifelink and indestructible until end of turn") {
                    game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
                }
            }
        }
    }
}
