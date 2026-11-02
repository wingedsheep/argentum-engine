package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.hidden.OpponentModel
import com.wingedsheep.ai.engine.knowledge.UnseenThreats
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Removal held for the creatures the opponent has not drawn yet, read off their decklist
 * ([UnseenThreats], [AiProfile.holdRemovalForUnseenThreats]).
 *
 * The production game (2026-11-02, game `7988f9c0`, turn 7): the AI's only Refute Destiny exiled a
 * 2/3 Greenhouse Propagator — a mana creature — from an opponent whose deck holds Ruric Thar, two
 * Titanbones, Hexhaven Invigorator and Craftwork Crusher. Their hand already held Titanbones and
 * Ruric Thar. The AI knows that list in live games (by design), and now prices it.
 */
class RemovalForUnseenThreatsTest : ScenarioTestBase() {

    /** The opponent's registered deck in that game (40 cards). */
    private val greenBombs = mapOf(
        "Hexhaven Invigorator" to 1, "Simulacrum Shaper" to 1, "Heartwood Crafter" to 1,
        "Yoshimaru, Scrappy Stray" to 1, "Titanbones, Towering Heart" to 2, "Way of the Wildspeaker" to 1,
        "Edgar, Moonlit Sovereign" to 1, "Kiora of Fire and Ashes" to 1, "Craftwork Crusher" to 1,
        "Ruric Thar, Magecrusher" to 1, "Konstrari Improviser" to 2, "Something Worth Saving" to 3,
        "Greenhouse Propagator" to 1, "Inspired Tethermage" to 2, "Wrecking Gecko" to 1,
        "Bestial Incursion" to 1, "Vinelasher Adept" to 1, "Sureshot Sower" to 1, "Room of Refuge" to 1,
        "Mountain" to 4, "Forest" to 12,
    )

    /** A deck whose creatures are all no better than the Propagator: nothing to wait for. */
    private val smallBall = mapOf(
        "Greenhouse Propagator" to 4, "Grizzly Bears" to 8, "Llanowar Elves" to 6, "Forest" to 17,
    )

    /**
     * The 7988f9c0 turn-7 position: the AI with Refute Destiny ("exile target green or blue
     * creature or planeswalker") and its Danitha, facing a lone Greenhouse Propagator behind five
     * cards in hand. What those cards are does not matter: the AI cannot see them, and its search
     * samples them from the decklist.
     */
    private fun position(aiLife: Int = 20, theirGraveyard: List<String> = emptyList()): TestGame = scenario().withPlayers()
        .apply { theirGraveyard.forEach { withCardInGraveyard(2, it) } }
        .withTurnNumber(7)
        .withActivePlayer(1)
        .withLifeTotal(1, aiLife)
        .withLifeTotal(2, 21)
        .withLandsOnBattlefield(1, "Plains", 4)
        .withCardInHand(1, "Refute Destiny")
        .withCardOnBattlefield(1, "Danitha, Sword of Hope")
        .withLandsOnBattlefield(2, "Forest", 3)
        .withCardOnBattlefield(2, "Greenhouse Propagator")
        .withCardInHand(2, "Something Worth Saving")
        .withCardInHand(2, "Way of the Wildspeaker")
        .withCardInHand(2, "Konstrari Improviser")
        .withCardInHand(2, "Inspired Tethermage")
        .withCardInHand(2, "Forest")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Forest")
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    /** What the LIVE AI does first, given [decklist] for the opponent (or no list at all). */
    private fun TestGame.firstMove(decklist: Map<String, Int>?): String {
        val models = decklist?.let { mapOf(player2Id to OpponentModel.KnownDecklist(it)) }.orEmpty()
        val action = AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE, opponentModels = models)
            .chooseAction(state)
        val cast = action as? CastSpell ?: return "no cast: ${action::class.simpleName}"
        val target = (cast.targets.singleOrNull() as? ChosenTarget.Permanent)?.entityId
        val name = state.getEntity(cast.cardId)?.get<CardComponent>()?.name
        return "$name -> ${target?.let { state.getEntity(it)?.get<CardComponent>()?.name }}"
    }

    private val spent = "Refute Destiny -> Greenhouse Propagator"

    init {
        test("the chance a threat is in hand or the next draws is hypergeometric") {
            UnseenThreats.noneDrawn(pool = 10, hits = 1, draws = 1) shouldBe (0.9).plusOrMinus(1e-9)
            UnseenThreats.noneDrawn(pool = 10, hits = 2, draws = 2) shouldBe (8.0 / 10 * 7.0 / 9).plusOrMinus(1e-9)
            UnseenThreats.noneDrawn(pool = 3, hits = 2, draws = 2) shouldBe 0.0
        }

        // The misplay: without the decklist the Propagator clears Refute Destiny's two-mana bar.
        test("without the opponent's decklist the AI exiles the 2/3 mana creature") {
            position().firstMove(decklist = null) shouldBe spent
        }

        test("knowing the deck holds Ruric Thar, Titanbones and Invigorator, it keeps its only answer") {
            val move = position().firstMove(greenBombs)
            withClue("first move with the decklist known: $move") {
                move shouldNotBe spent
            }
        }

        // Control: the list holds nothing better than what is on the table — nothing to wait for.
        test("against a deck of small creatures the removal is still spent") {
            position().firstMove(smallBall) shouldBe spent
        }

        // Control: a creature hurting us now is answered now. At 8 life the Propagator's two power
        // kills in four attacks, so the bombs still in their deck no longer hold the removal.
        test("when the target is hurting us the removal is spent whatever the deck holds") {
            position(aiLife = 8).firstMove(greenBombs) shouldBe spent
        }

        // Control: the bar falls as the threats show up. With every creature that beats the
        // Propagator already in their graveyard, nothing is left to wait for.
        test("once the bombs have been seen the removal is spent") {
            val seen = listOf(
                "Hexhaven Invigorator", "Titanbones, Towering Heart", "Titanbones, Towering Heart",
                "Edgar, Moonlit Sovereign", "Craftwork Crusher", "Ruric Thar, Magecrusher", "Wrecking Gecko",
            )
            val game = position(theirGraveyard = seen)
            game.firstMove(greenBombs) shouldBe spent
        }
    }
}
