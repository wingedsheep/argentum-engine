package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Flayed Nim — Mirrodin #65, {3}{B} Creature — Skeleton 2/2
 *
 * Whenever this creature deals combat damage to a creature, that creature's controller loses
 * that much life.
 * {2}{B}: Regenerate this creature.
 *
 * The drain rider is the part worth proving. It is wired as
 * `LoseLife(ContextProperty(TRIGGER_DAMAGE_AMOUNT), ControllerOfTriggeringEntity)` on an
 * **outgoing** `DealsCombatDamageToCreature` trigger — a direction no other card exercises
 * (Tephraderm uses the same two primitives on the *incoming* side), so all three claims are
 * checked here:
 *
 *  - the life loss lands on the *damaged creature's* controller, not on the Nim's controller;
 *  - the amount is the damage **actually dealt**, not the Nim's printed power (a pumped Nim
 *    drains for more);
 *  - "that creature's controller" still resolves when the damaged creature died to the same
 *    combat damage, since the trigger reads last-known information.
 *
 * Blocked combat is used throughout so the defender takes no combat damage of their own —
 * every point of life they lose is the drain.
 */
class FlayedNimScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    /** Attack with [attacker] into a single [blocker], stopping once combat damage is dealt. */
    fun attackIntoBlocker(
        driver: GameTestDriver,
        attacker: EntityId,
        blocker: EntityId,
        attackingPlayer: EntityId,
        defendingPlayer: EntityId
    ) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attackingPlayer, listOf(attacker), defendingPlayer)
        driver.bothPass()
        driver.declareBlockers(defendingPlayer, mapOf(blocker to listOf(attacker)))
        driver.bothPass()
        // No first strikers, so the first-strike step is skipped entirely (CR 510.4).
        driver.currentStep shouldBe Step.COMBAT_DAMAGE
        // Resolve the drain trigger.
        driver.bothPass()
    }

    test("the damaged creature's controller loses life equal to the damage dealt") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nim = driver.putCreatureOnBattlefield(attacker, "Flayed Nim")
        driver.removeSummoningSickness(nim)
        // 3/3 — survives the Nim's 2 damage, so the drain is the only life change.
        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        attackIntoBlocker(driver, nim, blocker, attacker, defender)

        // Blocked, so no combat damage reached the defender — the 2 lost life is the drain.
        driver.assertLifeTotal(defender, 18)
        // The drain hits the *blocker's* controller, not the Nim's.
        driver.assertLifeTotal(attacker, 20)
    }

    test("drains for the damage actually dealt, not the Nim's printed power") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nim = driver.putCreatureOnBattlefield(attacker, "Flayed Nim")
        driver.removeSummoningSickness(nim)
        // 5/5 — survives a pumped Nim's 5 damage.
        val blocker = driver.putCreatureOnBattlefield(defender, "Force of Nature")
        driver.removeSummoningSickness(blocker)

        // Giant Growth the Nim to 5/5 before damage.
        val growth = driver.putCardInHand(attacker, "Giant Growth")
        driver.giveMana(attacker, Color.GREEN, 1)
        driver.castSpellWithTargets(attacker, growth, listOf(ChosenTarget.Permanent(nim)))
        driver.bothPass()

        attackIntoBlocker(driver, nim, blocker, attacker, defender)

        // 5 damage dealt -> 5 life lost. A hardcoded printed power would have drained 2.
        driver.assertLifeTotal(defender, 15)
        driver.assertLifeTotal(attacker, 20)
    }

    test("still drains when the damaged creature dies to that same combat damage") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nim = driver.putCreatureOnBattlefield(attacker, "Flayed Nim")
        driver.removeSummoningSickness(nim)
        // 1/1 — dies to the Nim's 2 damage, so "that creature's controller" must resolve from
        // last-known information by the time the trigger resolves.
        val blocker = driver.putCreatureOnBattlefield(defender, "Savannah Lions")
        driver.removeSummoningSickness(blocker)

        attackIntoBlocker(driver, nim, blocker, attacker, defender)

        driver.assertLifeTotal(defender, 18)
        driver.assertLifeTotal(attacker, 20)
    }

    test("does not drain when the Nim deals combat damage to a player") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nim = driver.putCreatureOnBattlefield(attacker, "Flayed Nim")
        driver.removeSummoningSickness(nim)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(nim), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)
        driver.bothPass()

        // 2 combat damage only — the trigger is scoped to damage dealt to a *creature*, so a
        // double-dip would show as 16.
        driver.assertLifeTotal(defender, 18)
    }
})
