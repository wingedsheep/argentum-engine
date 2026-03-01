package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Form of the Dragon.
 *
 * Form of the Dragon: {4}{R}{R}{R}
 * Enchantment
 * At the beginning of your upkeep, this enchantment deals 5 damage to any target.
 * At the beginning of each end step, your life total becomes 5.
 * Creatures without flying can't attack you.
 */
class FormOfTheDragonScenarioTest : ScenarioTestBase() {

    init {
        context("Form of the Dragon") {

            test("upkeep trigger deals 5 damage to target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Form of the Dragon")
                    .withCardOnBattlefield(2, "Elvish Aberration")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Advance to upkeep — Form of the Dragon triggers
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // The trigger should be on the stack and need a target
                withClue("Should have pending decision for target selection") {
                    game.state.pendingDecision shouldNotBe null
                }

                // Target the opponent's creature
                val targetId = game.findPermanent("Elvish Aberration")!!
                game.selectTargets(listOf(targetId))

                // Resolve the trigger
                game.resolveStack()

                // Elvish Aberration (5/4) should be dead from 5 damage
                withClue("Elvish Aberration should be destroyed by 5 damage") {
                    game.findPermanent("Elvish Aberration") shouldBe null
                }
            }

            test("upkeep trigger deals 5 damage to target player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Form of the Dragon")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val initialLife = game.getLifeTotal(2)

                // Advance to upkeep — trigger fires
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Target opponent player
                game.selectTargets(listOf(game.player2Id))

                // Resolve the trigger
                game.resolveStack()

                withClue("Opponent should take 5 damage") {
                    game.getLifeTotal(2) shouldBe initialLife - 5
                }
            }

            test("end step sets controller's life total to 5") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Form of the Dragon")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .withLifeTotal(1, 20)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Player should start at 20 life") {
                    game.getLifeTotal(1) shouldBe 20
                }

                // Advance to end step — trigger sets life to 5
                game.passUntilPhase(Phase.ENDING, Step.END)
                // Resolve the end step trigger
                game.resolveStack()

                withClue("Player's life total should become 5") {
                    game.getLifeTotal(1) shouldBe 5
                }

                withClue("Opponent's life total should be unchanged") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("end step sets life to 5 even if below 5") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Form of the Dragon")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .withLifeTotal(1, 2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Player should start at 2 life") {
                    game.getLifeTotal(1) shouldBe 2
                }

                // Advance to end step — trigger sets life to 5
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Player's life total should become 5 (gained life)") {
                    game.getLifeTotal(1) shouldBe 5
                }
            }

            test("creatures without flying cannot attack controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Form of the Dragon")
                    .withCardOnBattlefield(2, "Elvish Aberration") // No flying
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val attackerId = game.findPermanent("Elvish Aberration")!!

                // Try to attack with a creature without flying — should fail
                val result = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(attackerId to game.player1Id))
                )

                withClue("Attacking with a creature without flying should be rejected") {
                    result.error shouldNotBe null
                }
            }

            test("creatures with flying can attack controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Form of the Dragon")
                    .withCardOnBattlefield(2, "Dragonstalker") // 3/3 flying
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val attackerId = game.findPermanent("Dragonstalker")!!

                // Attacking with a flying creature should work
                val result = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(attackerId to game.player1Id))
                )

                withClue("Attacking with a flying creature should succeed") {
                    result.error shouldBe null
                }
            }
        }
    }
}
