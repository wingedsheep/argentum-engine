package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Ancestor's Prophet.
 *
 * Card reference:
 * - Ancestor's Prophet (4W): 1/5 Creature — Human Cleric
 *   "Tap five untapped Clerics you control: You gain 10 life."
 */
class AncestorsProphetScenarioTest : ScenarioTestBase() {

    init {
        context("Ancestor's Prophet tap five clerics ability") {
            test("gains 10 life when tapping five clerics") {
                // Setup: Player 1 has Ancestor's Prophet + 4 other Clerics (5 total)
                val game = scenario()
                    .withPlayers("Cleric Player", "Opponent")
                    .withCardOnBattlefield(1, "Ancestor's Prophet")
                    .withCardOnBattlefield(1, "Disciple of Grace")
                    .withCardOnBattlefield(1, "Disciple of Grace")
                    .withCardOnBattlefield(1, "Disciple of Malice")
                    .withCardOnBattlefield(1, "Venerable Monk")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify initial state
                withClue("Player 1 should start at 20 life") {
                    game.getLifeTotal(1) shouldBe 20
                }

                // Find all clerics on the battlefield
                val prophet = game.findPermanent("Ancestor's Prophet")!!
                val clerics = game.findAllPermanents("Disciple of Grace") +
                    game.findAllPermanents("Disciple of Malice") +
                    game.findAllPermanents("Venerable Monk") +
                    listOf(prophet)

                withClue("Should have 5 clerics") {
                    clerics.size shouldBe 5
                }

                // Activate the ability with 5 clerics as tap cost
                val cardDef = cardRegistry.getCard("Ancestor's Prophet")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = clerics
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = prophet,
                        abilityId = ability.id,
                        costPayment = costPayment
                    )
                )

                withClue("Ability should activate successfully") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability on the stack
                game.resolveStack()

                // Should gain 10 life
                withClue("Player 1 should gain 10 life") {
                    game.getLifeTotal(1) shouldBe 30
                }
            }

            test("can tap clerics with summoning sickness for the cost") {
                // Summoning sickness only prevents a creature from attacking or
                // activating its own {T} abilities. Other permanents can still tap it.
                val game = scenario()
                    .withPlayers("ClericMaster", "Opponent")
                    .withCardOnBattlefield(1, "Ancestor's Prophet", summoningSickness = false)
                    .withCardOnBattlefield(1, "Nova Cleric", summoningSickness = true)
                    .withCardOnBattlefield(1, "Battlefield Medic", summoningSickness = true)
                    .withCardOnBattlefield(1, "Daru Healer", summoningSickness = true)
                    .withCardOnBattlefield(1, "Whipcorder", summoningSickness = true)
                    .withCardOnBattlefield(1, "Disciple of Grace", summoningSickness = true)
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val prophet = game.findPermanent("Ancestor's Prophet")!!
                val otherClerics = game.findAllPermanents("Nova Cleric") +
                    game.findAllPermanents("Battlefield Medic") +
                    game.findAllPermanents("Daru Healer") +
                    game.findAllPermanents("Whipcorder") +
                    game.findAllPermanents("Disciple of Grace")

                // Use prophet + 4 summoning-sick clerics = 5 total
                val clericsToPay = listOf(prophet) + otherClerics.take(4)

                val cardDef = cardRegistry.getCard("Ancestor's Prophet")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = clericsToPay
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = prophet,
                        abilityId = ability.id,
                        costPayment = costPayment
                    )
                )

                withClue("Ability should activate successfully even with summoning-sick clerics") {
                    activateResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Player 1 should gain 10 life (10 -> 20)") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("cannot activate with fewer than five clerics") {
                // Setup: Player 1 has only 3 Clerics
                val game = scenario()
                    .withPlayers("Cleric Player", "Opponent")
                    .withCardOnBattlefield(1, "Ancestor's Prophet")
                    .withCardOnBattlefield(1, "Disciple of Grace")
                    .withCardOnBattlefield(1, "Disciple of Malice")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Try to activate with only 3 clerics
                val prophet = game.findPermanent("Ancestor's Prophet")!!
                val clerics = listOf(prophet) +
                    game.findAllPermanents("Disciple of Grace") +
                    game.findAllPermanents("Disciple of Malice")

                val cardDef = cardRegistry.getCard("Ancestor's Prophet")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = clerics
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = prophet,
                        abilityId = ability.id,
                        costPayment = costPayment
                    )
                )

                withClue("Ability should fail with insufficient clerics") {
                    activateResult.error shouldNotBe null
                }

                // Life should remain unchanged
                withClue("Player 1 life should be unchanged") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
