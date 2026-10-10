package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Counterbalance — {U}{U} Enchantment (Coldsnap #31)
 *
 * "Whenever an opponent casts a spell, you may reveal the top card of your library. If you do,
 *  counter that spell if it has the same mana value as the revealed card."
 *
 * Covers the matching reveal, a mismatched reveal, a declined reveal, and the empty-library case
 * where nothing is revealed — a mana-value-0 spell must not be countered by "nothing".
 */
class CounterbalanceScenarioTest : ScenarioTestBase() {

    private fun game(libraryCard: String?, spell: String, land: String, lands: Int): TestGame {
        val builder = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Counterbalance")
            .withCardInHand(2, spell)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (lands > 0) builder.withLandsOnBattlefield(2, land, lands)
        if (libraryCard != null) builder.withCardInLibrary(1, libraryCard)
        return builder.build()
    }

    init {
        test("revealing a card with the same mana value counters the spell") {
            val game = game("Grizzly Bears", "Grizzly Bears", "Forest", 2)

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.answerYesNo(true)
            game.resolveStack()

            withClue("the opponent's Grizzly Bears (MV 2) is countered") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
            withClue("the revealed card stays on top of the library") {
                game.librarySize(1) shouldBe 1
            }
        }

        test("a revealed card with a different mana value does not counter") {
            val game = game("Lightning Bolt", "Grizzly Bears", "Forest", 2)

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("declining to reveal lets the spell resolve") {
            val game = game("Grizzly Bears", "Grizzly Bears", "Forest", 2)

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("with an empty library nothing is revealed, so a mana-value-0 spell is not countered") {
            val game = game(null, "Ornithopter", "Forest", 0)

            game.castSpell(2, "Ornithopter").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(true)
            game.resolveStack()

            game.isOnBattlefield("Ornithopter") shouldBe true
        }
    }
}
