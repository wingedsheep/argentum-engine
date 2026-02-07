package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Birchlore Rangers.
 *
 * Card reference:
 * - Birchlore Rangers (G): 1/1 Creature — Elf Druid Ranger
 *   "Tap two untapped Elves you control: Add one mana of any color."
 *   Morph {G}
 */
class BirchloreRangersScenarioTest : ScenarioTestBase() {

    init {
        context("Birchlore Rangers tap two Elves ability") {
            test("adds one mana of the chosen color when tapping two Elves") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!
                val warrior = game.findPermanent("Elvish Warrior")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(rangers, warrior)
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = costPayment,
                        manaColorChoice = Color.RED
                    )
                )

                withClue("Ability should activate successfully") {
                    activateResult.error shouldBe null
                }

                // Both Elves should be tapped
                withClue("Birchlore Rangers should be tapped") {
                    game.state.getEntity(rangers)?.has<TappedComponent>() shouldBe true
                }
                withClue("Elvish Warrior should be tapped") {
                    game.state.getEntity(warrior)?.has<TappedComponent>() shouldBe true
                }

                // Mana pool should have 1 red mana
                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Should have 1 red mana in pool") {
                    manaPool.red shouldBe 1
                }
                withClue("Should have no other colored mana") {
                    manaPool.green shouldBe 0
                    manaPool.blue shouldBe 0
                    manaPool.white shouldBe 0
                    manaPool.black shouldBe 0
                }
            }

            test("can produce any color of mana") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Wirewood Elf")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!
                val warrior = game.findPermanent("Elvish Warrior")!!
                val wirewood = game.findPermanent("Wirewood Elf")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                // First activation: produce blue mana by tapping Rangers + Warrior
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(rangers, warrior)),
                        manaColorChoice = Color.BLUE
                    )
                )

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Should have 1 blue mana") {
                    manaPool.blue shouldBe 1
                }
            }

            test("cannot activate with fewer than two Elves") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(rangers)
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = costPayment,
                        manaColorChoice = Color.GREEN
                    )
                )

                withClue("Ability should fail with only one Elf") {
                    activateResult.error shouldNotBe null
                }
            }

            test("non-Elf creatures cannot be tapped for the cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Goblin Piledriver")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!
                val goblin = game.findPermanent("Goblin Piledriver")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(rangers, goblin)
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = costPayment,
                        manaColorChoice = Color.GREEN
                    )
                )

                withClue("Ability should fail when tapping a non-Elf") {
                    activateResult.error shouldNotBe null
                }
            }

            test("already tapped Elves cannot be used") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!
                val warrior = game.findPermanent("Elvish Warrior")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(rangers, warrior)
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = costPayment,
                        manaColorChoice = Color.GREEN
                    )
                )

                withClue("Ability should fail when one Elf is already tapped") {
                    activateResult.error shouldNotBe null
                }
            }
        }
    }
}
