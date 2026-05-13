package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Risky Research.
 *
 * Card reference:
 * - Risky Research ({2}{B}): Sorcery
 *   "Surveil 2, then draw two cards, then you lose 2 life."
 */
class RiskyResearchScenarioTest : ScenarioTestBase() {

    // Library (top → bottom): Forest (pos 1), Island (pos 2), Mountain (pos 3), Plains (pos 4)
    private fun ScenarioBuilder.withStandardLibrary(): ScenarioBuilder =
        withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Plains")

    init {
        context("Risky Research — Surveil 2, draw 2, lose 2 life") {

            test("mills both surveiled cards then draws the next two and loses 2 life") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Risky Research")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withStandardLibrary()
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Risky Research")
                withClue("Casting Risky Research should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Surveil 2: choose which of the top 2 library cards go to the graveyard
                withClue("Engine should present a Surveil selection decision") {
                    game.hasPendingDecision() shouldBe true
                }
                val surveilDecision = game.getPendingDecision()
                surveilDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Surveil 2 should offer exactly 2 cards") {
                    surveilDecision.options.size shouldBe 2
                }

                // Put both surveiled cards (Forest and Island) into the graveyard
                game.selectCards(surveilDecision.options)

                // When the entire surveiled collection is milled there are no cards left to
                // put back, so no ReorderLibraryDecision is presented.
                withClue("No pending decision after milling all surveiled cards") {
                    game.hasPendingDecision() shouldBe false
                }

                withClue("Risky Research should be in the caster's graveyard") {
                    game.isInGraveyard(1, "Risky Research") shouldBe true
                }
                withClue("Forest (surveiled — milled) should be in the graveyard") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                }
                withClue("Island (surveiled — milled) should be in the graveyard") {
                    game.isInGraveyard(1, "Island") shouldBe true
                }
                withClue("Mountain (library position 3) should be drawn into hand") {
                    game.isInHand(1, "Mountain") shouldBe true
                }
                withClue("Plains (library position 4) should be drawn into hand") {
                    game.isInHand(1, "Plains") shouldBe true
                }
                withClue("Hand size should be 2 after drawing two cards") {
                    game.handSize(1) shouldBe 2
                }
                withClue("Caster should lose exactly 2 life") {
                    game.getLifeTotal(1) shouldBe 18
                }
                withClue("Opponent's life total should be unchanged") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("keeping both surveiled cards on top means they are the two drawn cards") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Risky Research")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withStandardLibrary()
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Risky Research")
                game.resolveStack()

                // Surveil 2: keep both on top — submit empty selection
                val surveilDecision = game.getPendingDecision() as SelectCardsDecision
                game.selectCards(emptyList())

                // Engine must ask the player to arrange the two kept cards
                withClue("Engine should present a reorder decision for the two kept cards") {
                    game.hasPendingDecision() shouldBe true
                }
                val reorderDecision = game.getPendingDecision()
                reorderDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
                // Keep original order: Forest first (to be drawn first), Island second
                game.submitDecision(OrderedResponse(reorderDecision.id, reorderDecision.cards))

                withClue("Forest (kept on top, drawn first) should be in hand") {
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("Island (kept second, drawn second) should be in hand") {
                    game.isInHand(1, "Island") shouldBe true
                }
                withClue("Mountain (library position 3) should remain in library") {
                    game.isInHand(1, "Mountain") shouldBe false
                }
                withClue("Graveyard should contain only Risky Research from this resolution") {
                    game.isInGraveyard(1, "Risky Research") shouldBe true
                    game.isInGraveyard(1, "Forest") shouldBe false
                    game.isInGraveyard(1, "Island") shouldBe false
                }
                withClue("Caster should lose exactly 2 life") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("milling one surveiled card and keeping one draws the kept card then the next library card") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Risky Research")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withStandardLibrary()
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Risky Research")
                game.resolveStack()

                // Surveil 2: mill Forest (options[0] = top card), keep Island (options[1])
                val surveilDecision = game.getPendingDecision() as SelectCardsDecision
                val forestId = surveilDecision.options[0]
                game.selectCards(listOf(forestId))

                // One card remains to be placed back on top — engine asks for ordering
                withClue("Engine should present a reorder decision for the one kept card") {
                    game.hasPendingDecision() shouldBe true
                }
                val reorderDecision = game.getPendingDecision() as ReorderLibraryDecision
                game.submitDecision(OrderedResponse(reorderDecision.id, reorderDecision.cards))

                withClue("Forest (milled) should be in the graveyard") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                }
                withClue("Island (kept on top, drawn first) should be in hand") {
                    game.isInHand(1, "Island") shouldBe true
                }
                withClue("Mountain (library position 3, drawn second) should be in hand") {
                    game.isInHand(1, "Mountain") shouldBe true
                }
                withClue("Plains (library position 4) should remain in library") {
                    game.isInHand(1, "Plains") shouldBe false
                }
                withClue("Caster should lose exactly 2 life") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }
        }
    }
}
