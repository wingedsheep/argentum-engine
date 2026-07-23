package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.AleshaWhoLaughsAtFate
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Alesha, Who Laughs at Fate {1}{B}{R} — Legendary Creature — Human Warrior 2/2
 *   First strike
 *   Whenever Alesha attacks, put a +1/+1 counter on it.
 *   Raid — At the beginning of your end step, if you attacked this turn, return target creature
 *   card with mana value less than or equal to Alesha's power from your graveyard to the
 *   battlefield.
 *
 * Proves the attack trigger stacks a counter, that the raid end-step trigger only fires after an
 * attack, and that the graveyard target list is capped by Alesha's *current* power — so the
 * counter she just got from attacking widens what she can reanimate.
 */
class AleshaWhoLaughsAtFateScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AleshaWhoLaughsAtFate))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("attacking puts a +1/+1 counter on Alesha, and raid reanimates within her new power") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val alesha = driver.putCreatureOnBattlefield(me, "Alesha, Who Laughs at Fate")
        driver.removeSummoningSickness(alesha)

        // MV 3 — out of reach for a 2/2 Alesha, in reach once the attack trigger makes her 3/3.
        val courser = driver.putCardInGraveyard(me, "Centaur Courser")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(alesha), defendingPlayer = opponent).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        plusCounters(driver, alesha) shouldBe 1
        driver.state.projectedState.getPower(alesha) shouldBe 3

        // Walk into the end step; the raid trigger goes on the stack and asks for a target.
        var safety = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && safety < 60) {
            driver.bothPass()
            safety++
        }
        val decision = driver.state.pendingDecision
        (decision is ChooseTargetsDecision) shouldBe true

        driver.submitTargetSelection(me, listOf(courser))
        while (driver.stackSize > 0) driver.bothPass()

        driver.getPermanents(me).contains(courser) shouldBe true
        driver.getGraveyard(me).contains(courser) shouldBe false
    }

    test("no attack this turn: the raid end-step trigger never fires") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val alesha = driver.putCreatureOnBattlefield(me, "Alesha, Who Laughs at Fate")
        driver.removeSummoningSickness(alesha)
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        var safety = 0
        while (driver.state.step != Step.END && safety < 60) {
            (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe false
            driver.bothPass()
            safety++
        }
        // One more round of passes inside the end step — still no trigger.
        driver.bothPass()

        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe false
        driver.getGraveyard(me).contains(lions) shouldBe true
        plusCounters(driver, alesha) shouldBe 0
    }
})
