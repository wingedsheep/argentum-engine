package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * CR 614.5: a replacement effect doesn't get another opportunity to affect an event it already
 * modified — including the new events its own replacement performs. A draw replacement whose
 * effect *pauses* (a discard choice) must still exclude itself from the draw it performs after the
 * pause; and once it finishes, the next draw of the same instruction is a fresh event it replaces
 * again. Pinned on both resume paths: the single-replacement dispatcher and the competing-
 * replacement order choice (CR 616.1).
 */
class DrawReplacementChainAcrossPauseTest : FunSpec({

    // Chains of Mephistopheles' shape without its draw-step exemption: the discard pauses for a
    // choice when the hand holds more than one card, then performs a draw of its own.
    val looter = card("Pausing Draw Replacer") {
        typeLine = "Enchantment"
        replacementEffect(ReplaceDrawWith(
            replacementEffect = Effects.Discard(1) then Effects.DrawCards(1),
            appliesTo = EventPattern.DrawEvent(player = Player.You),
        ))
    }
    val lifeDraw = card("Synchronous Draw Replacer") {
        typeLine = "Enchantment"
        replacementEffect(ReplaceDrawWith(
            replacementEffect = Effects.GainLife(1) then Effects.DrawCards(1),
            appliesTo = EventPattern.DrawEvent(player = Player.You),
        ))
    }
    fun drawSpell(n: Int) = card("Draw Replacement Test Draw $n") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(n) }
    }
    val drawOne = drawSpell(1)
    val drawTwo = drawSpell(2)

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(looter, lifeDraw, drawOne, drawTwo))
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.librarySize(p: EntityId) = state.getZone(ZoneKey(p, Zone.LIBRARY)).size
    fun GameTestDriver.discardFirstOption(p: EntityId) {
        val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(p, listOf(decision.options.first())).error shouldBe null
    }

    test("a replacement that pauses does not replace the draw it performs after the pause") {
        val d = driver(); val me = d.player1
        d.putPermanentOnBattlefield(me, looter.name)
        d.castSpell(me, d.putCardInHand(me, drawOne.name)).error shouldBe null
        val hand = d.getHandSize(me); val library = d.librarySize(me)
        d.bothPass()

        d.discardFirstOption(me)
        // Pre-fix the replacement's own draw was re-replaced: another discard prompt, until the
        // hand ran out.
        d.pendingDecision shouldBe null
        d.getHandSize(me) shouldBe hand
        d.librarySize(me) shouldBe library - 1
    }

    test("the next draw of the same instruction is a new event the replacement applies to again") {
        val d = driver(); val me = d.player1
        d.putPermanentOnBattlefield(me, looter.name)
        d.castSpell(me, d.putCardInHand(me, drawTwo.name)).error shouldBe null
        val hand = d.getHandSize(me); val library = d.librarySize(me)
        d.bothPass()

        d.discardFirstOption(me)
        d.discardFirstOption(me)
        d.pendingDecision shouldBe null
        d.getHandSize(me) shouldBe hand
        d.librarySize(me) shouldBe library - 2
    }

    test("after an order choice, each competing replacement applies once across the pause") {
        val d = driver(); val me = d.player1
        d.putPermanentOnBattlefield(me, looter.name)
        d.putPermanentOnBattlefield(me, lifeDraw.name)
        d.castSpell(me, d.putCardInHand(me, drawOne.name)).error shouldBe null
        val hand = d.getHandSize(me); val library = d.librarySize(me); val life = d.getLifeTotal(me)
        d.bothPass()

        val order = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val looterFirst = order.options.indexOfFirst { it.startsWith(looter.name) }
        d.submitDecision(me, OptionChosenResponse(order.id, looterFirst)).error shouldBe null
        d.discardFirstOption(me)

        // The looter's draw is replaced by the life replacer (the looter can't apply again),
        // whose own draw neither can replace.
        d.pendingDecision shouldBe null
        d.getLifeTotal(me) shouldBe life + 1
        d.getHandSize(me) shouldBe hand
        d.librarySize(me) shouldBe library - 1
    }
})
