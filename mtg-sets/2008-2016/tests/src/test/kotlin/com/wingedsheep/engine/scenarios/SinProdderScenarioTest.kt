package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.SinProdder
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sin Prodder (SOI) — at the beginning of your upkeep, reveal the top card of your library. Any
 * opponent may have you put it into your graveyard; if a player does, Sin Prodder deals that player
 * damage equal to its mana value. Otherwise, put it into your hand.
 */
class SinProdderScenarioTest : FunSpec({

    data class Setup(val driver: GameTestDriver, val me: EntityId, val opp: EntityId, val top: EntityId)

    /** Sin Prodder in play; on my next upkeep [topCard] is on top of my library and the trigger is about to resolve. */
    fun atMyUpkeep(topCard: String): Setup {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(SinProdder)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 30))
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(me, "Sin Prodder")
        d.passPriorityUntil(Step.UPKEEP, activePlayer = me)
        val top = d.putCardOnTopOfLibrary(me, topCard)
        d.stackSize shouldBe 1
        d.bothPass()
        return Setup(d, me, opp, top)
    }

    test("the opponent accepts: the card goes to the graveyard and Sin Prodder deals them its mana value") {
        val (d, me, opp, top) = atMyUpkeep("Centaur Courser") // {2}{G}, mana value 3

        val ask = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        ask.playerId shouldBe opp
        d.submitYesNo(opp, true)

        d.pendingDecision shouldBe null
        d.getGraveyard(me) shouldContain top
        d.getHand(me) shouldNotContain top
        d.getLifeTotal(opp) shouldBe 17
        d.getLifeTotal(me) shouldBe 20
    }

    test("the opponent declines: the card goes to my hand and nobody takes damage") {
        val (d, me, opp, top) = atMyUpkeep("Centaur Courser")

        d.submitYesNo(opp, false)

        d.pendingDecision shouldBe null
        d.getHand(me) shouldContain top
        d.getGraveyard(me) shouldNotContain top
        d.getLifeTotal(opp) shouldBe 20
    }

    test("a land has mana value 0: accepting bins it for no damage") {
        val (d, me, opp, top) = atMyUpkeep("Forest")

        d.submitYesNo(opp, true)

        d.getGraveyard(me) shouldContain top
        d.getLifeTotal(opp) shouldBe 20
    }
})
