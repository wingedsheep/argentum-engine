package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.RomulanBirdOfPrey
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Romulan Bird-of-Prey (TRK #77) — {1}{U} Artifact — Spacecraft, 4/4:
 * "Cloaking Device — As long as this Spacecraft isn't a creature, it has hexproof. Whenever you cast
 * a noncreature spell, if this Spacecraft isn't a creature, put two charge counters on it.
 * Station. 8+ | Flying"
 *
 * Pins the layer interaction: the hexproof grant (layer 6) and the trigger's intervening-if read the
 * Spacecraft's projected creature type (layer 4), so reaching 8 charge counters drops the hexproof
 * and switches the counter trigger off.
 */
class RomulanBirdOfPreyScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(RomulanBirdOfPrey))
        it.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.charge(ship: EntityId): Int =
        state.getEntity(ship)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    fun GameTestDriver.setCharge(ship: EntityId, count: Int) {
        replaceState(state.updateEntity(ship) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.CHARGE, count))
        })
    }

    /** Cast a Lightning Bolt at the opponent and let everything resolve. */
    fun GameTestDriver.boltOpponent(me: EntityId, opp: EntityId) {
        giveMana(me, Color.RED, 1)
        val bolt = putCardInHand(me, "Lightning Bolt")
        castSpell(me, bolt, listOf(opp)).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("uncharged it is a hexproof noncreature, and each noncreature spell adds two charge counters") {
        val d = newDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val ship = d.putPermanentOnBattlefield(me, "Romulan Bird-of-Prey")

        withClue("not a creature yet, so it has hexproof") {
            d.state.projectedState.isCreature(ship) shouldBe false
            d.state.projectedState.hasKeyword(ship, Keyword.HEXPROOF) shouldBe true
        }

        d.boltOpponent(me, opp)
        d.charge(ship) shouldBe 2
        d.boltOpponent(me, opp)
        d.charge(ship) shouldBe 4
    }

    test("at 8 charge counters it becomes a flying creature, loses hexproof, and stops charging") {
        val d = newDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val ship = d.putPermanentOnBattlefield(me, "Romulan Bird-of-Prey")
        d.setCharge(ship, 6)

        withClue("at 6 it is still a hexproof noncreature") {
            d.state.projectedState.isCreature(ship) shouldBe false
            d.state.projectedState.hasKeyword(ship, Keyword.HEXPROOF) shouldBe true
        }

        d.boltOpponent(me, opp)
        withClue("the trigger takes it from 6 to 8") { d.charge(ship) shouldBe 8 }

        withClue("stationed: a 4/4 flying artifact creature without hexproof") {
            d.state.projectedState.isCreature(ship) shouldBe true
            d.state.projectedState.hasKeyword(ship, Keyword.FLYING) shouldBe true
            d.state.projectedState.hasKeyword(ship, Keyword.HEXPROOF) shouldBe false
        }

        d.boltOpponent(me, opp)
        withClue("it is a creature now, so the intervening-if stops the trigger") { d.charge(ship) shouldBe 8 }
    }

    test("creature spells don't charge it") {
        val d = newDriver()
        val me = d.activePlayer!!
        val ship = d.putPermanentOnBattlefield(me, "Romulan Bird-of-Prey")

        d.giveMana(me, Color.WHITE, 1)
        val lions = d.putCardInHand(me, "Savannah Lions")
        d.castSpell(me, lions).error shouldBe null
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 10) d.bothPass()

        d.charge(ship) shouldBe 0
    }
})
