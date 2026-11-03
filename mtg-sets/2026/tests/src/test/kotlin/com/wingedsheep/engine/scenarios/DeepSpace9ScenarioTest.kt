package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.DeepSpace9
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Deep Space 9 ({9} Legendary Artifact — Spacecraft):
 * "When Deep Space 9 enters and at the beginning of your upkeep, exile the top four cards of your
 * library. Put all permanent cards exiled this way onto the battlefield.
 * Station
 * 9+ | Creatures you control get +3/+3."
 */
class DeepSpace9ScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(DeepSpace9))
        it.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.charge(id: EntityId, count: Int) {
        replaceState(state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.CHARGE, count))
        })
    }

    /** Stacks the library so the top four are, from the top: Bolt, Courser, Giant Growth, Forest. */
    fun GameTestDriver.stackTopFour(me: EntityId): List<EntityId> {
        val forest = putCardOnTopOfLibrary(me, "Forest")
        val growth = putCardOnTopOfLibrary(me, "Giant Growth")
        val courser = putCardOnTopOfLibrary(me, "Centaur Courser")
        val bolt = putCardOnTopOfLibrary(me, "Lightning Bolt")
        return listOf(bolt, courser, growth, forest)
    }

    test("entering exiles the top four and puts the permanent cards onto the battlefield") {
        val d = newDriver()
        val me = d.activePlayer!!
        val (bolt, courser, growth, forest) = d.stackTopFour(me)
        val fifth = d.state.getLibrary(me)[4]

        d.giveColorlessMana(me, 9)
        d.castSpell(me, d.putCardInHand(me, "Deep Space 9")).error shouldBe null
        d.bothPass() // resolve the spell
        d.bothPass() // resolve the enters trigger

        val battlefield = d.state.getBattlefield()
        withClue("the permanent cards exiled this way enter under your control") {
            battlefield shouldContainAll listOf(courser, forest)
            d.state.projectedState.getController(courser) shouldBe me
            d.state.projectedState.getController(forest) shouldBe me
        }
        withClue("the nonpermanent cards stay in exile") {
            d.getExile(me) shouldContainAll listOf(bolt, growth)
            battlefield shouldNotContain bolt
            battlefield shouldNotContain growth
        }
        withClue("only four cards were exiled") {
            d.state.getLibrary(me).first() shouldBe fifth
        }
    }

    test("at the beginning of your upkeep it exiles the top four again") {
        val d = newDriver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Deep Space 9")

        d.passPriorityUntil(Step.UPKEEP, d.getOpponent(me))
        val (bolt, courser, growth, forest) = d.stackTopFour(me)
        d.passPriorityUntil(Step.UPKEEP, me)
        d.bothPass() // resolve the upkeep trigger

        d.state.getBattlefield() shouldContainAll listOf(courser, forest)
        d.getExile(me) shouldContainAll listOf(bolt, growth)
    }

    test("at 9+ charge counters creatures you control get +3/+3; below 9 they don't") {
        val d = newDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val station = d.putPermanentOnBattlefield(me, "Deep Space 9")
        val mine = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val theirs = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        d.charge(station, 8)
        d.state.projectedState.getPower(mine) shouldBe 3
        withClue("it has no power/toughness box, so it never becomes a creature") {
            d.state.projectedState.isCreature(station) shouldBe false
        }

        d.charge(station, 1)
        val projected = d.state.projectedState
        projected.getPower(mine) shouldBe 6
        projected.getToughness(mine) shouldBe 6
        projected.getPower(theirs) shouldBe 3
        projected.isCreature(station) shouldBe false
    }
})
