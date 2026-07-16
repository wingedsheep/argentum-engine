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
 * Savior of Ollenbock (VOW #34) — {1}{W}{W} 1/2 Creature — Human Soldier, Training + a
 * train-triggered exile that returns on leave.
 *
 * The full payoff loop (CR 702.149c "trains"):
 *  - Training puts a +1/+1 counter when the Savior (power 1) attacks alongside a greater-power
 *    creature. That counter placement emits the parameterless `TrainedEvent` — the Savior "trains".
 *  - "Whenever this creature trains, exile up to one other target creature from the battlefield or
 *    creature card from a graveyard." ([Effects.ExileUntilLeaves] links the exiled card to the
 *    Savior.) "Up to one" means it can also exile nothing.
 *  - "When this creature leaves the battlefield, put the exiled cards onto the battlefield under
 *    their owners' control." ([Effects.ReturnLinkedExileUnderOwnersControl] — back to the
 *    battlefield, unlike Fear of Abduction which returns to hand.)
 *
 * Proven both ways: train → exile an opponent's creature → destroy the Savior → the creature returns
 * to the battlefield under its owner's control; and the "up to one" path where training exiles
 * nothing.
 */
class SaviorOfOllenbockScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), startingLife = 20)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Resolve until the trains-trigger asks for its exile target (a [ChooseTargetsDecision]). */
    fun GameTestDriver.resolveUntilTargetChoice(): ChooseTargetsDecision {
        var guard = 0
        while (guard++ < 40) {
            val decision = pendingDecision
            if (decision is ChooseTargetsDecision) return decision
            if (stackSize > 0 || decision != null) bothPass() else break
        }
        error("expected the trains-trigger to ask for its exile target; none appeared")
    }

    test("train exiles an opponent's creature; destroying the Savior returns it to the battlefield") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val savior = driver.putCreatureOnBattlefield(me, "Savior of Ollenbock")     // power 1, training
        val centaur = driver.putCreatureOnBattlefield(me, "Centaur Courser")        // power 3 (greater)
        listOf(savior, centaur).forEach { driver.removeSummoningSickness(it) }
        val victim = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")          // the exile target

        // Attack alongside the Centaur → the Savior trains (one +1/+1 counter), which fires the
        // "whenever this creature trains" exile trigger.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(savior, centaur), opp)

        driver.resolveUntilTargetChoice()
        driver.submitTargetSelection(me, listOf(victim))

        // Drain the rest of the combat triggers.
        var guard = 0
        while ((driver.stackSize > 0 || driver.pendingDecision != null) && guard++ < 20) {
            if (driver.pendingDecision is ChooseTargetsDecision) break
            driver.bothPass()
        }

        driver.plusOneCounters(savior) shouldBe 1
        // The opponent's creature is exiled (not on the battlefield).
        driver.getExileCardNames(opp).contains("Grizzly Bears") shouldBe true
        driver.getPermanents(opp).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe false

        // Kill the Savior with Doom Blade in the postcombat main → its leaves trigger returns the
        // linked-exiled creature to the battlefield under its owner's (the opponent's) control.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val doomBlade = driver.putCardInHand(me, "Doom Blade")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, doomBlade, targets = listOf(savior)).isSuccess shouldBe true
        while ((driver.stackSize > 0 || driver.pendingDecision != null) && guard++ < 60) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }

        driver.getGraveyard(me).any { driver.getCardName(it) == "Savior of Ollenbock" } shouldBe true
        // The exiled creature comes back to the battlefield under its owner's control.
        driver.getPermanents(opp).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
    }

    test("training may exile nothing (\"up to one\") — the Savior still gets its counter") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val savior = driver.putCreatureOnBattlefield(me, "Savior of Ollenbock")
        val centaur = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        listOf(savior, centaur).forEach { driver.removeSummoningSickness(it) }
        val bystander = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(savior, centaur), opp)

        driver.resolveUntilTargetChoice()
        // "Up to one" — submit no target.
        driver.submitTargetSelection(me, emptyList())

        var guard = 0
        while ((driver.stackSize > 0 || driver.pendingDecision != null) && guard++ < 20) {
            if (driver.pendingDecision is ChooseTargetsDecision) break
            driver.bothPass()
        }

        driver.plusOneCounters(savior) shouldBe 1
        // Nothing was exiled — the bystander is untouched.
        driver.getPermanents(opp).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
    }
})
