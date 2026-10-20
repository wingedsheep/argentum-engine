package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.HowlingMine
import com.wingedsheep.mtg.sets.definitions.leg.cards.ChainsOfMephistopheles
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Chains of Mephistopheles (LEG #91) — {1}{B} Enchantment. "If a player would draw a card except
 * the first one they draw in each of their draw steps, that player discards a card instead. If the
 * player discards a card this way, they draw a card. If the player doesn't discard a card this way,
 * they mill a card."
 */
class ChainsOfMephistophelesScenarioTest : FunSpec({

    fun drawSpell(n: Int) = card("Chains Test Draw $n") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(n) }
    }
    val drawOne = drawSpell(1)
    val drawTwo = drawSpell(2)

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ChainsOfMephistopheles, HowlingMine, drawOne, drawTwo))
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.library(p: EntityId) = state.getZone(ZoneKey(p, Zone.LIBRARY))
    fun GameTestDriver.discard(p: EntityId, card: EntityId) {
        pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(p, listOf(card)).error shouldBe null
    }

    test("each card of a multi-card draw is replaced by discard-then-draw, applied once per card") {
        val d = driver(); val me = d.player1
        d.putPermanentOnBattlefield(d.player2, "Chains of Mephistopheles")
        val bears = d.putCardInHand(me, "Grizzly Bears")
        val giant = d.putCardInHand(me, "Centaur Courser")
        d.castSpell(me, d.putCardInHand(me, drawTwo.name)).error shouldBe null
        val hand = d.getHandSize(me); val library = d.library(me).size
        d.bothPass()

        d.discard(me, bears)
        d.discard(me, giant)
        d.pendingDecision shouldBe null
        d.getHandSize(me) shouldBe hand
        d.library(me).size shouldBe library - 2
        d.getGraveyardCardNames(me).count { it == "Grizzly Bears" || it == "Centaur Courser" } shouldBe 2
    }

    test("with an empty hand the player mills a card and doesn't draw") {
        val d = driver(); val me = d.player1
        d.putPermanentOnBattlefield(me, "Chains of Mephistopheles")
        val spell = d.putCardInHand(me, drawTwo.name)
        d.getHand(me).filter { it != spell }.forEach(d::moveToGraveyard)
        d.castSpell(me, spell).error shouldBe null
        val top = d.library(me).take(2); val graveyard = d.getGraveyard(me).size
        d.bothPass()

        d.pendingDecision shouldBe null
        d.getHandSize(me) shouldBe 0
        d.getGraveyard(me).toSet().containsAll(top) shouldBe true
        d.getGraveyard(me).size shouldBe graveyard + 3 // two milled cards and the spell
    }

    test("the first draw of a draw step is exempt, an additional draw in that step is not") {
        val d = driver(); val me = d.player1; val foe = d.player2
        d.putPermanentOnBattlefield(me, "Chains of Mephistopheles")
        d.putPermanentOnBattlefield(me, "Howling Mine")
        val foeHand = d.getHandSize(foe); val foeLibrary = d.library(foe).size
        d.passPriorityUntil(Step.DRAW)
        d.activePlayer shouldBe foe
        // The turn-based draw went through untouched; Howling Mine's extra draw is on the stack.
        d.getHandSize(foe) shouldBe foeHand + 1
        d.pendingDecision shouldBe null
        d.bothPass()

        val discarded = d.getHand(foe).first()
        d.discard(foe, discarded)
        d.pendingDecision shouldBe null
        d.getHandSize(foe) shouldBe foeHand + 1
        d.library(foe).size shouldBe foeLibrary - 2
        (discarded in d.getGraveyard(foe)) shouldBe true
    }

    test("two Chains each replace the draw once: discard twice, then draw") {
        val d = driver(); val me = d.player1
        d.putPermanentOnBattlefield(me, "Chains of Mephistopheles")
        d.putPermanentOnBattlefield(d.player2, "Chains of Mephistopheles")
        val bears = d.putCardInHand(me, "Grizzly Bears")
        val giant = d.putCardInHand(me, "Centaur Courser")
        d.castSpell(me, d.putCardInHand(me, drawOne.name)).error shouldBe null
        val hand = d.getHandSize(me); val library = d.library(me).size
        d.bothPass()

        // The drawing player orders the two Chains (CR 616.1); the second then replaces the
        // first one's draw, and nothing replaces the second one's.
        val order = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(me, OptionChosenResponse(order.id, 0)).error shouldBe null
        d.discard(me, bears)
        d.discard(me, giant)
        d.pendingDecision shouldBe null
        d.getHandSize(me) shouldBe hand - 1
        d.library(me).size shouldBe library - 1
    }
})
