package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Scout the City (SPM #113).
 *
 * {1}{G} Sorcery. Choose one —
 *   • Look Around — Mill three cards. You may put a permanent card from among them into
 *     your hand. You gain 3 life.
 *   • Bring Down — Destroy target creature with flying.
 */
class ScoutTheCityScenarioTest : ScenarioTestBase() {

    init {
        context("Scout the City — choose one") {

            test("Look Around: mill three, put a permanent into hand, gain 3 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Scout the City")
                    .withLandsOnBattlefield(1, "Forest", 2) // {1}{G}
                    // Top three of library (first added = top): creature, instant, land.
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                val bear = game.findCardsInLibrary(1, "Grizzly Bears").first()
                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").first()
                val mountain = game.findCardsInLibrary(1, "Mountain").first()

                game.castSpellWithMode(1, "Scout the City", modeIndex = 0).error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision
                withClue("A selection decision should be presented after milling") {
                    (decision != null) shouldBe true
                }
                val select = decision as SelectCardsDecision

                // "You may" → 0..1 selections.
                select.minSelections shouldBe 0
                select.maxSelections shouldBe 1

                withClue("The three milled cards are now in the graveyard") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 1
                    game.findCardsInGraveyard(1, "Mountain").size shouldBe 1
                }

                // Only permanent cards (creature + land) are selectable; the instant is not.
                select.options shouldContainExactlyInAnyOrder listOf(bear, mountain)
                select.nonSelectableOptions shouldContainExactlyInAnyOrder listOf(bolt)

                game.selectCards(listOf(bear))
                game.resolveStack()

                withClue("The chosen permanent goes to hand; the rest stay in the graveyard") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 1
                    game.findCardsInGraveyard(1, "Mountain").size shouldBe 1
                }
                withClue("You gain 3 life") {
                    game.getLifeTotal(1) shouldBe startingLife + 3
                }
            }

            test("Look Around: declining the permanent still mills and gains 3 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Scout the City")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                game.castSpellWithMode(1, "Scout the City", modeIndex = 0).error shouldBe null
                game.resolveStack()

                val select = game.state.pendingDecision as SelectCardsDecision
                select.minSelections shouldBe 0

                // Decline the optional pick.
                game.selectCards(emptyList())
                game.resolveStack()

                withClue("Nothing went to hand; all three milled cards stay in the graveyard") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 1
                    game.findCardsInGraveyard(1, "Mountain").size shouldBe 1
                }
                withClue("You still gain 3 life") {
                    game.getLifeTotal(1) shouldBe startingLife + 3
                }
            }

            test("Bring Down: destroy target creature with flying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Scout the City")
                    .withCardOnBattlefield(2, "Storm Crow") // 1/2 flier
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crow = game.findPermanent("Storm Crow")!!
                game.castSpellWithMode(1, "Scout the City", modeIndex = 1, targetId = crow).error shouldBe null
                game.resolveStack()

                withClue("Storm Crow (flying) was destroyed") {
                    game.findPermanent("Storm Crow") shouldBe null
                    game.isInGraveyard(2, "Storm Crow") shouldBe true
                }
            }

            test("Bring Down: a non-flying creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Scout the City")
                    .withCardOnBattlefield(2, "Grizzly Bears") // no flying — illegal
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.castSpellWithMode(1, "Scout the City", modeIndex = 1, targetId = bears)

                withClue("Grizzly Bears (no flying) is not a legal target for Bring Down") {
                    (result.error != null) shouldBe true
                }
            }
        }
    }
}
