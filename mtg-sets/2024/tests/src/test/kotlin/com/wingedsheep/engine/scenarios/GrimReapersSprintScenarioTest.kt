package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.StepChangedEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.GrimReapersSprint
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Grim Reaper's Sprint {4}{R} — Enchantment — Aura
 *   Morbid — This spell costs {3} less to cast if a creature died this turn.
 *   Enchant creature
 *   When this Aura enters, untap each creature you control. If it's your main phase, there is an
 *   additional combat phase after this phase.
 *   Enchanted creature gets +2/+2 and has haste.
 *
 * Pins the turn order "after this phase" produces: cast precombat, the extra combat comes straight
 * after the precombat main phase and the natural combat and postcombat main follow it; cast
 * postcombat, the extra combat follows that main phase and the turn goes to its end step — the Aura
 * never adds a main phase of its own.
 */
class GrimReapersSprintScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + GrimReapersSprint)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /** Cast the Aura on [host] at full cost and let it and its enters trigger resolve. */
    fun GameTestDriver.castSprint(caster: EntityId, host: EntityId) {
        val aura = putCardInHand(caster, "Grim Reaper's Sprint")
        giveMana(caster, Color.RED, 5)
        castSpell(caster, aura, targets = listOf(host)).error shouldBe null
        var guard = 0
        while (guard++ < 10 && stackSize > 0) bothPass()
    }

    /** Pass through the rest of the active player's turn; the combat/main/end steps entered, in order. */
    fun GameTestDriver.phaseMarkersThroughTurn(): List<Step> {
        val startPlayer = activePlayer
        val firstEventIndex = events.size
        var guard = 0
        while (activePlayer == startPlayer && guard++ < 400) bothPass()
        return events.drop(firstEventIndex)
            .filterIsInstance<StepChangedEvent>()
            .map { it.newStep }
            .filter { it == Step.BEGIN_COMBAT || it == Step.POSTCOMBAT_MAIN || it == Step.END }
    }

    test("cast precombat: untaps your creatures, pumps the host, and the extra combat comes before the natural one") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val host = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val tapped = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.tapPermanent(tapped)

        driver.castSprint(me, host)

        driver.isTapped(tapped) shouldBe false
        val projected = driver.state.projectedState
        projected.getPower(host) shouldBe 4
        projected.getToughness(host) shouldBe 4
        projected.hasKeyword(host, Keyword.HASTE) shouldBe true

        driver.phaseMarkersThroughTurn() shouldContainExactly listOf(
            Step.BEGIN_COMBAT,    // the additional combat, right after the precombat main phase
            Step.BEGIN_COMBAT,    // the natural combat phase
            Step.POSTCOMBAT_MAIN, // the natural postcombat main phase
            Step.END
        )
    }

    test("cast postcombat: the extra combat follows that main phase, then the end step") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val host = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.castSprint(me, host)

        driver.phaseMarkersThroughTurn() shouldContainExactly listOf(
            Step.BEGIN_COMBAT, // the additional combat — no main phase after it
            Step.END
        )
    }
})
