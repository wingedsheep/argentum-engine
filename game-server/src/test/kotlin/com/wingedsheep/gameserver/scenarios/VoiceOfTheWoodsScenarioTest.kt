package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Voice of the Woods.
 *
 * Card reference:
 * - Voice of the Woods ({3}{G}{G}): 2/2 Creature — Elf
 *   "Tap five untapped Elves you control: Create a 7/7 green Elemental creature token with trample."
 */
class VoiceOfTheWoodsScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Voice of the Woods tap five elves ability") {

            test("creates a 7/7 Elemental token with trample by tapping five elves") {
                val game = scenario()
                    .withPlayers("ElfMaster", "Opponent")
                    .withCardOnBattlefield(1, "Voice of the Woods")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Wirewood Elf")
                    .withCardOnBattlefield(1, "Wellwisher")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val voice = game.findPermanent("Voice of the Woods")!!
                val elves = listOf(voice) +
                    game.findAllPermanents("Elvish Warrior") +
                    game.findAllPermanents("Wirewood Elf") +
                    game.findAllPermanents("Wellwisher")

                withClue("Should have 5 elves") {
                    elves.size shouldBe 5
                }

                val cardDef = cardRegistry.getCard("Voice of the Woods")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = voice,
                        abilityId = ability.id,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(tappedPermanents = elves)
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                val elementalTokens = game.findAllPermanents("Elemental Token")
                withClue("Should create exactly 1 Elemental token") {
                    elementalTokens.size shouldBe 1
                }

                val projected = stateProjector.project(game.state)
                val token = elementalTokens.first()

                withClue("Elemental token should be 7/7") {
                    projected.getPower(token) shouldBe 7
                    projected.getToughness(token) shouldBe 7
                }

                withClue("Elemental token should have trample") {
                    projected.hasKeyword(token, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("cannot activate with fewer than five elves") {
                val game = scenario()
                    .withPlayers("ElfMaster", "Opponent")
                    .withCardOnBattlefield(1, "Voice of the Woods")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Wirewood Elf")
                    // Only 3 elves total
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val voice = game.findPermanent("Voice of the Woods")!!
                val elves = listOf(voice) +
                    game.findAllPermanents("Elvish Warrior") +
                    game.findAllPermanents("Wirewood Elf")

                withClue("Should have only 3 elves") {
                    elves.size shouldBe 3
                }

                val cardDef = cardRegistry.getCard("Voice of the Woods")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = voice,
                        abilityId = ability.id,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(tappedPermanents = elves)
                    )
                )

                withClue("Ability should fail with insufficient elves") {
                    result.error shouldNotBe null
                }
            }

            test("can activate multiple times if enough elves are available") {
                val game = scenario()
                    .withPlayers("ElfMaster", "Opponent")
                    .withCardOnBattlefield(1, "Voice of the Woods")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Wirewood Elf")
                    .withCardOnBattlefield(1, "Wellwisher")
                    .withCardOnBattlefield(1, "Taunting Elf")
                    .withCardOnBattlefield(1, "Elvish Pioneer")
                    .withCardOnBattlefield(1, "Elvish Pioneer")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Symbiotic Elf")
                    // 10 elves total (Voice + 9 others)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val voice = game.findPermanent("Voice of the Woods")!!
                val allElves = listOf(voice) +
                    game.findAllPermanents("Elvish Warrior") +
                    game.findAllPermanents("Wirewood Elf") +
                    game.findAllPermanents("Wellwisher") +
                    game.findAllPermanents("Taunting Elf") +
                    game.findAllPermanents("Elvish Pioneer") +
                    game.findAllPermanents("Birchlore Rangers") +
                    game.findAllPermanents("Symbiotic Elf")

                withClue("Should have 10 elves") {
                    allElves.size shouldBe 10
                }

                val cardDef = cardRegistry.getCard("Voice of the Woods")!!
                val ability = cardDef.script.activatedAbilities.first()

                // First activation with first 5 elves
                val firstFive = allElves.take(5)
                val result1 = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = voice,
                        abilityId = ability.id,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(tappedPermanents = firstFive)
                    )
                )

                withClue("First activation should succeed: ${result1.error}") {
                    result1.error shouldBe null
                }

                game.resolveStack()

                // Second activation with remaining 5 elves
                val secondFive = allElves.drop(5)
                val result2 = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = voice,
                        abilityId = ability.id,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(tappedPermanents = secondFive)
                    )
                )

                withClue("Second activation should succeed: ${result2.error}") {
                    result2.error shouldBe null
                }

                game.resolveStack()

                val elementalTokens = game.findAllPermanents("Elemental Token")
                withClue("Should create 2 Elemental tokens total") {
                    elementalTokens.size shouldBe 2
                }
            }
        }
    }
}
