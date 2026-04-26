package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Foraging Wickermaw.
 *
 * - {2} Artifact Creature — Scarecrow (1/3)
 *   When this creature enters, surveil 1.
 *   {1}: Add one mana of any color. This creature becomes that color until end of turn.
 *        Activate only once each turn.
 *
 * Focus: the new BecomeChosenManaColor effect — verifies the mana ability produces
 * the chosen color AND grants that color to the source for the rest of the turn.
 */
class ForagingWickermawScenarioTest : ScenarioTestBase() {

    init {
        context("Foraging Wickermaw mana ability") {
            test("adds one mana of the chosen color and the creature becomes that color") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Foraging Wickermaw", summoningSickness = false)
                    .withCardOnBattlefield(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wickermaw = game.findPermanent("Foraging Wickermaw")!!
                val cardDef = cardRegistry.getCard("Foraging Wickermaw")!!
                val ability = cardDef.script.activatedAbilities.first()

                val projectedBefore = game.state.projectedState
                withClue("Wickermaw is colorless before activation") {
                    projectedBefore.getColors(wickermaw).isEmpty() shouldBe true
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wickermaw,
                        abilityId = ability.id,
                        manaColorChoice = Color.BLUE
                    )
                )

                withClue("Activation should succeed (auto-pays {1} from Forest)") {
                    result.error shouldBe null
                }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Should have 1 blue mana in pool") { pool.blue shouldBe 1 }

                val projectedAfter = game.state.projectedState
                withClue("Wickermaw should now be blue") {
                    projectedAfter.getColors(wickermaw) shouldBe setOf("BLUE")
                }
            }

            test("can produce any color and the creature reflects the latest pick") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Foraging Wickermaw", summoningSickness = false)
                    .withCardOnBattlefield(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wickermaw = game.findPermanent("Foraging Wickermaw")!!
                val cardDef = cardRegistry.getCard("Foraging Wickermaw")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wickermaw,
                        abilityId = ability.id,
                        manaColorChoice = Color.GREEN
                    )
                )
                withClue("Activation should succeed") { result.error shouldBe null }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Should have 1 green mana in pool") { pool.green shouldBe 1 }

                val projected = game.state.projectedState
                withClue("Wickermaw should be green") {
                    projected.getColors(wickermaw) shouldBe setOf("GREEN")
                }
            }

            test("once-per-turn restriction prevents a second activation in the same turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Foraging Wickermaw", summoningSickness = false)
                    .withCardOnBattlefield(1, "Forest")
                    .withCardOnBattlefield(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wickermaw = game.findPermanent("Foraging Wickermaw")!!
                val cardDef = cardRegistry.getCard("Foraging Wickermaw")!!
                val ability = cardDef.script.activatedAbilities.first()

                val first = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wickermaw,
                        abilityId = ability.id,
                        manaColorChoice = Color.WHITE
                    )
                )
                withClue("First activation should succeed") { first.error shouldBe null }

                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wickermaw,
                        abilityId = ability.id,
                        manaColorChoice = Color.RED
                    )
                )
                withClue("Second activation in the same turn should fail") {
                    second.error.shouldNotBeNullAndMentionOncePerTurn()
                }
            }
        }
    }
}

private fun String?.shouldNotBeNullAndMentionOncePerTurn() {
    require(this != null) { "Expected an error message but got null" }
    require(this.contains("once", ignoreCase = true)) {
        "Expected error to mention once-per-turn restriction, got: $this"
    }
}
