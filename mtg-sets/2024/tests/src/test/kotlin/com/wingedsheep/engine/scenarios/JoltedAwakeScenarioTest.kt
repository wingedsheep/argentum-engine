package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.JoltedAwake
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class JoltedAwakeScenarioTest : FunSpec({
    val bauble = card("Test Zero Artifact") { manaCost = "{X}"; typeLine = "Artifact" }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(JoltedAwake, bauble))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy() = state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.cast(targetName: String? = null) {
        val spell = putCardInHand(player1, "Jolted Awake")
        val targets = targetName?.let { listOf(ChosenTarget.Card(putCardInGraveyard(player1, it), player1, Zone.GRAVEYARD)) } ?: emptyList()
        giveMana(player1, Color.WHITE, 1)
        castSpellWithTargets(player1, spell, targets).error shouldBe null
        bothPass()
    }
    test("gains energy first then pays the target mana value and returns it without a new stack object") {
        val d = driver()
        d.cast("Grizzly Bears")
        d.energy() shouldBe 2
        (d.state.pendingDecision as YesNoDecision).yesText shouldBe "Pay 2 energy counters"
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 0
        d.findPermanent(d.player1, "Grizzly Bears") shouldNotBe null
        d.state.stack.size shouldBe 0
    }
    test("declining retains energy and leaves the target in the graveyard") {
        val d = driver()
        d.cast("Grizzly Bears")
        d.submitYesNo(d.player1, false).error shouldBe null
        d.energy() shouldBe 2
        d.getGraveyardCardNames(d.player1).contains("Grizzly Bears") shouldBe true
    }
    test("unaffordable payment does not prompt or spend part of the energy") {
        val d = driver()
        d.cast("Hill Giant")
        d.state.pendingDecision shouldBe null
        d.energy() shouldBe 2
        d.findPermanent(d.player1, "Hill Giant") shouldBe null
    }
    test("no chosen target still gains energy without a payment prompt") {
        val d = driver()
        d.cast()
        d.energy() shouldBe 2
        d.state.pendingDecision shouldBe null
    }
    test("zero mana value including X in the graveyard still offers a zero payment") {
        val d = driver()
        d.cast(bauble.name)
        d.state.pendingDecision shouldNotBe null
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 2
        d.findPermanent(d.player1, bauble.name) shouldNotBe null
    }
    test("a target leaving the graveyard makes the spell fizzle without gaining energy") {
        val d = driver()
        val spell = d.putCardInHand(d.player1, "Jolted Awake")
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(bear, d.player1, Zone.GRAVEYARD))).error shouldBe null
        d.replaceState(d.zones.moveToZone(d.state, bear, Zone.EXILE).state)
        d.bothPass()
        d.energy() shouldBe 0
        d.state.pendingDecision shouldBe null
    }
    test("cycling draws a card without gaining energy") {
        val d = driver()
        val spell = d.putCardInHand(d.player1, "Jolted Awake")
        d.giveMana(d.player1, Color.WHITE, 2)
        val before = d.state.getHand(d.player1).size
        d.submit(CycleCard(d.player1, spell)).error shouldBe null
        d.bothPass()
        d.state.getHand(d.player1).size shouldBe before
        d.energy() shouldBe 0
        d.getGraveyardCardNames(d.player1).contains("Jolted Awake") shouldBe true
    }
})
