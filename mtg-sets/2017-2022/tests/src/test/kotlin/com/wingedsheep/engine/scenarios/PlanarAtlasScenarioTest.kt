package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j22.cards.PlanarAtlas
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlanarAtlasScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + PlanarAtlas)
        initMirrorMatch(deck = Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castAtlas(): EntityId {
        val you = activePlayer!!
        val atlas = putCardInHand(you, "Planar Atlas")
        giveColorlessMana(you, 2)
        submit(CastSpell(you, atlas, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        bothPass()
        bothPass()
        return atlas
    }

    test("declining the look leaves the library unchanged and Atlas enters tapped") {
        val d = setup()
        val you = d.activePlayer!!
        val before = d.state.getLibrary(you)
        val atlas = d.castAtlas()
        d.isTapped(atlas) shouldBe true
        d.submitYesNo(you, false)
        d.state.getLibrary(you) shouldBe before
        d.pendingDecision shouldBe null
    }

    test("reveals only the chosen land, leaves it on top, and bottoms the other three") {
        val d = setup()
        val you = d.activePlayer!!
        d.putCardOnTopOfLibrary(you, "Centaur Courser")
        val before = d.state.getLibrary(you)
        d.castAtlas()
        d.submitYesNo(you, true)
        val choice = d.pendingDecision as SelectCardsDecision
        choice.nonSelectableOptions shouldBe listOf(before.first())
        choice.minSelections shouldBe 0
        choice.maxSelections shouldBe 1
        choice.options.toSet() shouldBe before.take(4).drop(1).toSet()
        val land = before[2]
        val result = d.submitCardSelection(you, listOf(land))
        result.events.filterIsInstance<CardsRevealedEvent>().single().cardIds shouldBe listOf(land)
        d.state.getLibrary(you).first() shouldBe land
        d.state.getLibrary(you).drop(1).dropLast(3) shouldBe before.drop(4)
        d.state.getLibrary(you).takeLast(3).toSet() shouldBe (before.take(4) - land).toSet()
        d.pendingDecision shouldBe null
    }

    test("accepting the look but choosing no land bottoms all four") {
        val d = setup()
        val you = d.activePlayer!!
        val before = d.state.getLibrary(you)
        d.castAtlas()
        d.submitYesNo(you, true)
        d.submitCardSelection(you, emptyList())
        d.state.getLibrary(you).dropLast(4) shouldBe before.drop(4)
        d.state.getLibrary(you).takeLast(4).toSet() shouldBe before.take(4).toSet()
        d.pendingDecision shouldBe null
    }

    test("a pile with no lands is still shown before all four cards go to the bottom") {
        val d = setup()
        val you = d.activePlayer!!
        repeat(4) { d.putCardOnTopOfLibrary(you, "Centaur Courser") }
        val before = d.state.getLibrary(you)
        d.castAtlas()
        d.submitYesNo(you, true)
        val choice = d.pendingDecision as SelectCardsDecision
        choice.options shouldBe emptyList()
        choice.nonSelectableOptions shouldBe before.take(4)
        d.submitCardSelection(you, emptyList())
        d.state.getLibrary(you).dropLast(4) shouldBe before.drop(4)
        d.state.getLibrary(you).takeLast(4).toSet() shouldBe before.take(4).toSet()
    }

    test("after untapping Atlas produces one colorless mana without using the stack") {
        val d = setup()
        val you = d.activePlayer!!
        val atlas = d.castAtlas()
        d.submitYesNo(you, false)
        d.untapPermanent(atlas)
        d.submit(ActivateAbility(you, atlas, PlanarAtlas.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        d.isTapped(atlas) shouldBe true
        d.state.getEntity(you)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
        d.state.stack.isEmpty() shouldBe true
    }
})
