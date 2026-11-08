package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.NotionThief
import com.wingedsheep.mtg.sets.definitions.lea.cards.HowlingMine
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Notion Thief (DGM #88) — "If an opponent would draw a card except the first one they draw in
 * each of their draw steps, instead that player skips that draw and you draw a card."
 */
class NotionThiefScenarioTest : FunSpec({

    fun drawSpell(n: Int) = card("Notion Thief Test Draw $n") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(n) }
    }
    val drawOne = drawSpell(1)
    val drawTwo = drawSpell(2)

    fun driver(startingPlayer: Int) = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(NotionThief, HowlingMine, drawOne, drawTwo))
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = startingPlayer)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.librarySize(p: EntityId) = state.getZone(ZoneKey(p, Zone.LIBRARY)).size

    test("the first card an opponent draws in their draw step is theirs; an extra draw goes to you") {
        val d = driver(startingPlayer = 0)
        val me = d.player1
        val foe = d.player2
        d.putPermanentOnBattlefield(me, "Notion Thief")
        d.putPermanentOnBattlefield(me, "Howling Mine") // each player draws an additional card in their draw step
        val myHand = d.getHandSize(me)
        val foeHand = d.getHandSize(foe)
        val foeLibrary = d.librarySize(foe)
        val myLibrary = d.librarySize(me)

        d.passPriorityUntil(Step.DRAW)
        d.activePlayer shouldBe foe
        // The turn-based draw is untouched; Howling Mine's extra draw is on the stack.
        d.getHandSize(foe) shouldBe foeHand + 1
        d.bothPass()

        d.pendingDecision shouldBe null
        d.getHandSize(foe) shouldBe foeHand + 1
        d.librarySize(foe) shouldBe foeLibrary - 1
        d.getHandSize(me) shouldBe myHand + 1
        d.librarySize(me) shouldBe myLibrary - 1
    }

    test("every card of an opponent's multi-card draw is replaced, and you draw that many") {
        val d = driver(startingPlayer = 1)
        val me = d.player1
        val foe = d.player2
        d.activePlayer shouldBe foe
        d.putPermanentOnBattlefield(me, "Notion Thief")
        val spell = d.putCardInHand(foe, drawTwo.name)
        val myHand = d.getHandSize(me)
        val foeHandAfterCasting = d.getHandSize(foe) - 1
        val foeLibrary = d.librarySize(foe)
        val myLibrary = d.librarySize(me)

        d.castSpell(foe, spell).error shouldBe null
        d.bothPass()

        d.pendingDecision shouldBe null
        d.getHandSize(foe) shouldBe foeHandAfterCasting
        d.librarySize(foe) shouldBe foeLibrary
        d.getHandSize(me) shouldBe myHand + 2
        d.librarySize(me) shouldBe myLibrary - 2
    }

    test("your own draws are not replaced") {
        val d = driver(startingPlayer = 0)
        val me = d.player1
        d.putPermanentOnBattlefield(me, "Notion Thief")
        val spell = d.putCardInHand(me, drawOne.name)
        val handAfterCasting = d.getHandSize(me) - 1
        val library = d.librarySize(me)

        d.castSpell(me, spell).error shouldBe null
        d.bothPass()

        d.getHandSize(me) shouldBe handAfterCasting + 1
        d.librarySize(me) shouldBe library - 1
    }

    test("with the Thief gone an opponent's extra draws are their own again") {
        val d = driver(startingPlayer = 1)
        val me = d.player1
        val foe = d.player2
        val spell = d.putCardInHand(foe, drawOne.name)
        val foeHandAfterCasting = d.getHandSize(foe) - 1
        val myHand = d.getHandSize(me)

        d.castSpell(foe, spell).error shouldBe null
        d.bothPass()

        d.getHandSize(foe) shouldBe foeHandAfterCasting + 1
        d.getHandSize(me) shouldBe myHand
    }
})
