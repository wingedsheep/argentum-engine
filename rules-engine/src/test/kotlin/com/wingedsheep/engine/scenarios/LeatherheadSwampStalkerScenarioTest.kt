package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Leatherhead, Swamp Stalker (TMT #117) — {2}{G}{G} 5/4 Legendary Crocodile.
 *
 * "Trample
 *  Leatherhead enters with a hexproof counter on her.
 *  Whenever Leatherhead deals combat damage to a player, you may remove a counter from her.
 *  When you do, destroy target artifact or enchantment that player controls."
 *
 * Two things the wording pins down, and both have been wrong here:
 *
 *  - **"A counter" is any kind**, not the hexproof one specifically. Once something has put +1/+1
 *    counters on her (Ouroboroid's begin-combat trigger is the common case) the controller chooses
 *    which kind comes off — the implementation used to hardcode the hexproof counter and spend it
 *    with no prompt at all.
 *  - **The choice is which kind, not whether.** Once you accept the "you may", a counter comes off;
 *    CR 603.12's "when you do" is what arms the destroy, so a controller who takes off nothing must
 *    get nothing. Modelling this as `RemoveCountersUpTo(1, …)` — a ceiling with no floor — let the
 *    controller say yes and then answer 0 at every prompt, destroying an artifact every combat
 *    without ever spending a counter.
 */
class LeatherheadSwampStalkerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun addCounters(driver: GameTestDriver, entityId: EntityId, type: CounterType, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        }
        driver.replaceState(newState)
    }

    fun counts(driver: GameTestDriver, entityId: EntityId): Map<CounterType, Int> =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.counters ?: emptyMap()

    /**
     * Pass priority until the pending decision satisfies [matches], so the tests don't have to know
     * how many stack objects sit between combat damage and each prompt (the combat-damage trigger,
     * then the separate CR 603.12 reflexive trigger).
     */
    fun GameTestDriver.advanceUntil(what: String, matches: (Any?) -> Boolean): PendingDecision {
        var safety = 0
        while (!matches(pendingDecision) && safety++ < 12) bothPass()
        return pendingDecision?.takeIf { matches(it) }
            ?: error("expected $what, got ${pendingDecision?.let { it::class.simpleName }}")
    }

    fun GameTestDriver.advanceToYesNo(): YesNoDecision =
        advanceUntil("a YesNoDecision") { it is YesNoDecision } as YesNoDecision

    fun GameTestDriver.advanceToChooseTargets(): ChooseTargetsDecision =
        advanceUntil("a ChooseTargetsDecision") { it is ChooseTargetsDecision } as ChooseTargetsDecision

    /**
     * Attack unblocked with a Leatherhead carrying a hexproof counter and [plusOnes] +1/+1
     * counters, then say yes to the "you may remove a counter" prompt. Returns the driver
     * parked on whatever decision comes next (the counter-kind prompts).
     */
    fun attackAndAccept(plusOnes: Int): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20), startingLife = 20)

        val attacker = driver.player1
        val defender = driver.player2

        val leatherhead = driver.putCreatureOnBattlefield(attacker, "Leatherhead, Swamp Stalker")
        driver.removeSummoningSickness(leatherhead)
        addCounters(driver, leatherhead, CounterType.HEXPROOF, 1)
        if (plusOnes > 0) addCounters(driver, leatherhead, CounterType.PLUS_ONE_PLUS_ONE, plusOnes)

        driver.putPermanentOnBattlefield(defender, "Test Enchantment")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(leatherhead), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)

        val yesNo = driver.advanceToYesNo()
        driver.submitYesNo(yesNo.playerId, true)

        return Triple(driver, leatherhead, defender)
    }

    /**
     * Answer the sequence of per-kind counter prompts, taking one counter off [take] and zero off
     * every other kind. Stops when the prompts run out.
     */
    fun answerCounterPrompts(driver: GameTestDriver, take: CounterType) {
        val wanted = when (take) {
            CounterType.HEXPROOF -> "hexproof"
            CounterType.PLUS_ONE_PLUS_ONE -> "+1/+1"
            else -> error("unsupported counter kind for this test: $take")
        }
        var safety = 0
        while (safety++ < 10) {
            val decision = driver.pendingDecision as? ChooseNumberDecision ?: return
            val amount = if (decision.prompt.contains(wanted)) 1 else 0
            driver.submitDecision(decision.playerId, NumberChosenResponse(decision.id, amount))
        }
    }

    test("with only the hexproof counter, that is the counter removed") {
        val (driver, leatherhead, defender) = attackAndAccept(plusOnes = 0)

        answerCounterPrompts(driver, take = CounterType.HEXPROOF)

        (counts(driver, leatherhead)[CounterType.HEXPROOF] ?: 0) shouldBe 0

        val chooseTargets = driver.advanceToChooseTargets()
        val enchantment = driver.findPermanent(defender, "Test Enchantment")!!
        driver.submitTargetSelection(chooseTargets.playerId, listOf(enchantment))
        var safety = 0
        while (driver.findPermanent(defender, "Test Enchantment") != null && safety++ < 6) {
            driver.bothPass()
        }

        driver.findPermanent(defender, "Test Enchantment") shouldBe null
    }

    test("with +1/+1 counters too, the controller is prompted for which kind to remove") {
        val (driver, _, _) = attackAndAccept(plusOnes = 3)

        // The bug was that no prompt appeared at all — the hexproof counter was spent silently.
        (driver.pendingDecision is ChooseNumberDecision) shouldBe true
    }

    test("keeping hexproof: a +1/+1 counter comes off instead and the destroy still happens") {
        val (driver, leatherhead, defender) = attackAndAccept(plusOnes = 3)

        answerCounterPrompts(driver, take = CounterType.PLUS_ONE_PLUS_ONE)

        val after = counts(driver, leatherhead)
        after[CounterType.HEXPROOF] shouldBe 1
        after[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2

        val chooseTargets = driver.advanceToChooseTargets()
        val enchantment = driver.findPermanent(defender, "Test Enchantment")!!
        driver.submitTargetSelection(chooseTargets.playerId, listOf(enchantment))
        var safety = 0
        while (driver.findPermanent(defender, "Test Enchantment") != null && safety++ < 6) {
            driver.bothPass()
        }

        driver.findPermanent(defender, "Test Enchantment") shouldBe null
    }

    test("spending hexproof leaves the +1/+1 counters alone") {
        val (driver, leatherhead, _) = attackAndAccept(plusOnes = 3)

        answerCounterPrompts(driver, take = CounterType.HEXPROOF)

        val after = counts(driver, leatherhead)
        (after[CounterType.HEXPROOF] ?: 0) shouldBe 0
        after[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 3
    }

    test("answering zero at every prompt still spends a counter — the destroy is never free") {
        val (driver, leatherhead, defender) = attackAndAccept(plusOnes = 3)

        // Try to take nothing: answer 0 to every prompt, including any the floor has already
        // narrowed to a single legal answer. The engine holds the controller to the minimum.
        var safety = 0
        while (safety++ < 10) {
            val decision = driver.pendingDecision as? ChooseNumberDecision ?: break
            driver.submitDecision(decision.playerId, NumberChosenResponse(decision.id, 0))
        }

        // 4 counters went in; "remove a counter" means exactly one comes off, controller's pick.
        counts(driver, leatherhead).values.sum() shouldBe 3

        // And the payoff is genuinely earned, so it does resolve.
        val chooseTargets = driver.advanceToChooseTargets()
        val enchantment = driver.findPermanent(defender, "Test Enchantment")!!
        driver.submitTargetSelection(chooseTargets.playerId, listOf(enchantment))
        safety = 0
        while (driver.findPermanent(defender, "Test Enchantment") != null && safety++ < 6) {
            driver.bothPass()
        }
        driver.findPermanent(defender, "Test Enchantment") shouldBe null
    }

    test("with a single kind of counter there is nothing to ask, so no prompt is raised") {
        val (driver, leatherhead, _) = attackAndAccept(plusOnes = 0)

        // One kind, floor and ceiling both 1: the only legal answer is applied rather than asked.
        (driver.pendingDecision is ChooseNumberDecision) shouldBe false
        counts(driver, leatherhead).values.sum() shouldBe 0
    }

    test("no counters left: the may-clause is never offered and nothing is destroyed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20), startingLife = 20)

        val attacker = driver.player1
        val defender = driver.player2

        // Straight onto the battlefield, so the enters-with-a-hexproof-counter replacement never
        // runs — this is Leatherhead a combat after her last counter was spent.
        val leatherhead = driver.putCreatureOnBattlefield(attacker, "Leatherhead, Swamp Stalker")
        driver.removeSummoningSickness(leatherhead)
        counts(driver, leatherhead).values.sum() shouldBe 0

        driver.putPermanentOnBattlefield(defender, "Test Enchantment")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(leatherhead), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)

        var safety = 0
        var asked = false
        while (safety++ < 12) {
            if (driver.pendingDecision is YesNoDecision) { asked = true; break }
            driver.bothPass()
        }

        asked shouldBe false
        (driver.findPermanent(defender, "Test Enchantment") != null) shouldBe true
    }
})
