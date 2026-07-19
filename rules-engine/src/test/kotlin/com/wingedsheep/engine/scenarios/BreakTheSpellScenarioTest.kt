package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Break the Spell — {W} Instant
 * "Destroy target enchantment. If a permanent you controlled or a token was destroyed this way,
 * draw a card."
 *
 * Covers all three branches of the conditional draw:
 *  - an opponent's ordinary enchantment → destroyed, no draw;
 *  - your own enchantment → destroyed, draw;
 *  - an opponent's enchantment *token* → destroyed, draw (the token clause is controller-agnostic).
 *
 * "Food Fight" is used as the target throughout: a plain WOE enchantment with only a static
 * ability, so destroying it produces no triggers that could muddy the hand-size assertions.
 */
class BreakTheSpellScenarioTest : ScenarioTestBase() {

    init {
        test("an opponent's ordinary enchantment is destroyed with no draw") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Break the Spell")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Food Fight")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val enchantment = game.findPermanent("Food Fight")!!
            game.castSpell(1, "Break the Spell", targetId = enchantment).error shouldBe null
            game.resolveStack()

            withClue("the enchantment is destroyed regardless of who controlled it") {
                game.isInGraveyard(2, "Food Fight") shouldBe true
                game.findPermanent("Food Fight") shouldBe null
            }
            withClue("it was neither yours nor a token, so no card is drawn") {
                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 1
            }
        }

        test("destroying an enchantment you control draws a card") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Break the Spell")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Food Fight")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val enchantment = game.findPermanent("Food Fight")!!
            game.castSpell(1, "Break the Spell", targetId = enchantment).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Food Fight") shouldBe true
            withClue("a permanent you controlled was destroyed this way → draw a card") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.librarySize(1) shouldBe 0
            }
        }

        test("destroying an opponent's enchantment token draws a card") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Break the Spell")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Food Fight", isToken = true)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val token = game.findPermanent("Food Fight")
            withClue("the token target should be on the battlefield") { token shouldNotBe null }

            game.castSpell(1, "Break the Spell", targetId = token!!).error shouldBe null
            game.resolveStack()

            withClue("the destroyed token ceases to exist") {
                game.findPermanent("Food Fight") shouldBe null
            }
            withClue("a token was destroyed this way → draw a card even though it wasn't yours") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.librarySize(1) shouldBe 0
            }
        }
    }
}
