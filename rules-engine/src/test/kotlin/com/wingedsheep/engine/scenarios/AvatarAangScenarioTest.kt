package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.AvatarTheLastAirbenderSet
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Avatar Aang // Aang, Master of Elements (TLA 207).
 *
 * Front — "Whenever you waterbend, earthbend, firebend, or airbend, draw a card. Then if you've
 * done all four this turn, transform Avatar Aang." Back — "{W}{U}{B}{R}{G} less" cost reduction and
 * an upkeep "you may transform … If you do, gain 4 life, draw four cards, put four +1/+1 counters
 * on him, and he deals 4 damage to each opponent."
 *
 * The bend event/trigger/tracker machinery itself is proven in FourBendEventScenarioTest; here we
 * pin Aang's own wiring: draw-per-bend, all-four transform, the {WUBRG} reduction reaching zero, and
 * the upkeep payoff.
 */
class AvatarAangScenarioTest : FunSpec({

    fun bendSpell(name: String, type: BendType) = card(name) {
        manaCost = "{0}"; typeLine = "Sorcery"; oracleText = "You ${type.oracleVerb}."
        spell { effect = Effects.EmitBend(type) }
    }
    val DoWaterbend = bendSpell("Do Waterbend", BendType.WATER)
    val DoEarthbend = bendSpell("Do Earthbend", BendType.EARTH)
    val DoFirebend = bendSpell("Do Firebend", BendType.FIRE)
    val DoAirbend = bendSpell("Do Airbend", BendType.AIR)
    // {5} vanilla creature — with Aang, Master of Elements the {WUBRG} overflow reduces its whole
    // generic cost to {0}, so it casts with no mana at all.
    val fiveDrop = card("Five Drop Dummy") {
        manaCost = "{5}"; typeLine = "Creature — Golem"; power = 3; toughness = 3
    }

    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all + AvatarTheLastAirbenderSet.cards +
                listOf(DoWaterbend, DoEarthbend, DoFirebend, DoAirbend, fiveDrop)
        )
        return d
    }

    fun GameTestDriver.resolveStack() { var g = 0; while (state.stack.isNotEmpty() && g++ < 40) bothPass() }
    fun GameTestDriver.castBend(pid: EntityId, name: String) {
        castSpell(pid, putCardInHand(pid, name)); resolveStack()
    }
    fun GameTestDriver.face(id: EntityId): DoubleFacedComponent.Face =
        state.getEntity(id)?.get<DoubleFacedComponent>()?.currentFace ?: error("not a DFC")
    fun GameTestDriver.drawnThisTurn(pid: EntityId): Int =
        state.getEntity(pid)?.get<CardsDrawnThisTurnComponent>()?.count ?: 0
    fun GameTestDriver.plusOne(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("all four bends this turn draw four cards and transform Avatar Aang") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val aang = d.putCreatureOnBattlefield(me, "Avatar Aang")
        d.face(aang) shouldBe DoubleFacedComponent.Face.FRONT

        val drawnBefore = d.drawnThisTurn(me)
        d.castBend(me, "Do Waterbend")
        d.castBend(me, "Do Earthbend")
        d.castBend(me, "Do Firebend")
        d.face(aang) shouldBe DoubleFacedComponent.Face.FRONT      // only three distinct so far
        d.castBend(me, "Do Airbend")

        d.drawnThisTurn(me) - drawnBefore shouldBe 4               // one card per bend
        d.face(aang) shouldBe DoubleFacedComponent.Face.BACK       // fourth distinct bend transforms
    }

    test("fewer than four distinct bends draw a card each but do not transform") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val aang = d.putCreatureOnBattlefield(me, "Avatar Aang")

        val drawnBefore = d.drawnThisTurn(me)
        d.castBend(me, "Do Waterbend")
        d.castBend(me, "Do Earthbend")
        d.castBend(me, "Do Waterbend")                             // repeat — still two distinct

        d.drawnThisTurn(me) - drawnBefore shouldBe 3              // fires per bend, not per distinct type
        d.face(aang) shouldBe DoubleFacedComponent.Face.FRONT
    }

    test("Aang, Master of Elements reduces your spells' cost by {W}{U}{B}{R}{G} (overflowing to generic)") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val aang = d.putCreatureOnBattlefield(me, "Avatar Aang")
        // Flip to the back via all four bends.
        d.castBend(me, "Do Waterbend"); d.castBend(me, "Do Earthbend")
        d.castBend(me, "Do Firebend"); d.castBend(me, "Do Airbend")
        d.face(aang) shouldBe DoubleFacedComponent.Face.BACK

        // {5} generic, no mana available: castable only because {WUBRG} overflows to a {5} reduction.
        val dummy = d.putCardInHand(me, "Five Drop Dummy")
        d.castSpell(me, dummy).error shouldBe null
        d.state.stack.any { it == dummy } shouldBe true
    }

    test("at each upkeep you may transform back, gaining life, cards, counters, and dealing damage") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val aang = d.putCreatureOnBattlefield(me, "Avatar Aang")
        d.castBend(me, "Do Waterbend"); d.castBend(me, "Do Earthbend")
        d.castBend(me, "Do Firebend"); d.castBend(me, "Do Airbend")
        d.face(aang) shouldBe DoubleFacedComponent.Face.BACK

        val myLifeBefore = d.getLifeTotal(me)
        val oppLifeBefore = d.getLifeTotal(opp)

        // Advance to the next upkeep; answer "yes" to the may-transform. Never bothPass while a
        // decision is pending (that would auto-decline it) — handle each pause. Aang's draws overfill
        // the hand, so a cleanup discard-to-hand-size lands before the upkeep trigger.
        var guard = 0
        var answered = false
        while (guard++ < 300 && !answered) {
            if (d.isPaused) {
                when (val dec = d.pendingDecision) {
                    is YesNoDecision -> {
                        d.submitDecision(dec.playerId, YesNoResponse(dec.id, true)); answered = true
                    }
                    is SelectCardsDecision -> d.submitDecision(
                        dec.playerId, CardsSelectedResponse(dec.id, dec.options.take(dec.minSelections))
                    )
                    else -> error("unexpected decision before upkeep may-transform: $dec")
                }
            } else {
                d.bothPass()
            }
        }
        answered shouldBe true
        var drain = 0
        while (drain++ < 20 && !d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.face(aang) shouldBe DoubleFacedComponent.Face.FRONT     // transformed back to the front
        d.getLifeTotal(me) shouldBe myLifeBefore + 4              // gain 4 life
        d.getLifeTotal(opp) shouldBe oppLifeBefore - 4           // 4 damage to each opponent
        d.plusOne(aang) shouldBe 4                                // four +1/+1 counters
        d.drawnThisTurn(me) shouldBe 4                            // draw four (turn-start reset + 4)
    }
})
