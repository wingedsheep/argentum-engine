package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Arcanis the Omnipotent.
 *
 * Card reference:
 * - Arcanis the Omnipotent ({3}{U}{U}{U}): Legendary Creature — Wizard 3/4
 *   "{T}: Draw three cards."
 *   "{2}{U}{U}: Return Arcanis the Omnipotent to its owner's hand."
 */
class ArcanisTheOmnipotentScenarioTest : ScenarioTestBase() {

    private fun addMana(game: TestGame, blue: Int = 0, colorless: Int = 0) {
        game.state = game.state.updateEntity(game.player1Id) { container ->
            container.with(ManaPoolComponent(blue = blue, colorless = colorless))
        }
    }

    init {
        context("Arcanis the Omnipotent - tap to draw three cards") {

            test("draws three cards when tap ability is activated") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Arcanis the Omnipotent")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Activate tap ability
                val arcanis = game.findPermanent("Arcanis the Omnipotent")!!
                val cardDef = cardRegistry.getCard("Arcanis the Omnipotent")!!
                val tapAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = arcanis,
                        abilityId = tapAbility.id
                    )
                )

                withClue("Tap ability should activate successfully") {
                    result.error shouldBe null
                }

                // Arcanis should be tapped
                withClue("Arcanis should be tapped") {
                    game.state.getEntity(arcanis)?.has<TappedComponent>() shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Should have drawn 3 cards
                withClue("Player should have drawn 3 cards") {
                    game.handSize(1) shouldBe initialHandSize + 3
                }
            }

            test("cannot activate tap ability when already tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Arcanis the Omnipotent", tapped = true)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val arcanis = game.findPermanent("Arcanis the Omnipotent")!!
                val cardDef = cardRegistry.getCard("Arcanis the Omnipotent")!!
                val tapAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = arcanis,
                        abilityId = tapAbility.id
                    )
                )

                withClue("Should not be able to activate when tapped") {
                    result.error shouldNotBe null
                }
            }

            test("cannot activate tap ability with summoning sickness") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Arcanis the Omnipotent", summoningSickness = true)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val arcanis = game.findPermanent("Arcanis the Omnipotent")!!
                val cardDef = cardRegistry.getCard("Arcanis the Omnipotent")!!
                val tapAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = arcanis,
                        abilityId = tapAbility.id
                    )
                )

                withClue("Should not be able to activate with summoning sickness") {
                    result.error shouldNotBe null
                }
            }
        }

        context("Arcanis the Omnipotent - return to hand ability") {

            test("returns to owner's hand when mana ability is activated") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Arcanis the Omnipotent")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                addMana(game, blue = 2, colorless = 2)

                val arcanis = game.findPermanent("Arcanis the Omnipotent")!!
                val cardDef = cardRegistry.getCard("Arcanis the Omnipotent")!!
                val bounceAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = arcanis,
                        abilityId = bounceAbility.id
                    )
                )

                withClue("Bounce ability should activate successfully") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Arcanis should no longer be on the battlefield
                withClue("Arcanis should not be on the battlefield") {
                    game.isOnBattlefield("Arcanis the Omnipotent") shouldBe false
                }

                // Arcanis should be in hand
                withClue("Arcanis should be in player's hand") {
                    game.isInHand(1, "Arcanis the Omnipotent") shouldBe true
                }
            }

            test("cannot activate bounce ability without enough mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Arcanis the Omnipotent")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Only 1 blue mana - not enough for {2}{U}{U}
                addMana(game, blue = 1, colorless = 0)

                val arcanis = game.findPermanent("Arcanis the Omnipotent")!!
                val cardDef = cardRegistry.getCard("Arcanis the Omnipotent")!!
                val bounceAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = arcanis,
                        abilityId = bounceAbility.id
                    )
                )

                withClue("Should not be able to activate without enough mana") {
                    result.error shouldNotBe null
                }
            }

            test("bounce ability does not require tapping") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Arcanis the Omnipotent", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                addMana(game, blue = 2, colorless = 2)

                val arcanis = game.findPermanent("Arcanis the Omnipotent")!!
                val cardDef = cardRegistry.getCard("Arcanis the Omnipotent")!!
                val bounceAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = arcanis,
                        abilityId = bounceAbility.id
                    )
                )

                withClue("Bounce ability should work even when tapped (no tap cost)") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Arcanis should be in hand") {
                    game.isInHand(1, "Arcanis the Omnipotent") shouldBe true
                }
            }
        }
    }
}
