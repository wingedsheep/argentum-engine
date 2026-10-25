package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Abundance (USG #229) — "If you would draw a card, you may instead choose land or nonland and
 * reveal cards from the top of your library until you reveal a card of the chosen kind. Put that
 * card into your hand and put all other cards revealed this way on the bottom of your library in
 * any order."
 *
 * The optional draw replacement wraps a resolution-time land/nonland choice and a reveal-until, so
 * these tests pin the decision flow: yes/no → choose kind → reorder the bottomed cards, once per
 * replaced draw, with a declined draw falling through to a normal draw.
 */
class AbundanceScenarioTest : ScenarioTestBase() {

    /** Library is seeded top-down in call order. Counsel of the Soratami draws two. */
    private fun abundance(vararg library: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Abundance")
        .withCardInHand(1, "Counsel of the Soratami")
        .withLandsOnBattlefield(1, "Island", 3)
        .apply { library.forEach { withCardInLibrary(1, it) } }
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.chooseKind(label: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(label))).error shouldBe null
    }

    private fun ScenarioTestBase.TestGame.keepOrderIfAsked() {
        if (getPendingDecision() is ReorderLibraryDecision) keepLibraryOrder()
    }

    init {
        context("Abundance") {

            test("each draw is replaced separately; a declined draw is a normal draw") {
                val game = abundance("Forest", "Mountain", "Grizzly Bears", "Hill Giant", "Plains")

                game.castSpell(1, "Counsel of the Soratami").error shouldBe null
                game.resolveStack()

                // First draw: replace it, choosing nonland.
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.chooseKind("Nonland")
                game.keepOrderIfAsked()

                withClue("the first nonland card revealed goes to hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("the lands revealed above it are not put into hand") {
                    game.isInHand(1, "Forest") shouldBe false
                    game.isInHand(1, "Mountain") shouldBe false
                }

                // Second draw: decline — Hill Giant is now on top and is drawn normally.
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                game.isInHand(1, "Hill Giant") shouldBe true
                withClue("Plains, then the two bottomed lands, remain in the library") {
                    game.librarySize(1) shouldBe 3
                }
            }

            test("choosing land takes the first land and bottoms the nonland cards above it") {
                val game = abundance("Grizzly Bears", "Hill Giant", "Forest", "Plains")

                game.castSpell(1, "Counsel of the Soratami").error shouldBe null
                game.resolveStack()

                game.answerYesNo(true)
                game.chooseKind("Land")
                game.keepOrderIfAsked()

                game.isInHand(1, "Forest") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe false
                game.isInHand(1, "Hill Giant") shouldBe false

                // Second draw: Plains is now on top.
                game.answerYesNo(false)
                game.resolveStack()
                game.isInHand(1, "Plains") shouldBe true
                game.librarySize(1) shouldBe 2
            }

            test("with no card of the chosen kind, no card is put into hand and the library is kept") {
                val game = abundance("Grizzly Bears", "Hill Giant")

                game.castSpell(1, "Counsel of the Soratami").error shouldBe null
                game.resolveStack()

                game.answerYesNo(true)
                game.chooseKind("Land")
                game.keepOrderIfAsked()

                withClue("2004-10-04 ruling: no card found, you don't get a card") {
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                    game.isInHand(1, "Hill Giant") shouldBe false
                    game.librarySize(1) shouldBe 2
                }
            }
        }
    }
}
