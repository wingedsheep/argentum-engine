package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.AssemblySpecialist
import com.wingedsheep.mtg.sets.definitions.trk.cards.BorgCube
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Borg Cube ({5}{W}{B} Artifact — Spacecraft, 6/6):
 * "Affinity for artifact creatures
 * Whenever this Spacecraft enters or attacks, assimilate a creature card exiled with it. If you can't,
 * exile up to one target creature.
 * Station
 * 12+ | Artifact creatures you control have flying."
 */
class BorgCubeScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(BorgCube, AssemblySpecialist))
        it.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.charge(id: EntityId, count: Int) {
        replaceState(state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.CHARGE, count))
        })
    }

    fun GameTestDriver.castCube(me: EntityId, generic: Int) {
        giveMana(me, Color.WHITE, 1)
        giveMana(me, Color.BLACK, 1)
        if (generic > 0) giveColorlessMana(me, generic)
        castSpell(me, putCardInHand(me, "Borg Cube")).error shouldBe null
        bothPass()
    }

    test("entering with nothing exiled, it exiles the target creature; attacking later assimilates it") {
        val d = newDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        d.castCube(me, 5)
        val cube = d.findPermanent(me, "Borg Cube")!!
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(me, listOf(bears)).error shouldBe null
        d.bothPass()
        withClue("no creature card was exiled with it, so the target is exiled") {
            d.state.getExile(opp) shouldContain bears
        }

        // Fully stationed: it is a 6/6 artifact creature that can attack next turn.
        d.charge(cube, 12)
        d.passPriorityUntil(Step.UPKEEP, opp)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, me)
        d.declareAttackers(me, listOf(cube), opp).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(me, listOf(courser)).error shouldBe null
        d.bothPass()
        (d.pendingDecision as? SelectCardsDecision)?.let { d.submitCardSelection(me, listOf(bears)) }

        withClue("the exiled Bears are assimilated, so the target is left alone") {
            (bears in d.state.getBattlefield()) shouldBe true
            (courser in d.state.getBattlefield()) shouldBe true
        }
        val projected = d.state.projectedState
        projected.getController(bears) shouldBe me
        projected.getTypes(bears) shouldContainAll setOf("ARTIFACT", "CREATURE")
        projected.getSubtypes(bears) shouldContain "Borg"
        projected.getSubtypes(bears) shouldNotContain "Bear"
        d.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        projected.getPower(bears) shouldBe 3
        withClue("at 12+ artifact creatures you control have flying") {
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            projected.hasKeyword(cube, Keyword.FLYING) shouldBe true
        }
    }

    test("below 12 charge counters it is no creature and grants no flying") {
        val d = newDriver()
        val me = d.activePlayer!!
        d.castCube(me, 5)
        if (d.pendingDecision is ChooseTargetsDecision) d.submitTargetSelection(me, emptyList())
        d.bothPass()
        val cube = d.findPermanent(me, "Borg Cube")!!
        val specialist = d.putCreatureOnBattlefield(me, "Assembly Specialist")
        d.charge(cube, 11)

        d.state.projectedState.isCreature(cube) shouldBe false
        d.state.projectedState.hasKeyword(specialist, Keyword.FLYING) shouldBe false
    }

    test("affinity for artifact creatures: each artifact creature you control makes it cost {1} less") {
        val d = newDriver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Assembly Specialist")
        d.putCreatureOnBattlefield(me, "Assembly Specialist")

        // Two artifact creatures: {W}{B} plus three generic is enough.
        d.castCube(me, 3)

        (d.findPermanent(me, "Borg Cube") != null) shouldBe true
    }
})
