package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.UlamogsDespoiler
import com.wingedsheep.mtg.sets.definitions.ktk.cards.AnafenzaTheForemost
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ulamog's Despoiler — "As this creature enters, you may put two cards your opponents own from
 * exile into their owners' graveyards. If you do, this creature enters with four +1/+1 counters."
 * All or none: exactly two opponents' exiled cards, never one, never your own.
 */
class UlamogsDespoilerScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(UlamogsDespoiler)
        registerCard(AnafenzaTheForemost)
        initMirrorMatch(deck = Deck.of("Wastes" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.plusCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castDespoiler(player: EntityId): EntityId {
        val despoiler = putCardInHand(player, "Ulamog's Despoiler")
        giveColorlessMana(player, 6)
        castSpell(player, despoiler).error shouldBe null
        // Every player passes in turn until it resolves (two in a duel, three in multiplayer).
        while (pendingDecision == null && despoiler in state.stack) passPriority(state.priorityPlayerId!!)
        return despoiler
    }

    test("processing two of an opponent's exiled cards makes it enter with four +1/+1 counters") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val a = d.putCardInExile(bob, "Grizzly Bears")
        val b = d.putCardInExile(bob, "Hill Giant")
        val c = d.putCardInExile(bob, "Island")

        val despoiler = d.castDespoiler(alice)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(alice, true).error shouldBe null
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.options shouldContainExactlyInAnyOrder listOf(a, b, c)
        pick.minSelections shouldBe 2
        pick.maxSelections shouldBe 2
        d.submitCardSelection(alice, listOf(a, b)).error shouldBe null

        d.state.getBattlefield() shouldContain despoiler
        d.plusCounters(despoiler) shouldBe 4
        d.getGraveyard(bob) shouldContain a
        d.getGraveyard(bob) shouldContain b
        d.state.getZone(ZoneKey(bob, Zone.EXILE)) shouldContain c
    }

    test("exactly one card is never enough: a one-card pick is rejected") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val a = d.putCardInExile(bob, "Grizzly Bears")
        d.putCardInExile(bob, "Hill Giant")
        d.putCardInExile(bob, "Island")

        d.castDespoiler(alice)
        d.submitYesNo(alice, true).error shouldBe null
        d.submitCardSelection(alice, listOf(a)).error shouldNotBe null
        d.state.getZone(ZoneKey(bob, Zone.EXILE)) shouldContain a
    }

    test("with exactly two candidates, yes processes both without a pick") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val a = d.putCardInExile(bob, "Grizzly Bears")
        val b = d.putCardInExile(bob, "Hill Giant")

        val despoiler = d.castDespoiler(alice)
        d.submitYesNo(alice, true).error shouldBe null

        d.pendingDecision.shouldBeNull()
        d.plusCounters(despoiler) shouldBe 4
        d.getGraveyard(bob) shouldContain a
        d.getGraveyard(bob) shouldContain b
    }

    test("declining leaves the exiled cards alone and it enters as a 5/5") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val a = d.putCardInExile(bob, "Grizzly Bears")
        val b = d.putCardInExile(bob, "Hill Giant")

        val despoiler = d.castDespoiler(alice)
        d.submitYesNo(alice, false).error shouldBe null

        d.state.getBattlefield() shouldContain despoiler
        d.plusCounters(despoiler) shouldBe 0
        d.state.getZone(ZoneKey(bob, Zone.EXILE)) shouldContain a
        d.state.getZone(ZoneKey(bob, Zone.EXILE)) shouldContain b
    }

    test("your own exiled cards don't count: one opponent card means no option") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val theirs = d.putCardInExile(bob, "Grizzly Bears")
        val mine = d.putCardInExile(alice, "Hill Giant")

        val despoiler = d.castDespoiler(alice)

        d.pendingDecision.shouldBeNull()
        d.state.getBattlefield() shouldContain despoiler
        d.plusCounters(despoiler) shouldBe 0
        d.state.getZone(ZoneKey(bob, Zone.EXILE)) shouldContain theirs
        d.state.getZone(ZoneKey(alice, Zone.EXILE)) shouldContain mine
        d.getGraveyard(alice) shouldNotContain mine
    }

    test("a graveyard-to-exile replacement doesn't stop it: the cards were still put, so it gets the counters") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        d.putPermanentOnBattlefield(alice, "Anafenza, the Foremost")
        d.putCardInExile(bob, "Grizzly Bears")
        d.putCardInExile(bob, "Hill Giant")

        val despoiler = d.castDespoiler(alice)
        d.submitYesNo(alice, true).error shouldBe null

        d.plusCounters(despoiler) shouldBe 4
        d.getGraveyard(bob).size shouldBe 0
    }

    test("in multiplayer the two cards may come from different opponents, each to its own owner's graveyard") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all)
            registerCard(UlamogsDespoiler)
        }
        val (alice, bob, carol) = d.initMultiplayer(List(3) { Deck.of("Wastes" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe alice
        val bobs = d.putCardInExile(bob, "Grizzly Bears")
        val carols = d.putCardInExile(carol, "Hill Giant")

        val despoiler = d.castDespoiler(alice)
        d.submitYesNo(alice, true).error shouldBe null

        d.plusCounters(despoiler) shouldBe 4
        d.getGraveyard(bob) shouldContain bobs
        d.getGraveyard(carol) shouldContain carols
    }
})
