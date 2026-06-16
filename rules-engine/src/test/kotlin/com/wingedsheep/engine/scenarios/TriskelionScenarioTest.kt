package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Triskelion (ATQ #73).
 *
 * {6} Artifact Creature — Construct 1/1
 * "This creature enters with three +1/+1 counters on it.
 *  Remove a +1/+1 counter from this creature: It deals 1 damage to any target."
 */
class TriskelionScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        fun plusOne(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        context("Triskelion") {

            // The ETB "enters with three +1/+1 counters" replacement only fires when the card
            // actually enters the battlefield, so the spell is cast & resolved rather than placed
            // directly with withCardOnBattlefield.
            test("enters with three +1/+1 counters and is a 4/4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Triskelion")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Triskelion").error shouldBe null
                game.resolveStack()

                val trisk = game.findPermanent("Triskelion")!!
                withClue("Triskelion enters with three +1/+1 counters") {
                    plusOne(game, trisk) shouldBe 3
                }

                val projected = stateProjector.project(game.state)
                withClue("Base 1/1 + three +1/+1 counters projects as a 4/4") {
                    projected.getPower(trisk) shouldBe 4
                    projected.getToughness(trisk) shouldBe 4
                }
            }

            test("removing a +1/+1 counter deals 1 damage to a player and shrinks Triskelion") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Triskelion")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Triskelion").error shouldBe null
                game.resolveStack()

                val trisk = game.findPermanent("Triskelion")!!
                withClue("Starts with three counters") { plusOne(game, trisk) shouldBe 3 }

                val ability = cardRegistry.getCard("Triskelion")!!.script.activatedAbilities[0]
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = trisk,
                        abilityId = ability.id,
                        targets = listOf(entityIdToChosenTarget(game.state, game.player2Id))
                    )
                )
                withClue("Activating the remove-counter ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The opponent took 1 damage") {
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("A +1/+1 counter was removed as the activation cost") {
                    plusOne(game, trisk) shouldBe 2
                }
                val projected = stateProjector.project(game.state)
                withClue("With one fewer counter Triskelion is a 3/3") {
                    projected.getPower(trisk) shouldBe 3
                    projected.getToughness(trisk) shouldBe 3
                }
            }
        }
    }
}
