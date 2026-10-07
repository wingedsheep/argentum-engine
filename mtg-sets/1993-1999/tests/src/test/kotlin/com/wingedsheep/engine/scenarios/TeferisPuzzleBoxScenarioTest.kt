package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vis.cards.TeferisPuzzleBox
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TeferisPuzzleBoxScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + TeferisPuzzleBox)
        initMirrorMatch(deck = Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (sourceLeaves in listOf(false, true)) {
        test("opponent orders their hand including the normal draw and replaces it; source leaves = $sourceLeaves") {
            val d = driver()
            val controller = d.activePlayer!!
            val opponent = d.getOpponent(controller)
            val box = d.putPermanentOnBattlefield(controller, "Teferi's Puzzle Box")
            val controllerHand = d.getHand(controller)
            val oldHand = d.getHand(opponent)
            val normalDraw = d.putCardOnTopOfLibrary(opponent, "Forest")

            d.passPriorityUntil(Step.DRAW, maxPasses = 200)
            d.activePlayer shouldBe opponent
            d.stackSize shouldBe 1
            d.getHand(opponent) shouldBe oldHand + normalDraw
            val hand = d.getHand(opponent)
            val library = d.state.getZone(ZoneKey(opponent, Zone.LIBRARY))
            if (sourceLeaves) d.moveToGraveyard(box)
            d.bothPass()

            val decision = d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
            decision.playerId shouldBe opponent
            decision.cards.toSet() shouldBe hand.toSet()
            d.submitOrderedResponse(opponent, hand.reversed()).error shouldBe null
            d.pendingDecision shouldBe null
            d.getHand(opponent) shouldBe library.take(hand.size)
            d.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).takeLast(hand.size) shouldBe hand.reversed()
            d.getHand(controller) shouldBe controllerHand
        }
    }

    test("an empty hand at resolution draws nothing and requires no ordering") {
        val d = driver()
        val controller = d.activePlayer!!
        val opponent = d.getOpponent(controller)
        d.putPermanentOnBattlefield(controller, "Teferi's Puzzle Box")
        d.passPriorityUntil(Step.DRAW, maxPasses = 200)
        d.getHand(opponent).forEach(d::moveToGraveyard)
        val library = d.state.getZone(ZoneKey(opponent, Zone.LIBRARY))
        d.bothPass()
        d.pendingDecision shouldBe null
        d.getHand(opponent) shouldBe emptyList()
        d.state.getZone(ZoneKey(opponent, Zone.LIBRARY)) shouldBe library
    }
})
