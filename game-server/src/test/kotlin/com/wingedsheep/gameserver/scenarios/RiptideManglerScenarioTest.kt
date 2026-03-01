package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Riptide Mangler.
 *
 * Card reference:
 * - Riptide Mangler ({1}{U}): Creature — Beast 0/3
 *   "{1}{U}: Change this creature's base power to target creature's power.
 *   (This effect lasts indefinitely.)"
 */
class RiptideManglerScenarioTest : ScenarioTestBase() {

    init {
        context("Riptide Mangler base power change") {

            test("changes base power to target creature's power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Riptide Mangler")
                    .withCardOnBattlefield(2, "Enormous Baloth") // 7/7
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val manglerId = game.findPermanent("Riptide Mangler")!!
                val balothId = game.findPermanent("Enormous Baloth")!!

                // Verify starting stats: 0/3
                val beforeState = game.getClientState(1)
                val manglerBefore = beforeState.cards.values.find { it.name == "Riptide Mangler" }
                withClue("Riptide Mangler should start as 0/3") {
                    manglerBefore shouldNotBe null
                    manglerBefore!!.power shouldBe 0
                    manglerBefore.toughness shouldBe 3
                }

                // Activate ability targeting Enormous Baloth
                val cardDef = cardRegistry.getCard("Riptide Mangler")!!
                val ability = cardDef.script.activatedAbilities.first()
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = manglerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(balothId))
                    )
                )
                withClue("Activating ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Verify stats changed to 7/3 (power from Baloth, toughness unchanged)
                val afterState = game.getClientState(1)
                val manglerAfter = afterState.cards.values.find { it.name == "Riptide Mangler" }
                withClue("Riptide Mangler should be 7/3 after copying Enormous Baloth's power") {
                    manglerAfter shouldNotBe null
                    manglerAfter!!.power shouldBe 7
                    manglerAfter.toughness shouldBe 3
                }
            }

            test("second activation overwrites first base power change") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Riptide Mangler")
                    .withCardOnBattlefield(1, "Fugitive Wizard") // 1/1
                    .withCardOnBattlefield(2, "Enormous Baloth") // 7/7
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val manglerId = game.findPermanent("Riptide Mangler")!!
                val balothId = game.findPermanent("Enormous Baloth")!!
                val wizardId = game.findPermanent("Fugitive Wizard")!!

                val cardDef = cardRegistry.getCard("Riptide Mangler")!!
                val ability = cardDef.script.activatedAbilities.first()

                // First activation: copy Enormous Baloth's power (7)
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = manglerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(balothId))
                    )
                )
                game.resolveStack()

                val midState = game.getClientState(1)
                val manglerMid = midState.cards.values.find { it.name == "Riptide Mangler" }
                withClue("Riptide Mangler should be 7/3 after first activation") {
                    manglerMid!!.power shouldBe 7
                }

                // Second activation: copy Fugitive Wizard's power (1)
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = manglerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(wizardId))
                    )
                )
                game.resolveStack()

                // The latest SetPower floating effect should override the previous one
                val afterState = game.getClientState(1)
                val manglerAfter = afterState.cards.values.find { it.name == "Riptide Mangler" }
                withClue("Riptide Mangler should be 1/3 after copying Fugitive Wizard's power") {
                    manglerAfter shouldNotBe null
                    manglerAfter!!.power shouldBe 1
                    manglerAfter.toughness shouldBe 3
                }
            }
        }
    }
}
