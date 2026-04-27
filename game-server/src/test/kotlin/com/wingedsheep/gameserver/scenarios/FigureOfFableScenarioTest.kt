package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Figure of Fable.
 *
 * {G/W} 1/1 Kithkin with three permanent activated abilities that transform it
 * into successively bigger creatures: Scout 2/3, Soldier 4/5, Avatar 7/8 with
 * protection from each opponent.
 */
class FigureOfFableScenarioTest : ScenarioTestBase() {

    private fun activateAbility(game: TestGame, abilityIndex: Int) {
        val sourceId = game.findPermanent("Figure of Fable")!!
        val cardDef = cardRegistry.getCard("Figure of Fable")!!
        val ability = cardDef.script.activatedAbilities[abilityIndex]
        val result = game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = sourceId,
                abilityId = ability.id
            )
        )
        withClue("Activation should succeed: ${result.error}") {
            result.error shouldBe null
        }
    }

    private fun setMana(game: TestGame, white: Int = 0, green: Int = 0, colorless: Int = 0) {
        game.state = game.state.updateEntity(game.player1Id) { container ->
            container.with(ManaPoolComponent(white = white, green = green, colorless = colorless))
        }
    }

    init {
        context("Figure of Fable transformation chain") {

            test("first ability turns it into a 2/3 Kithkin Scout") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                projected.getPower(sourceId) shouldBe 2
                projected.getToughness(sourceId) shouldBe 3
                withClue("Should have Scout subtype") {
                    ("Scout" in projected.getSubtypes(sourceId)) shouldBe true
                }
                withClue("Should still be a Kithkin") {
                    ("Kithkin" in projected.getSubtypes(sourceId)) shouldBe true
                }
            }

            test("second ability does nothing if not yet a Scout (Scryfall ruling)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                withClue("Should remain 1/1 — no Scout, condition fails") {
                    projected.getPower(sourceId) shouldBe 1
                    projected.getToughness(sourceId) shouldBe 1
                }
                withClue("Should not be a Soldier") {
                    ("Soldier" in projected.getSubtypes(sourceId)) shouldBe false
                }
            }

            test("Scout becomes a 4/5 Kithkin Soldier") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                projected.getPower(sourceId) shouldBe 4
                projected.getToughness(sourceId) shouldBe 5
                ("Soldier" in projected.getSubtypes(sourceId)) shouldBe true
                withClue("Scout subtype is replaced") {
                    ("Scout" in projected.getSubtypes(sourceId)) shouldBe false
                }
            }

            test("Soldier becomes a 7/8 Kithkin Avatar with protection from each opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                setMana(game, green = 3, colorless = 3)
                activateAbility(game, 2)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                projected.getPower(sourceId) shouldBe 7
                projected.getToughness(sourceId) shouldBe 8
                ("Avatar" in projected.getSubtypes(sourceId)) shouldBe true
                withClue("Should have protection from each opponent") {
                    projected.hasKeyword(sourceId, "PROTECTION_FROM_EACH_OPPONENT") shouldBe true
                }
            }
        }
    }
}
