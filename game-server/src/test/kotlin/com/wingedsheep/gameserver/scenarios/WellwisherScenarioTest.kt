package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Wellwisher's activated ability.
 *
 * Card reference:
 * - Wellwisher (1G): 1/1 Creature - Elf
 *   "{T}: You gain 1 life for each Elf on the battlefield."
 */
class WellwisherScenarioTest : ScenarioTestBase() {

    init {
        context("Wellwisher tribal life gain") {
            test("gains life equal to number of Elves on battlefield") {
                // Setup: Player 1 has Wellwisher and two other Elves
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Wellwisher")  // 1 Elf
                    .withCardOnBattlefield(1, "Wirewood Elf") // 2 Elves
                    .withCardOnBattlefield(1, "Elvish Warrior") // 3 Elves
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify initial state
                withClue("Player 1 should start at 10 life") {
                    game.getLifeTotal(1) shouldBe 10
                }

                // Activate Wellwisher's ability
                val wellwisher = game.findPermanent("Wellwisher")!!
                val cardDef = cardRegistry.getCard("Wellwisher")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wellwisher,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Should gain 3 life (3 Elves on battlefield)
                withClue("Player 1 should gain 3 life (3 Elves)") {
                    game.getLifeTotal(1) shouldBe 13
                }
            }

            test("counts opponent's Elves too") {
                // Setup: Both players have Elves
                val game = scenario()
                    .withPlayers("Elf Player", "Elf Opponent")
                    .withCardOnBattlefield(1, "Wellwisher")  // 1 Elf (player 1)
                    .withCardOnBattlefield(2, "Elvish Warrior") // 1 Elf (player 2)
                    .withCardOnBattlefield(2, "Wirewood Elf") // 1 Elf (player 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Activate Wellwisher
                val wellwisher = game.findPermanent("Wellwisher")!!
                val cardDef = cardRegistry.getCard("Wellwisher")!!
                val ability = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wellwisher,
                        abilityId = ability.id
                    )
                )
                game.resolveStack()

                // Should gain 3 life (1 Elf you control + 2 opponent Elves)
                withClue("Player 1 should gain 3 life (all Elves count)") {
                    game.getLifeTotal(1) shouldBe 13
                }
            }

            test("gains only 1 life when Wellwisher is the only Elf") {
                // Setup: Wellwisher is the only Elf
                val game = scenario()
                    .withPlayers("Lonely Elf", "Opponent")
                    .withCardOnBattlefield(1, "Wellwisher")  // Only 1 Elf
                    .withCardOnBattlefield(1, "Grizzly Bears") // Not an Elf
                    .withCardOnBattlefield(2, "Hill Giant") // Not an Elf
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Activate Wellwisher
                val wellwisher = game.findPermanent("Wellwisher")!!
                val cardDef = cardRegistry.getCard("Wellwisher")!!
                val ability = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wellwisher,
                        abilityId = ability.id
                    )
                )
                game.resolveStack()

                // Should gain only 1 life (Wellwisher itself is the only Elf)
                withClue("Player 1 should gain 1 life (only Wellwisher is an Elf)") {
                    game.getLifeTotal(1) shouldBe 11
                }
            }
        }
    }
}
