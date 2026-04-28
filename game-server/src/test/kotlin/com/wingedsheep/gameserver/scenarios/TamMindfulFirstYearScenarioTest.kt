package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tam, Mindful First-Year (ECL).
 *
 * {1}{G/U} Legendary Creature — Gorgon Wizard, 2/2.
 *
 * - Each other creature you control has hexproof from each of its colors.
 * - {T}: Target creature you control becomes all colors until end of turn.
 *
 * Per the printed ruling, "hexproof from each color" expands to one hexproof keyword
 * per color the creature currently has — colorless creatures get nothing.
 */
class TamMindfulFirstYearScenarioTest : ScenarioTestBase() {

    private val colorlessGolem = CardDefinition.creature(
        name = "Test Colorless Golem",
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype("Golem")),
        power = 2, toughness = 2
    )

    init {
        cardRegistry.register(colorlessGolem)

        context("Static — hexproof from each of its colors") {

            test("a green creature you control gains HEXPROOF_FROM_GREEN but not other colors") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tam, Mindful First-Year")
                    .withCardOnBattlefield(1, "Grizzly Bears") // green 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Green creature should have hexproof from green") {
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_GREEN") shouldBe true
                }
                withClue("Green creature should NOT have hexproof from other colors") {
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_WHITE") shouldBe false
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_BLUE") shouldBe false
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_BLACK") shouldBe false
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_RED") shouldBe false
                }
            }

            test("Tam itself does not gain hexproof from its own colors (excludeSelf)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tam, Mindful First-Year")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamId = game.findPermanent("Tam, Mindful First-Year")!!
                val projected = game.state.projectedState

                withClue("Tam shouldn't grant itself hexproof — it says 'each other creature'") {
                    projected.hasKeyword(tamId, "HEXPROOF_FROM_GREEN") shouldBe false
                    projected.hasKeyword(tamId, "HEXPROOF_FROM_BLUE") shouldBe false
                }
            }

            test("opponent's creature doesn't gain hexproof from Tam") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tam, Mindful First-Year")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentBears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Opponent's creature must not benefit from Tam's static") {
                    projected.hasKeyword(opponentBears, "HEXPROOF_FROM_GREEN") shouldBe false
                }
            }

            test("colorless creature you control gains no hexproof at all") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tam, Mindful First-Year")
                    .withCardOnBattlefield(1, "Test Colorless Golem")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val walkerId = game.findPermanent("Test Colorless Golem")!!
                val projected = game.state.projectedState

                withClue("Colorless creature should not pick up any HEXPROOF_FROM_<color>") {
                    projected.hasKeyword(walkerId, "HEXPROOF_FROM_WHITE") shouldBe false
                    projected.hasKeyword(walkerId, "HEXPROOF_FROM_BLUE") shouldBe false
                    projected.hasKeyword(walkerId, "HEXPROOF_FROM_BLACK") shouldBe false
                    projected.hasKeyword(walkerId, "HEXPROOF_FROM_RED") shouldBe false
                    projected.hasKeyword(walkerId, "HEXPROOF_FROM_GREEN") shouldBe false
                }
            }
        }

        context("Activated — target creature you control becomes all colors") {

            test("activating Tam makes the target gain all five colors and HEXPROOF_FROM_<each>") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tam, Mindful First-Year")
                    .withCardOnBattlefield(1, "Grizzly Bears") // green 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamId = game.findPermanent("Tam, Mindful First-Year")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val tamCardDef = cardRegistry.getCard("Tam, Mindful First-Year")!!
                val activatedAbility = tamCardDef.script.activatedAbilities.single()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tamId,
                        abilityId = activatedAbility.id,
                        targets = listOf(ChosenTarget.Permanent(bearsId))
                    )
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val projected = game.state.projectedState
                val bearsColors = projected.getColors(bearsId)
                withClue("Bears should now be all five colors, got $bearsColors") {
                    bearsColors shouldBe setOf("WHITE", "BLUE", "BLACK", "RED", "GREEN")
                }

                withClue("Hexproof from each color should now apply") {
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_WHITE") shouldBe true
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_BLUE") shouldBe true
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_BLACK") shouldBe true
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_RED") shouldBe true
                    projected.hasKeyword(bearsId, "HEXPROOF_FROM_GREEN") shouldBe true
                }
            }
        }
    }
}
