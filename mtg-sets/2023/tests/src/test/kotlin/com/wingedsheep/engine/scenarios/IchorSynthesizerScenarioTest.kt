package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.IchorSynthesizer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ichor Synthesizer (ONE #55) — {1}{U} 1/3 Creature — Phyrexian Wizard.
 *
 * "Whenever you cast a noncreature spell, put an oil counter on this creature.
 *  As long as this creature has four or more oil counters on it, it gets +2/+0 and can't be blocked."
 */
class IchorSynthesizerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IchorSynthesizer))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun setOil(driver: GameTestDriver, id: EntityId, count: Int) {
        driver.replaceState(driver.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.OIL to count))) })
    }

    test("casting a noncreature spell adds an oil counter; a creature spell does not") {
        val driver = newDriver()
        val p1 = driver.player1
        val opp = driver.getOpponent(p1)
        val synth = driver.putCreatureOnBattlefield(p1, "Ichor Synthesizer")

        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, bolt, listOf(opp)).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the trigger
        oil(driver, synth) shouldBe 1
        driver.bothPass() // resolve the bolt

        val bears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveColorlessMana(p1, 1)
        driver.castSpell(p1, bears).outcome shouldBe Outcome.Done
        driver.bothPass()
        oil(driver, synth) shouldBe 1
    }

    test("with three oil counters it is a 1/3 that can be blocked") {
        val driver = newDriver()
        val p1 = driver.player1
        val opp = driver.getOpponent(p1)
        val synth = driver.putCreatureOnBattlefield(p1, "Ichor Synthesizer")
        driver.removeSummoningSickness(synth)
        setOil(driver, synth, 3)
        driver.state.projectedState.getPower(synth) shouldBe 1
        driver.state.projectedState.getToughness(synth) shouldBe 3

        val blocker = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(synth), opp).error shouldBe null
        driver.bothPass()
        driver.declareBlockers(opp, mapOf(blocker to listOf(synth))).error shouldBe null
    }

    test("the fourth oil counter makes it a 3/3 that can't be blocked") {
        val driver = newDriver()
        val p1 = driver.player1
        val opp = driver.getOpponent(p1)
        val synth = driver.putCreatureOnBattlefield(p1, "Ichor Synthesizer")
        driver.removeSummoningSickness(synth)
        setOil(driver, synth, 3)

        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, bolt, listOf(opp)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.bothPass()
        oil(driver, synth) shouldBe 4
        driver.state.projectedState.getPower(synth) shouldBe 3
        driver.state.projectedState.getToughness(synth) shouldBe 3

        val blocker = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(synth), opp).error shouldBe null
        driver.bothPass()
        driver.declareBlockers(opp, mapOf(blocker to listOf(synth))).error shouldNotBe null
    }
})
