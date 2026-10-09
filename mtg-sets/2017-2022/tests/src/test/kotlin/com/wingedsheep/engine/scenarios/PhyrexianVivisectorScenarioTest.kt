package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PhyrexianVivisectorScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bolt(target: EntityId) {
        giveMana(player1, Color.RED, 1)
        val spell = putCardInHand(player1, "Lightning Bolt")
        castSpell(player1, spell, listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("its own death scries and the chosen card can go to the bottom") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Phyrexian Vivisector")
        val top = d.putCardOnTopOfLibrary(d.player1, "Mountain")
        d.bolt(source)
        d.stackSize shouldBe 1
        d.bothPass()
        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        choice.options shouldBe listOf(top)
        d.submitDecision(d.player1, CardsSelectedResponse(choice.id, listOf(top)))
        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).last() shouldBe top
        d.pendingDecision shouldBe null
        d.stackSize shouldBe 0
    }

    test("an allied death scries but an opposing death does not") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Phyrexian Vivisector")
        val enemy = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.bolt(enemy)
        d.stackSize shouldBe 0
        val ally = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.bolt(ally)
        d.stackSize shouldBe 1
        d.bothPass()
        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(d.player1, CardsSelectedResponse(choice.id, emptyList()))
        val order = d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        d.submitDecision(d.player1, OrderedResponse(order.id, order.cards))
        d.pendingDecision shouldBe null
        d.stackSize shouldBe 0
    }
})
