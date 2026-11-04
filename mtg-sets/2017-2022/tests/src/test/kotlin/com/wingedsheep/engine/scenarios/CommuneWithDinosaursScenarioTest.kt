package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.CommuneWithDinosaurs
import com.wingedsheep.mtg.sets.definitions.xln.cards.ThrashOfRaptors
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CommuneWithDinosaursScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(CommuneWithDinosaurs, ThrashOfRaptors))
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castCommune() {
        val you = activePlayer!!
        giveMana(you, Color.GREEN, 1)
        val spell = putCardInHand(you, "Commune with Dinosaurs")
        castSpell(you, spell).error shouldBe null
        bothPass()
    }

    for (pickDinosaur in listOf(true, false)) {
        test("reveals a ${if (pickDinosaur) "Dinosaur" else "land"} and bottoms the rest in the chosen order") {
            val d = setup()
            val you = d.activePlayer!!
            val unlooked = d.state.getZone(ZoneKey(you, Zone.LIBRARY))
            val dinosaur = d.putCardOnTopOfLibrary(you, "Thrash of Raptors")
            val land = d.putCardOnTopOfLibrary(you, "Island")
            val bear = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
            val giant = d.putCardOnTopOfLibrary(you, "Hill Giant")
            val bolt = d.putCardOnTopOfLibrary(you, "Lightning Bolt")
            d.castCommune()

            val select = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            select.minSelections shouldBe 0
            select.maxSelections shouldBe 1
            select.options shouldContainExactlyInAnyOrder listOf(dinosaur, land)
            select.nonSelectableOptions shouldContainExactlyInAnyOrder listOf(bear, giant, bolt)
            val chosen = if (pickDinosaur) dinosaur else land
            val result = d.submitCardSelection(you, listOf(chosen))
            result.error shouldBe null
            result.events.filterIsInstance<CardsRevealedEvent>().flatMap { it.cardIds } shouldBe listOf(chosen)
            d.getHand(you) shouldContain chosen

            val order = listOf(giant, if (pickDinosaur) land else dinosaur, bolt, bear)
            d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>().cards shouldContainExactlyInAnyOrder order
            d.submitOrderedResponse(you, order).error shouldBe null
            d.state.getZone(ZoneKey(you, Zone.LIBRARY)) shouldBe unlooked + order
            d.isPaused shouldBe false
            d.stackSize shouldBe 0
        }
    }

    test("may decline a matching card and put all five on the bottom") {
        val d = setup()
        val you = d.activePlayer!!
        val unlooked = d.state.getZone(ZoneKey(you, Zone.LIBRARY))
        val cards = listOf("Forest", "Thrash of Raptors", "Grizzly Bears", "Hill Giant", "Lightning Bolt")
            .map { d.putCardOnTopOfLibrary(you, it) }
        val hand = d.getHand(you)
        d.castCommune()
        val result = d.submitCardSelection(you, emptyList())
        result.error shouldBe null
        result.events.filterIsInstance<CardsRevealedEvent>() shouldBe emptyList()
        d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>().cards shouldContainExactlyInAnyOrder cards
        d.submitOrderedResponse(you, cards).error shouldBe null
        d.getHand(you) shouldBe hand
        d.state.getZone(ZoneKey(you, Zone.LIBRARY)) shouldBe unlooked + cards
    }

    test("a short library shows all cards even when none match") {
        val d = setup()
        val you = d.activePlayer!!
        val key = ZoneKey(you, Zone.LIBRARY)
        d.replaceState(d.state.copy(zones = d.state.zones + (key to emptyList())))
        val bear = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val bolt = d.putCardOnTopOfLibrary(you, "Lightning Bolt")
        val hand = d.getHand(you)
        d.castCommune()
        val select = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        select.options shouldBe emptyList()
        select.nonSelectableOptions shouldContainExactlyInAnyOrder listOf(bear, bolt)
        d.submitCardSelection(you, emptyList()).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>().cards shouldContainExactlyInAnyOrder listOf(bear, bolt)
        d.submitOrderedResponse(you, listOf(bear, bolt)).error shouldBe null
        d.state.getZone(key) shouldBe listOf(bear, bolt)
        d.getHand(you) shouldBe hand
        d.isPaused shouldBe false
    }
})
