package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.GnollHunter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pack tactics (Adventures in the Forgotten Realms) — "Whenever this creature attacks, if you
 * attacked with creatures with total power 6 or greater this combat, …", proven through Gnoll
 * Hunter (2/2, +1/+1 counter on itself).
 *
 * The engine models it as an attack trigger gated by
 * [com.wingedsheep.sdk.scripting.events.AttackPredicate.AttackersTotalPowerAtLeast], which sums the
 * declared attackers' projected power when the trigger is detected. The matrix:
 *  1. total below 6 → no trigger;
 *  2. total exactly 6, counting the pack tactics creature itself → trigger;
 *  3. a pump before declaration counts (projected power, not printed);
 *  4. a pump after declaration doesn't make it trigger (the total is read at declaration);
 *  5. once triggered, losing an attacker before resolution doesn't stop it.
 */
class PackTacticsTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GnollHunter)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), startingLife = 20)
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

    fun GameTestDriver.creatures(me: EntityId, vararg names: String): List<EntityId> =
        names.map { putCreatureOnBattlefield(me, it).also(::removeSummoningSickness) }

    test("does not trigger when the attackers' total power is below 6") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val (gnoll, centaur) = driver.creatures(me, "Gnoll Hunter", "Centaur Courser") // 2 + 3

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(gnoll, centaur), opp)
        driver.stackSize shouldBe 0
        driver.drainStack()

        driver.plusOneCounters(gnoll) shouldBe 0
    }

    test("triggers at exactly 6 total power, counting the pack tactics creature itself") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val attackers = driver.creatures(me, "Gnoll Hunter", "Centaur Courser", "Savannah Lions") // 2 + 3 + 1

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, attackers, opp)
        driver.stackSize shouldBe 1
        driver.drainStack()

        driver.plusOneCounters(attackers[0]) shouldBe 1
    }

    test("a pump before attackers are declared counts toward the total (projected power)") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val (gnoll, centaur) = driver.creatures(me, "Gnoll Hunter", "Centaur Courser")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val giantGrowth = driver.putCardInHand(me, "Giant Growth")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, giantGrowth, targets = listOf(centaur)).outcome shouldBe Outcome.Done
        driver.drainStack() // Centaur Courser is 6/6: total 8

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(gnoll, centaur), opp)
        driver.drainStack()

        driver.plusOneCounters(gnoll) shouldBe 1
    }

    test("a pump after attackers are declared does not make it trigger") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val (gnoll, centaur) = driver.creatures(me, "Gnoll Hunter", "Centaur Courser") // 5 at declaration

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(gnoll, centaur), opp)
        driver.stackSize shouldBe 0

        val giantGrowth = driver.putCardInHand(me, "Giant Growth")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, giantGrowth, targets = listOf(centaur)).outcome shouldBe Outcome.Done
        driver.drainStack() // total is now 8, but too late

        driver.plusOneCounters(gnoll) shouldBe 0
    }

    test("once triggered, it resolves even if an attacker leaves before resolution") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val attackers = driver.creatures(me, "Gnoll Hunter", "Centaur Courser", "Savannah Lions")
        val (gnoll, centaur) = attackers

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, attackers, opp)
        driver.stackSize shouldBe 1

        // In response, Lightning Bolt kills Centaur Courser: the attackers' total drops to 3.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(centaur)).outcome shouldBe Outcome.Done
        driver.drainStack()

        driver.getPermanents(me).contains(centaur) shouldBe false
        driver.plusOneCounters(gnoll) shouldBe 1
    }
})
