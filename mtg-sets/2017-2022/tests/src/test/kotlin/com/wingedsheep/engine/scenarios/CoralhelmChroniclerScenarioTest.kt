package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Coralhelm Chronicler (ZNR #54) — Whenever you cast a kicked spell, draw a card, then discard a
 * card. When this creature enters, look at the top five cards of your library. You may reveal a card
 * with a kicker ability from among them and put it into your hand. Put the rest on the bottom of
 * your library in a random order.
 */
class CoralhelmChroniclerScenarioTest : ScenarioTestBase() {

    init {
        test("enters: only a card with a kicker ability can be taken from the top five") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Coralhelm Chronicler")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Murasa Sproutling")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val sproutling = game.findCardsInLibrary(1, "Murasa Sproutling").single()
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()

            game.castSpell(1, "Coralhelm Chronicler").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldContain sproutling
            decision.options shouldNotContain bears

            game.selectCards(listOf(sproutling)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Murasa Sproutling") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
            game.librarySize(1) shouldBe 4
        }

        test("casting a kicked spell loots; a kicker card cast without its kicker doesn't") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Coralhelm Chronicler")
                .withCardInHand(1, "Murasa Sproutling")
                .withCardInHand(1, "Murasa Sproutling")
                .withLandsOnBattlefield(1, "Forest", 8)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val (unkicked, kicked) = game.findCardsInHand(1, "Murasa Sproutling")

            game.execute(CastSpell(game.player1Id, unkicked)).error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.resolveStack()
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 2

            game.execute(CastSpell(game.player1Id, kicked, declaredCostSlot = ChoiceSlot.KICKED))
                .error shouldBe null
            game.resolveStack()

            // Loot: drew an Island, then discarded it (the only card in hand, so no choice to make).
            game.librarySize(1) shouldBe 1
            game.graveyardSize(1) shouldBe 1
            game.handSize(1) shouldBe 0
        }
    }
}
