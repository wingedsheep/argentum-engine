package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Saruman of Many Colors (LTR #223) — {3}{W}{U}{B} Legendary Avatar Wizard 5/4.
 *
 *   Ward—Discard an enchantment, instant, or sorcery card.
 *   Whenever you cast your second spell each turn, each opponent mills two cards. When one or
 *   more cards are milled this way, exile target enchantment, instant, or sorcery card with equal
 *   or lesser mana value than that spell from an opponent's graveyard. Copy the exiled card. You
 *   may cast the copy without paying its mana cost.
 *
 * Proves:
 *  - Casting your SECOND spell each turn mills two cards from each opponent.
 *  - The reflexive trigger then lets you exile a legal enchantment/instant/sorcery card from an
 *    opponent's graveyard, copy it, and cast the copy for free (here a copied Divination draws
 *    two cards for Saruman's controller).
 *  - A graveyard card with mana value GREATER than the triggering second spell is NOT a legal
 *    target (Divination MV 3 is legal under an MV-3 second spell; Lórien Revealed MV 5 is not).
 */
class SarumanOfManyColorsScenarioTest : ScenarioTestBase() {

    init {
        context("Saruman of Many Colors — second-spell mill + reflexive exile/copy/cast") {

            test("second spell mills two from each opponent; exile, copy, and cast a legal graveyard card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Saruman of Many Colors")
                    // Two no-target {3} creature spells (MV 3) — the second one triggers Saruman.
                    .withCardsInHand(1, "Palladium Myr", 2)
                    .withLandsOnBattlefield(1, "Island", 8)
                    // Opponent has a library to mill and a legal graveyard target (Divination, MV 3).
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Centaur Courser")
                    .withCardInLibrary(2, "Forest")
                    .withCardInGraveyard(2, "Divination")
                    // Saruman's controller needs cards to draw from the copied Divination (draw 2).
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentGraveyardBefore = game.graveyardSize(2)
                val opponentLibraryBefore = game.librarySize(2)
                val controllerHandBefore = game.handSize(1)
                val targetDivination = graveyardCardId(game, 2, "Divination")

                // First spell of the turn (no second-spell trigger yet).
                game.castSpell(1, "Palladium Myr")
                game.resolveStack()

                // Second spell — fires Saruman's "whenever you cast your second spell each turn".
                game.castSpell(1, "Palladium Myr")
                game.resolveStack()

                // Trigger resolves: each opponent mills two; then the reflexive targets a graveyard
                // enchantment/instant/sorcery with MV <= the second Divination (MV 3).
                withClue("Each opponent milled two cards (library -2)") {
                    game.librarySize(2) shouldBe (opponentLibraryBefore - 2)
                }
                withClue("...into their graveyard (the milled cards + the pre-existing Divination)") {
                    game.graveyardSize(2) shouldBe (opponentGraveyardBefore + 2)
                }

                // Choose the legal target — the opponent's Divination (MV 3 <= 3).
                game.hasPendingDecision() shouldBe true
                game.selectTargets(listOf(targetDivination))

                // The reflexive ability is still on the stack; resolving it exiles the targeted
                // Divination, copies it, and prompts "you may cast the copy" — accept the copy.
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                // The copied Divination has no target — resolve it (draws two for the controller).
                game.resolveStack()

                withClue("The targeted Divination was exiled from the opponent's graveyard") {
                    game.isInGraveyard(2, "Divination") shouldBe false
                }
                withClue("Casting the free Divination copy drew Saruman's controller two cards") {
                    // Hand: cast both Myrs (-2), the copied Divination draws 2 (+2).
                    game.handSize(1) shouldBe (controllerHandBefore - 2 + 2)
                }
            }

            test("a graveyard card with mana value greater than the second spell is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Saruman of Many Colors")
                    .withCardsInHand(1, "Palladium Myr", 2)
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Centaur Courser")
                    .withCardInLibrary(2, "Forest")
                    // Legal target (MV 3) and an illegal one (Lórien Revealed, MV 5 > 3).
                    .withCardInGraveyard(2, "Divination")
                    .withCardInGraveyard(2, "Lórien Revealed")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val legalDivination = graveyardCardId(game, 2, "Divination")
                val illegalLorien = graveyardCardId(game, 2, "Lórien Revealed")

                game.castSpell(1, "Palladium Myr")
                game.resolveStack()
                game.castSpell(1, "Palladium Myr")
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                val decision = game.getPendingDecision()
                (decision is ChooseTargetsDecision) shouldBe true
                val legal = (decision as ChooseTargetsDecision).legalTargets[0] ?: emptyList()

                withClue("Divination (MV 3 <= 3) is a legal target") {
                    legal.contains(legalDivination) shouldBe true
                }
                withClue("Lórien Revealed (MV 5 > 3) is NOT a legal target") {
                    legal.contains(illegalLorien) shouldBe false
                }
            }
        }
    }

    private fun graveyardCardId(game: TestGame, playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getGraveyard(playerId).first { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }
}
