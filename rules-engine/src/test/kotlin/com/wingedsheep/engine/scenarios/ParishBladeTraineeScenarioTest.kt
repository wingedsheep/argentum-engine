package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Parish-Blade Trainee (VOW #29) — {1}{W} 1/2 Creature — Human Soldier, Training + a dies payoff.
 *
 * "When this creature dies, put its counters on target creature you control." The Trainee grows
 * itself with +1/+1 counters via Training (CR 702.149a), and those counters aren't wasted when it
 * dies — [Effects.MoveAllLastKnownCounters] reads them from last-known information (the permanent is
 * already in the graveyard when the dies trigger resolves) and relocates every counter to a target
 * creature the controller still has.
 *
 * Flow: train the Trainee in combat (one +1/+1 counter alongside a greater-power attacker), then
 * kill it with Doom Blade in the postcombat main phase. The dies trigger targets a surviving
 * creature — its selection is a [ChooseTargetsDecision], so it's driven explicitly (the shared
 * `autoResolveDecision` helper throws on target decisions).
 */
class ParishBladeTraineeScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), startingLife = 20)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.drainStack() {
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard < 20) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    test("training grows the Trainee, and its dies trigger moves those counters to a creature you control") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val trainee = driver.putCreatureOnBattlefield(me, "Parish-Blade Trainee")   // power 1, training
        val centaur = driver.putCreatureOnBattlefield(me, "Centaur Courser")        // power 3 (greater)
        val recipient = driver.putCreatureOnBattlefield(me, "Savannah Lions")       // survives to receive counters
        listOf(trainee, centaur, recipient).forEach { driver.removeSummoningSickness(it) }

        // Train the Trainee: attacking alongside the Centaur (power 3) puts one +1/+1 counter on it.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(trainee, centaur), opp)
        driver.drainStack()
        driver.plusOneCounters(trainee) shouldBe 1

        // Postcombat main: kill the Trainee with Doom Blade. Its dies trigger goes on the stack.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val doomBlade = driver.putCardInHand(me, "Doom Blade")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, doomBlade, targets = listOf(trainee)).isSuccess shouldBe true

        // Resolve Doom Blade + the ensuing dies trigger; pick the recipient when it asks for a target.
        var targeted = false
        var guard = 0
        while (!targeted && guard++ < 40) {
            when (driver.pendingDecision) {
                is ChooseTargetsDecision -> {
                    driver.submitTargetSelection(me, listOf(recipient)); targeted = true
                }
                else -> if (driver.stackSize > 0) driver.bothPass() else break
            }
        }
        targeted shouldBe true
        driver.drainStack()

        // The Trainee is dead; its +1/+1 counter moved to the recipient.
        driver.getGraveyard(me).any { driver.getCardName(it) == "Parish-Blade Trainee" } shouldBe true
        driver.plusOneCounters(recipient) shouldBe 1
    }
})
