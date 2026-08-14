package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Planetarium of Wan Shi Tong (TLA #259) — "Whenever you scry or surveil, look at the top card of
 * your library. You may cast that card without paying its mana cost. **Do this only once each
 * turn.**"
 *
 * The rider is `effectOncePerTurn` (CR 603.2h), and this card is the reason the engine's lowering
 * has to look past the top of the effect tree. Its effect is
 * `Composite(look at top card, May(cast it))`, and the ruling pins the turn's single use to the
 * **cast**, not to the look:
 *
 * > Once you choose to cast the top card of your library, Planetarium of Wan Shi Tong's ability
 * > won't trigger again that turn.
 *
 * So the spending gate belongs *inside* the consent gate at the tail of that composite, not around
 * the whole thing: every scry or surveil keeps showing you the top card until you actually cast
 * one. If the budget were spent by the look, the first scry of the turn would be the only one —
 * which is exactly what the old `oncePerTurn` modelling did.
 */
class PlanetariumOfWanShiTongScenarioTest : ScenarioTestBase() {

    init {
        val scrySpell = card("Test Scry Spell") {
            manaCost = "{U}"
            typeLine = "Instant"
            oracleText = "Scry 1."
            spell { effect = Effects.Scry(1) }
        }
        cardRegistry.register(listOf(scrySpell))

        /** Resolve a scry's "which cards to the bottom" + "reorder the top" prompts. */
        fun TestGame.answerScry() {
            skipSelection()
            keepLibraryOrder()
        }

        fun newGame() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Planetarium of Wan Shi Tong")
            .withCardsInHand(1, "Test Scry Spell", 2)
            .withLandsOnBattlefield(1, "Island", 4)
            .withCardInLibrary(1, "Grizzly Bears") // top of the library — what the Planetarium shows
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("declining the free cast does not spend the turn — a later scry still offers it") {
            val game = newGame()

            // First scry: the Planetarium looks and offers the free cast. Decline it.
            game.castSpell(1, "Test Scry Spell").error shouldBe null
            game.resolveStack()
            game.answerScry()
            game.resolveStack()
            withClue("the first scry offers the free cast") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(false)
            game.resolveStack()
            withClue("declining cast nothing") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            // Second scry the same turn: looking is not casting, so the ability triggers again.
            game.castSpell(1, "Test Scry Spell").error shouldBe null
            game.resolveStack()
            game.answerScry()
            game.resolveStack()
            withClue("the ability triggers again after a decline") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true)
            game.resolveStack()
            withClue("accepting cast the top card for free") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("casting spends the turn — a later scry offers nothing") {
            val game = newGame()

            game.castSpell(1, "Test Scry Spell").error shouldBe null
            game.resolveStack()
            game.answerScry()
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()
            withClue("the first offer was taken") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            // "Once you choose to cast the top card of your library, the ability won't trigger
            // again that turn" — not even to look.
            game.castSpell(1, "Test Scry Spell").error shouldBe null
            game.resolveStack()
            game.answerScry()
            game.resolveStack()
            withClue("the cast was taken this turn, so the ability does not trigger again") {
                game.hasPendingDecision() shouldBe false
            }
        }
    }
}
