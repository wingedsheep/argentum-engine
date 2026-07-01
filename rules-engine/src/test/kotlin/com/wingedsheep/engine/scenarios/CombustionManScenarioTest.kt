package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.CombustionMan
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Combustion Man (TLA) — {3}{R}{R} Legendary Creature — Human Assassin, 4/6.
 *
 * "Whenever Combustion Man attacks, destroy target permanent unless its controller has Combustion
 *  Man deal damage to them equal to his power."
 *
 * The pay-or-suffer choice is routed to the TARGET PERMANENT'S controller (a [ChooseActionEffect]
 * whose `player` is `EffectTarget.TargetController`). That player chooses between:
 *   • accepting the avoidance damage — Combustion Man deals damage equal to his power to them, the
 *     permanent survives; or
 *   • declining — the permanent is destroyed and no damage is dealt.
 * These tests pin both branches and confirm the avoidance damage scales with Combustion Man's
 * current power.
 */
class CombustionManScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + CombustionMan)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /**
     * Drive Combustion Man's attack against [defender], targeting [targetPermanent] with the trigger,
     * and resolve the stack until the targeted permanent's controller is asked to choose. Returns the
     * pending [ChooseOptionDecision].
     */
    fun GameTestDriver.attackAndReachChoice(
        attacker: EntityId,
        cm: EntityId,
        defender: EntityId,
        targetPermanent: EntityId
    ): ChooseOptionDecision {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attacker, listOf(cm), defender)
        // Combustion Man's controller chooses which permanent the trigger targets.
        submitTargetSelection(attacker, listOf(targetPermanent))

        var guard = 0
        while (pendingDecision !is ChooseOptionDecision && guard < 50) {
            if (state.stack.isEmpty() && pendingDecision == null) break
            bothPass()
            guard++
        }
        return pendingDecision as? ChooseOptionDecision
            ?: error("Expected a ChooseOptionDecision, but pending decision is $pendingDecision")
    }

    test("controller accepts the damage: they lose life equal to Combustion Man's power, permanent survives") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val cm = driver.putCreatureOnBattlefield(me, "Combustion Man")
        driver.removeSummoningSickness(cm)
        // The permanent the trigger targets — controlled by the opponent, so the opponent chooses.
        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val lifeBefore = driver.getLifeTotal(opponent)
        val decision = driver.attackAndReachChoice(me, cm, opponent, victim)
        decision.playerId shouldBe opponent

        // Option 0 = "Have Combustion Man deal damage to you equal to his power".
        driver.submitDecision(opponent, OptionChosenResponse(decision.id, 0))

        // Combustion Man (power 4) dealt 4 to the opponent; the permanent was not destroyed.
        driver.getLifeTotal(opponent) shouldBe lifeBefore - 4
        driver.getGraveyardCardNames(opponent).contains("Grizzly Bears") shouldBe false
    }

    test("controller declines: the targeted permanent is destroyed and no damage is dealt") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val cm = driver.putCreatureOnBattlefield(me, "Combustion Man")
        driver.removeSummoningSickness(cm)
        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val lifeBefore = driver.getLifeTotal(opponent)
        val decision = driver.attackAndReachChoice(me, cm, opponent, victim)
        decision.playerId shouldBe opponent

        // Option 1 = "Let the permanent be destroyed".
        driver.submitDecision(opponent, OptionChosenResponse(decision.id, 1))

        driver.getGraveyardCardNames(opponent).contains("Grizzly Bears") shouldBe true
        driver.getLifeTotal(opponent) shouldBe lifeBefore
    }

    test("avoidance damage scales with Combustion Man's current power") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val cm = driver.putCreatureOnBattlefield(me, "Combustion Man")
        driver.removeSummoningSickness(cm)
        // Pump Combustion Man to power 6 (4 base + two +1/+1 counters).
        driver.addComponent(cm, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        driver.state.projectedState.getPower(cm) shouldBe 6

        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val lifeBefore = driver.getLifeTotal(opponent)
        val decision = driver.attackAndReachChoice(me, cm, opponent, victim)
        driver.submitDecision(opponent, OptionChosenResponse(decision.id, 0))

        // Damage equals current power (6), not the printed 4.
        driver.getLifeTotal(opponent) shouldBe lifeBefore - 6
        driver.getGraveyardCardNames(opponent).contains("Grizzly Bears") shouldBe false
    }
})
