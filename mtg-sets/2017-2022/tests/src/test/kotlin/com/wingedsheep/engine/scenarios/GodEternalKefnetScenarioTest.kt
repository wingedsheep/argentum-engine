package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.GodEternalKefnet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * God-Eternal Kefnet — "You may reveal the first card you draw each turn as you draw it. Whenever
 * you reveal an instant or sorcery card this way, copy that card and you may cast the copy. That
 * copy costs {2} less to cast."
 *
 * The optional reveal itself is pinned by the engine's `OptionalFirstDrawRevealTest`; this covers
 * Kefnet's own trigger: the copy is made in the hand and cast from there for {2} less, the original
 * stays in hand, and a land revealed this way does nothing.
 */
class GodEternalKefnetScenarioTest : FunSpec({

    val drawOne = card("Test Kefnet Draw") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(1) }
    }
    val lifeSorcery = card("Test Kefnet Life Sorcery") {
        manaCost = "{2}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(4) }
    }

    // Starting player's first turn: no draw-step draw, so the spell's draw is the turn's first.
    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GodEternalKefnet, drawOne, lifeSorcery))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while (guard++ < 30 && state.stack.isNotEmpty() && pendingDecision == null) bothPass()
    }

    fun GameTestDriver.inHand(player: EntityId, id: EntityId) = id in state.getZone(ZoneKey(player, Zone.HAND))

    fun GameTestDriver.drawWithKefnet(me: EntityId) {
        castSpell(me, putCardInHand(me, "Test Kefnet Draw"))
        drain()
        (pendingDecision as YesNoDecision).prompt.endsWith("with God-Eternal Kefnet?") shouldBe true
    }

    test("revealing a sorcery copies it, and the copy is cast for {2} less") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "God-Eternal Kefnet")
        val sorcery = driver.putCardOnTopOfLibrary(me, "Test Kefnet Life Sorcery")
        val life = driver.getLifeTotal(me)

        driver.drawWithKefnet(me)
        driver.submitYesNo(me, true)
        driver.events.filterIsInstance<CardRevealedFromDrawEvent>().single().cardEntityId shouldBe sorcery

        // Kefnet's trigger resolves: copy the card, then "you may cast the copy".
        driver.giveMana(me, Color.BLUE, 1)
        driver.drain()
        driver.submitYesNo(me, true)
        driver.drain()

        withClue("the copy cost only {U} ({2}{U} less {2}) and resolved") {
            driver.getLifeTotal(me) shouldBe life + 4
        }
        withClue("the original stays in hand") {
            driver.inHand(me, sorcery) shouldBe true
        }
        driver.state.stack.shouldBeEmpty()
    }

    test("declining to cast the copy leaves only the original in hand") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "God-Eternal Kefnet")
        val sorcery = driver.putCardOnTopOfLibrary(me, "Test Kefnet Life Sorcery")
        val life = driver.getLifeTotal(me)

        driver.drawWithKefnet(me)
        driver.submitYesNo(me, true)
        val handWithOriginal = driver.state.getZone(ZoneKey(me, Zone.HAND)).size
        driver.drain()
        driver.submitYesNo(me, false)
        driver.drain()

        driver.getLifeTotal(me) shouldBe life
        driver.inHand(me, sorcery) shouldBe true
        withClue("the uncast copy ceased to exist") {
            driver.state.getZone(ZoneKey(me, Zone.HAND)).size shouldBe handWithOriginal
        }
    }

    test("revealing a land does nothing") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "God-Eternal Kefnet")
        driver.putCardOnTopOfLibrary(me, "Forest")

        driver.drawWithKefnet(me)
        driver.submitYesNo(me, true)
        driver.events.filterIsInstance<CardRevealedFromDrawEvent>().size shouldBe 1
        driver.state.stack.shouldBeEmpty()
        driver.pendingDecision shouldBe null
    }
})
