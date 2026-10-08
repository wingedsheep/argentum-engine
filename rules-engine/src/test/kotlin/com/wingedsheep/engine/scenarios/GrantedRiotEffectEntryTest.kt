package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Granted Riot (CR 702.136) on a permanent an *effect* puts onto the battlefield.
 *
 * Riot is a replacement effect on entering (CR 702.136a, 614.12): it applies however the permanent
 * enters, not only when it resolves as a spell. A lord's granted riot used to be synthesized only at
 * the spell-resolution, token and land-play seams, so a reanimated or searched-up creature under
 * Rhythm of the Wild entered with neither a counter nor haste. These pin the effect-entry seam:
 *  - a reanimation (`MoveToZone`) and a library search (`MoveCollection`) both ask counter-or-haste
 *    before the creature enters, and apply the answer as it arrives;
 *  - each granting lord is a separate instance (CR 702.136b) — two lords, two questions;
 *  - the lord's "creatures you control" filter reads the controller it enters under.
 */
class GrantedRiotEffectEntryTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.chooseRiot(player: EntityId, haste: Boolean) {
        val pick = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        pick.playerId shouldBe player
        submitDecision(player, OptionChosenResponse(pick.id, if (haste) 1 else 0)).error shouldBe null
    }

    /** [me] casts Breath of Life on the Grizzly Bears in their graveyard; stops at the first riot question. */
    fun GameTestDriver.reanimateBears(me: EntityId): EntityId {
        val bears = putCardInGraveyard(me, "Grizzly Bears")
        val breath = putCardInHand(me, "Breath of Life")
        giveMana(me, Color.WHITE, 5)
        submit(
            CastSpell(me, breath, targets = listOf(ChosenTarget.Card(bears, me, Zone.GRAVEYARD)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        bothPass()
        return bears
    }

    test("a reanimated creature under Rhythm of the Wild chooses a +1/+1 counter as it enters") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Rhythm of the Wild")

        val bears = d.reanimateBears(me)
        (bears in d.state.getBattlefield()) shouldBe false // the choice comes before it enters
        d.chooseRiot(me, haste = false)

        (bears in d.state.getBattlefield()) shouldBe true
        d.plusOneCounters(bears) shouldBe 1
        d.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
    }

    test("choosing haste gives it haste and no counter") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Rhythm of the Wild")

        val bears = d.reanimateBears(me)
        d.chooseRiot(me, haste = true)

        d.plusOneCounters(bears) shouldBe 0
        d.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
    }

    test("two granting lords are two instances: two questions, two counters (CR 702.136b)") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Rhythm of the Wild")
        d.putPermanentOnBattlefield(me, "Rhythm of the Wild")

        val bears = d.reanimateBears(me)
        d.chooseRiot(me, haste = false)
        d.chooseRiot(me, haste = false)

        d.plusOneCounters(bears) shouldBe 2
    }

    test("an opponent's Rhythm of the Wild grants nothing to your creature") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(d.getOpponent(me), "Rhythm of the Wild")

        val bears = d.reanimateBears(me)
        d.pendingDecision shouldBe null
        (bears in d.state.getBattlefield()) shouldBe true
        d.plusOneCounters(bears) shouldBe 0
    }

    test("a creature searched onto the battlefield (Green Sun's Zenith) gets the riot choice too") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Rhythm of the Wild")
        val bears = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val zenith = d.putCardInHand(me, "Green Sun's Zenith")
        d.giveMana(me, Color.GREEN, 3)
        d.castXSpell(me, zenith, xValue = 2).error shouldBe null
        d.bothPass()

        val search = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(bears)).error shouldBe null
        search.playerId shouldBe me
        d.chooseRiot(me, haste = false)

        (bears in d.state.getBattlefield()) shouldBe true
        d.plusOneCounters(bears) shouldBe 1
    }
})
