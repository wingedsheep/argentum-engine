package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Catapult Master.
 *
 * Card reference:
 * - Catapult Master (3WW): 3/3 Creature — Human Soldier
 *   "Tap five untapped Soldiers you control: Exile target creature."
 */
class CatapultMasterScenarioTest : ScenarioTestBase() {

    init {
        context("Catapult Master tap five soldiers ability") {

            test("exiles target creature when tapping five soldiers") {
                // Setup: Player 1 has Catapult Master + 4 other Soldiers (5 total)
                val game = scenario()
                    .withPlayers("Soldier Player", "Opponent")
                    .withCardOnBattlefield(1, "Catapult Master")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Elvish Warrior") // Opponent's creature to exile
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify initial state
                withClue("Elvish Warrior should start on battlefield") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }

                // Find all soldiers
                val master = game.findPermanent("Catapult Master")!!
                val soldiers = listOf(master) + game.findAllPermanents("Glory Seeker")

                withClue("Should have 5 soldiers") {
                    soldiers.size shouldBe 5
                }

                // Find the target to exile
                val targetId = game.findPermanent("Elvish Warrior")!!

                // Activate the ability with 5 soldiers as tap cost, targeting opponent's creature
                val cardDef = cardRegistry.getCard("Catapult Master")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = soldiers
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = master,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(targetId)),
                        costPayment = costPayment
                    )
                )

                withClue("Ability should activate successfully: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability on the stack
                game.resolveStack()

                // Elvish Warrior should be exiled (no longer on battlefield)
                withClue("Elvish Warrior should no longer be on the battlefield") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                }

                // Verify it's in exile, not graveyard
                withClue("Elvish Warrior should not be in graveyard") {
                    game.isInGraveyard(2, "Elvish Warrior") shouldBe false
                }
            }

            test("cannot activate with fewer than five soldiers") {
                // Setup: Player 1 has only 3 Soldiers total
                val game = scenario()
                    .withPlayers("Soldier Player", "Opponent")
                    .withCardOnBattlefield(1, "Catapult Master")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val master = game.findPermanent("Catapult Master")!!
                val soldiers = listOf(master) + game.findAllPermanents("Glory Seeker")

                withClue("Should only have 3 soldiers") {
                    soldiers.size shouldBe 3
                }

                val targetId = game.findPermanent("Elvish Warrior")!!

                val cardDef = cardRegistry.getCard("Catapult Master")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = soldiers
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = master,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(targetId)),
                        costPayment = costPayment
                    )
                )

                withClue("Ability should fail with insufficient soldiers") {
                    activateResult.error shouldNotBe null
                }

                // Target should remain on battlefield
                withClue("Elvish Warrior should still be on the battlefield") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
            }

            test("can exile own creature") {
                // Setup: Player 1 has 5 Soldiers and also controls the target
                val game = scenario()
                    .withPlayers("Soldier Player", "Opponent")
                    .withCardOnBattlefield(1, "Catapult Master")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Dive Bomber") // 5th Soldier + target
                    .withCardOnBattlefield(1, "Elvish Warrior") // Target to exile
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val master = game.findPermanent("Catapult Master")!!
                val diveBomber = game.findPermanent("Dive Bomber")!!
                val soldiers = listOf(master, diveBomber) + game.findAllPermanents("Glory Seeker")

                withClue("Should have 5 soldiers") {
                    soldiers.size shouldBe 5
                }

                val targetId = game.findPermanent("Elvish Warrior")!!

                val cardDef = cardRegistry.getCard("Catapult Master")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = soldiers
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = master,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(targetId)),
                        costPayment = costPayment
                    )
                )

                withClue("Ability should activate successfully: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Elvish Warrior should be exiled") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                }
            }
        }
    }
}
