package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.HarnessInfinity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Harness Infinity — "Exchange your hand and graveyard. Exile Harness Infinity."
 *
 * Both zones are gathered before either moves, so the old hand lands in the graveyard and only
 * the old graveyard comes back to hand; the spell exiles itself rather than joining either pile.
 */
class HarnessInfinityScenarioTest : FunSpec({
    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + HarnessInfinity)
        initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun castHarness(d: GameTestDriver, me: EntityId): EntityId {
        val harness = d.putCardInHand(me, "Harness Infinity")
        d.giveMana(me, Color.BLACK, 4)
        d.giveMana(me, Color.GREEN, 3)
        d.castSpell(me, harness).outcome shouldBe Outcome.Done
        d.bothPass()
        return harness
    }

    test("exchanges hand and graveyard, then exiles itself") {
        val d = setup()
        val me = d.activePlayer!!
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")
        val bolt = d.putCardInGraveyard(me, "Lightning Bolt")
        val oldHand = d.state.getZone(ZoneKey(me, Zone.HAND)).toList()

        val harness = castHarness(d, me)

        d.state.getZone(ZoneKey(me, Zone.HAND)) shouldContainExactlyInAnyOrder listOf(bears, bolt)
        d.state.getZone(ZoneKey(me, Zone.GRAVEYARD)) shouldContainExactlyInAnyOrder oldHand
        d.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain harness
    }

    test("with an empty graveyard the hand still goes to the graveyard") {
        val d = setup()
        val me = d.activePlayer!!
        val oldHand = d.state.getZone(ZoneKey(me, Zone.HAND)).toList()

        val harness = castHarness(d, me)

        d.state.getZone(ZoneKey(me, Zone.HAND)).isEmpty() shouldBe true
        d.state.getZone(ZoneKey(me, Zone.GRAVEYARD)) shouldContainExactlyInAnyOrder oldHand
        d.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain harness
    }
})
