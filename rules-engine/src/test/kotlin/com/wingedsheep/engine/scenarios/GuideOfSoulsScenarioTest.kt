package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
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
 * Guide of Souls (MH3) — proves the two new pieces the card needed: the player-scoped Energy
 * "get 1 on another creature ETB" trigger, and [com.wingedsheep.sdk.scripting.effects.PayFixedCountersEffect]
 * as the all-or-nothing action half of a reflexive "may pay {E}{E}{E}. When you do, ..." ability.
 */
class GuideOfSoulsScenarioTest : FunSpec({

    val projector = StateProjector()

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        return d
    }

    fun energyOf(d: GameTestDriver, playerId: EntityId): Int =
        d.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun counterOf(d: GameTestDriver, entityId: EntityId, type: CounterType): Int =
        d.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun seedEnergy(d: GameTestDriver, playerId: EntityId, amount: Int) {
        d.replaceState(
            d.state.updateEntity(playerId) { container ->
                val current = container.get<CountersComponent>() ?: CountersComponent()
                container.with(current.withAdded(CounterType.ENERGY, amount))
            }
        )
    }

    /** Drain priority/triggers until a YesNoDecision surfaces (or we give up). */
    fun drainToYesNo(d: GameTestDriver, maxSteps: Int = 10): Boolean {
        repeat(maxSteps) {
            if (d.pendingDecision is YesNoDecision) return true
            if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }
        return d.pendingDecision is YesNoDecision
    }

    test("another creature entering under your control gains 1 life and 1 energy") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Guide of Souls")
        val lifeBefore = d.getLifeTotal(active)
        energyOf(d, active) shouldBe 0

        d.giveMana(active, Color.GREEN, 2) // Grizzly Bears costs {1}{G}
        val bearsId = d.putCardInHand(active, "Grizzly Bears")
        d.castSpell(active, bearsId)
        d.bothPass() // resolves Grizzly Bears, detects and queues the trigger
        d.bothPass() // resolves the queued triggered ability itself

        d.getLifeTotal(active) shouldBe lifeBefore + 1
        energyOf(d, active) shouldBe 1
    }

    test("paying {E}{E}{E} on attack puts 2 +1/+1 and a flying counter on the target and makes it an Angel") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Guide of Souls")
        val wurm = d.putCreatureOnBattlefield(active, "Craw Wurm")
        d.removeSummoningSickness(wurm)
        seedEnergy(d, active, 3)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(wurm), opp)

        drainToYesNo(d) shouldBe true
        d.submitYesNo(active, true)
        repeat(10) {
            if (d.pendingDecision != null && d.pendingDecision !is YesNoDecision) {
                d.submitTargetSelection(active, listOf(wurm))
            } else if (d.pendingDecision != null) {
                d.autoResolveDecision()
            } else {
                d.bothPass()
            }
        }

        val projected = projector.project(d.state)
        counterOf(d, wurm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        counterOf(d, wurm, CounterType.FLYING) shouldBe 1
        projected.hasSubtype(wurm, "Angel") shouldBe true
        energyOf(d, active) shouldBe 0
    }

    test("declining the may-pay leaves the attacker and energy untouched") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Guide of Souls")
        val wurm = d.putCreatureOnBattlefield(active, "Craw Wurm")
        d.removeSummoningSickness(wurm)
        seedEnergy(d, active, 3)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(wurm), opp)

        drainToYesNo(d) shouldBe true
        d.submitYesNo(active, false)
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        val projected = projector.project(d.state)
        counterOf(d, wurm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        projected.hasSubtype(wurm, "Angel") shouldBe false
        energyOf(d, active) shouldBe 3
    }

    test("with fewer than 3 energy, the may-pay prompt never appears at all") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Guide of Souls")
        val wurm = d.putCreatureOnBattlefield(active, "Craw Wurm")
        d.removeSummoningSickness(wurm)
        seedEnergy(d, active, 2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(wurm), opp)

        var sawMayPayPrompt = false
        repeat(10) {
            if (d.pendingDecision is YesNoDecision) sawMayPayPrompt = true
            if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }

        // The prompt must never appear at all — isActionFeasible gates it before offering the
        // "may pay" yes/no, not just before letting the payment go through.
        sawMayPayPrompt shouldBe false

        val projected = projector.project(d.state)
        counterOf(d, wurm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        projected.hasSubtype(wurm, "Angel") shouldBe false
        energyOf(d, active) shouldBe 2
    }

    test("attacker destroyed after the reflexive ability's target is locked in but before it " +
        "resolves causes it to fizzle (CR 603.12: a real stack object with its own priority window)") {
        // The "when you do" half of a reflexive trigger is a genuinely separate triggered ability
        // (CR 603.12) — it goes on the stack with its target already chosen, and opponents get a
        // real priority window to respond before it resolves. This proves that window actually
        // exists: the opponent kills the locked-in attacker in response, and CR 608.2b fizzles the
        // ability (illegal target) instead of it silently resolving anyway.
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Guide of Souls")
        val bears = d.putCreatureOnBattlefield(active, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        seedEnergy(d, active, 3)

        val boltId = d.putCardInHand(opp, "Lightning Bolt")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(bears), opp)

        drainToYesNo(d) shouldBe true
        d.submitYesNo(active, true)
        d.submitTargetSelection(active, listOf(bears))

        // Confirm the window actually exists rather than assuming it: the reflexive ability must
        // be a real, unresolved object on the stack right now, not already-applied counters.
        d.stackSize shouldBe 1
        counterOf(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0

        // Mana pools empty at each step change (CR 500.4), so the opponent's red mana is given only
        // now, right before they need it — not back when the board was set up.
        d.giveMana(opp, Color.RED, 1)

        // Active player passes priority on the newly-stacked ability; the opponent responds
        // instead of passing, Bolting the now-locked-in target before it resolves.
        d.passPriority(active)
        d.castSpell(opp, boltId, listOf(bears)).isSuccess shouldBe true
        d.bothPass() // resolve Lightning Bolt: 3 damage kills the 2/2 Grizzly Bears
        d.bothPass() // the reflexive ability tries to resolve; its only target is now illegal

        // Dying doesn't delete the entity — it moves to the graveyard — so check zone membership,
        // not existence, and confirm the reflexive payoff never touched it.
        d.state.getBattlefield().contains(bears) shouldBe false
        counterOf(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        // Energy was already spent when the action (paying {E}{E}{E}) completed — the fizzle only
        // affects the reflexive payoff, not the cost that was already paid.
        energyOf(d, active) shouldBe 0
    }
})
