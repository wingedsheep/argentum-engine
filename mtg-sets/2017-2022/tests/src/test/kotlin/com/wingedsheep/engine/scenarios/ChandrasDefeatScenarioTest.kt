package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Chandra's Defeat (HOU #86) — {R} Instant.
 *
 *   Chandra's Defeat deals 5 damage to target red creature or red planeswalker. If that permanent
 *   is a Chandra planeswalker, you may discard a card. If you do, draw a card.
 *
 * Only the Chandra branch offers the rummage.
 */
class ChandrasDefeatScenarioTest : ScenarioTestBase() {

    init {
        fun board(victim: String) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Chandra's Defeat")
            .withCardInHand(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardInLibrary(1, "Island")
            .withCardOnBattlefield(2, victim)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("deals 5 damage to a red creature without offering a rummage") {
            val game = board("Hill Giant")

            game.castSpell(1, "Chandra's Defeat", targetId = game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.librarySize(1) shouldBe 1
        }

        test("killing a Chandra planeswalker lets the caster discard a card to draw one") {
            val game = board("Chandra, Torch of Defiance")

            game.castSpell(1, "Chandra's Defeat", targetId = game.findPermanent("Chandra, Torch of Defiance")!!)
                .error shouldBe null
            game.resolveStack()

            val bears = game.findCardsInHand(1, "Grizzly Bears").single()
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            if (game.hasPendingDecision()) game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Chandra, Torch of Defiance") shouldBe true
            withClue("the discarded card is in the graveyard") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
            withClue("the draw replaced it") {
                game.isInHand(1, "Island") shouldBe true
                game.handSize(1) shouldBe 1
            }
        }

        test("declining the rummage neither discards nor draws") {
            val game = board("Chandra, Torch of Defiance")

            game.castSpell(1, "Chandra's Defeat", targetId = game.findPermanent("Chandra, Torch of Defiance")!!)
                .error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Chandra, Torch of Defiance") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.librarySize(1) shouldBe 1
        }

        test("a red planeswalker that isn't a Chandra takes the damage without a rummage") {
            val game = board("Sarkhan, the Dragonspeaker")

            game.castSpell(1, "Chandra's Defeat", targetId = game.findPermanent("Sarkhan, the Dragonspeaker")!!)
                .error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInGraveyard(2, "Sarkhan, the Dragonspeaker") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.librarySize(1) shouldBe 1
        }

        test("a non-red creature isn't a legal target") {
            val game = board("Grizzly Bears")

            game.castSpell(1, "Chandra's Defeat", targetId = game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
        }
    }
}
