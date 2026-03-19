package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Hired Claw.
 *
 * Hired Claw {R}
 * Creature — Lizard Mercenary
 * 1/2
 *
 * Whenever you attack with one or more Lizards, this creature deals 1 damage to target opponent.
 * {1}{R}: Put a +1/+1 counter on this creature. Activate only if an opponent lost life this turn
 * and only once each turn.
 */
class HiredClawTest : ScenarioTestBase() {

    init {
        context("Hired Claw triggered ability") {
            test("deals 1 damage to opponent when attacking with a Lizard") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hired Claw")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Hired Claw (itself a Lizard)
                val attackResult = game.declareAttackers(mapOf("Hired Claw" to 2))
                withClue("Declaring Hired Claw as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // In 2-player game, opponent is auto-selected as target
                // Resolve the triggered ability
                game.resolveStack()

                // Opponent should have taken 1 damage from the trigger
                withClue("Opponent should take 1 damage from triggered ability") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }

        context("Hired Claw activated ability") {
            test("adds +1/+1 counter when opponent lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hired Claw")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Mark opponent as having lost life this turn
                game.state = game.state.updateEntity(game.player2Id) { container ->
                    container.with(LifeLostThisTurnComponent)
                }

                // Activate the ability
                val hiredClawId = game.findPermanent("Hired Claw")!!
                val cardDef = cardRegistry.getCard("Hired Claw")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hiredClawId,
                        abilityId = ability.id
                    )
                )
                withClue("Ability activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Hired Claw should now have a +1/+1 counter
                val counters = game.state.getEntity(hiredClawId)?.get<CountersComponent>()
                withClue("Hired Claw should have 1 +1/+1 counter") {
                    counters shouldNotBe null
                    counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("cannot be activated if opponent hasn't lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hired Claw")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Try to activate — should fail because opponent hasn't lost life
                val hiredClawId = game.findPermanent("Hired Claw")!!
                val cardDef = cardRegistry.getCard("Hired Claw")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hiredClawId,
                        abilityId = ability.id
                    )
                )
                withClue("Activation should fail without opponent life loss") {
                    result.error shouldNotBe null
                }
            }

            test("can only be activated once per turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hired Claw")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Mark opponent as having lost life this turn
                game.state = game.state.updateEntity(game.player2Id) { container ->
                    container.with(LifeLostThisTurnComponent)
                }

                // Activate the ability once
                val hiredClawId = game.findPermanent("Hired Claw")!!
                val cardDef = cardRegistry.getCard("Hired Claw")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hiredClawId,
                        abilityId = ability.id
                    )
                )
                withClue("First activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Try activating again — should fail
                val result2 = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hiredClawId,
                        abilityId = ability.id
                    )
                )
                withClue("Second activation should fail (once per turn)") {
                    result2.error shouldNotBe null
                }
            }
        }
    }
}
