package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Transmute Artifact — "Sacrifice an artifact. If you do, search your library for an artifact card.
 * If that card's mana value is ≤ the sacrificed artifact's mana value, put it onto the battlefield.
 * If it's greater, you may pay {X}, where X is the difference. If you do, put it onto the
 * battlefield. If you don't, put it into its owner's graveyard. Then shuffle."
 *
 * Each test gives the caster a single artifact, so the "sacrifice an artifact" selection
 * auto-resolves (one eligible) and the first decision presented is the library search. Covers all
 * three branches of the mana-value comparison:
 *  - found MV ≤ sacrificed MV → free onto the battlefield,
 *  - found MV > sacrificed MV, pay the difference → onto the battlefield,
 *  - found MV > sacrificed MV, decline → into the graveyard.
 */
class TransmuteArtifactScenarioTest : ScenarioTestBase() {

    /** The library card offered in the current search decision, chosen by name. */
    private fun searchPick(game: TestGame, name: String): EntityId {
        val decision = game.getPendingDecision() as SelectCardsDecision
        return decision.cardInfo!!.entries.first { it.value.name == name }.key
    }

    init {
        context("Transmute Artifact mana-value comparison branches") {

            test("found mana value <= sacrificed: put onto the battlefield for free") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Transmute Artifact")
                    .withCardOnBattlefield(1, "Triskelion", summoningSickness = false) // MV 6 (sacrificed)
                    .withCardInLibrary(1, "Ornithopter") // MV 0 (found)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Transmute Artifact")
                game.resolveStack()

                // The single artifact (Triskelion) was auto-sacrificed; the first decision is the search.
                withClue("Triskelion is sacrificed") { game.isInGraveyard(1, "Triskelion") shouldBe true }
                game.selectCards(listOf(searchPick(game, "Ornithopter")))

                withClue("Ornithopter (MV 0 ≤ 6) enters the battlefield free") {
                    game.isOnBattlefield("Ornithopter") shouldBe true
                }
                withClue("No pending pay decision (free branch)") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("found mana value > sacrificed, pay the difference: put onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Transmute Artifact")
                    .withCardOnBattlefield(1, "Ornithopter", summoningSickness = false) // MV 0 (sacrificed)
                    .withCardInLibrary(1, "Triskelion") // MV 6 (found) → difference 6
                    .withLandsOnBattlefield(1, "Island", 8) // enough to pay {6}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Transmute Artifact")
                game.resolveStack()

                withClue("Ornithopter is sacrificed") { game.isInGraveyard(1, "Ornithopter") shouldBe true }
                game.selectCards(listOf(searchPick(game, "Triskelion")))

                // MV 6 > 0 → may pay {6}; pay it.
                withClue("A pay decision should be pending (pay-the-difference branch)") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                withClue("Triskelion enters the battlefield after paying the difference") {
                    game.isOnBattlefield("Triskelion") shouldBe true
                }
            }

            test("found mana value > sacrificed, decline payment: put into the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Transmute Artifact")
                    .withCardOnBattlefield(1, "Ornithopter", summoningSickness = false) // MV 0 (sacrificed)
                    .withCardInLibrary(1, "Triskelion") // MV 6 (found)
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Transmute Artifact")
                game.resolveStack()

                game.selectCards(listOf(searchPick(game, "Triskelion")))

                withClue("A pay decision should be pending") { game.hasPendingDecision() shouldBe true }
                game.answerYesNo(false) // decline

                withClue("Declined → Triskelion goes to the graveyard, not the battlefield") {
                    game.isOnBattlefield("Triskelion") shouldBe false
                    game.isInGraveyard(1, "Triskelion") shouldBe true
                }
            }
        }
    }
}
