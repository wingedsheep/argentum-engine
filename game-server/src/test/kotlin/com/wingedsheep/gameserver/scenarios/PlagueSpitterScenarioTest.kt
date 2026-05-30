package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Plague Spitter.
 *
 * {2}{B} Creature — Phyrexian Horror 2/2
 * "At the beginning of your upkeep, this creature deals 1 damage to each creature and each player."
 * "When this creature dies, it deals 1 damage to each creature and each player."
 */
class PlagueSpitterScenarioTest : ScenarioTestBase() {

    private val xenobear = CardDefinition.creature(
        name = "Test 1/1", manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Bear")), power = 1, toughness = 1
    )
    private val toughBear = CardDefinition.creature(
        name = "Test 2/2", manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Bear")), power = 2, toughness = 2
    )

    init {
        cardRegistry.register(xenobear)
        cardRegistry.register(toughBear)

        context("Plague Spitter upkeep trigger") {
            test("deals 1 damage to each creature and each player at upkeep") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Plague Spitter")
                    .withCardOnBattlefield(2, "Test 1/1")
                    .withCardOnBattlefield(2, "Test 2/2")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Both players take 1 damage from the upkeep trigger") {
                    game.getLifeTotal(1) shouldBe 19
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("The 1/1 dies to 1 damage") {
                    game.isOnBattlefield("Test 1/1") shouldBe false
                }
                withClue("The 2/2 survives 1 damage") {
                    game.isOnBattlefield("Test 2/2") shouldBe true
                }
                withClue("Plague Spitter itself (2/2) survives 1 damage") {
                    game.isOnBattlefield("Plague Spitter") shouldBe true
                }
            }
        }

        context("Plague Spitter dies trigger") {
            test("deals 1 damage to each creature and each player when it dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Smother")
                    .withCardOnBattlefield(1, "Plague Spitter")
                    .withCardOnBattlefield(2, "Test 1/1")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spitter = game.findPermanent("Plague Spitter")!!

                // Destroy Plague Spitter with Smother; its dies trigger then deals 1 to all.
                game.castSpell(1, "Smother", spitter)
                game.resolveStack()

                withClue("Plague Spitter is dead") {
                    game.isOnBattlefield("Plague Spitter") shouldBe false
                }
                withClue("Both players take 1 damage from the dies trigger") {
                    game.getLifeTotal(1) shouldBe 19
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("The opposing 1/1 dies to the dies trigger's 1 damage") {
                    game.isOnBattlefield("Test 1/1") shouldBe false
                }
            }
        }
    }
}
