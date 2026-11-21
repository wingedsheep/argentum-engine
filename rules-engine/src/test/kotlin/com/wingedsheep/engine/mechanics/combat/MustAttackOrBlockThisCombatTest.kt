package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.combat.MarkMustAttackExecutor
import com.wingedsheep.engine.handlers.effects.combat.MarkMustBlockExecutor
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.MarkMustAttackEffect
import com.wingedsheep.sdk.scripting.effects.MarkMustBlockEffect
import com.wingedsheep.sdk.scripting.effects.MustAttackWindow
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Combat-scoped requirements — `MarkMustAttackEffect(window = MustAttackWindow.ThisCombat)` and
 * `MarkMustBlockEffect(duration = Duration.EndOfCombat)`: "target creature attacks or blocks this
 * combat if able" (Boros Battleshaper). Each binds the declaration of the combat phase in progress
 * and is gone once that combat phase ends, so a later combat in the same turn is free of it.
 * Requirements are never guarantees (CR 508.1d / 509.1c): a creature that can't attack or block is
 * simply excused.
 */
class MustAttackOrBlockThisCombatTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.markAttack(creature: EntityId, by: EntityId) {
        val effect = MarkMustAttackEffect(EffectTarget.SpecificEntity(creature), MustAttackWindow.ThisCombat)
        replaceState(MarkMustAttackExecutor().execute(state, effect, EffectContext(sourceId = null, controllerId = by)).state)
    }

    fun GameTestDriver.markBlock(creature: EntityId, by: EntityId, duration: Duration = Duration.EndOfCombat) {
        val effect = MarkMustBlockEffect(EffectTarget.SpecificEntity(creature), duration)
        replaceState(MarkMustBlockExecutor().execute(state, effect, EffectContext(sourceId = null, controllerId = by)).state)
    }

    test("a creature marked to attack this combat must attack in that declaration") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(mine)
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.markAttack(mine, by = driver.player2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, emptyMap()).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(mine to driver.player2)).error shouldBe null
    }

    test("the attack requirement ends with the combat phase") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(mine)
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.markAttack(mine, by = driver.player2)
        driver.state.projectedState.mustAttack(mine) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, mapOf(mine to driver.player2)).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.state.projectedState.mustAttack(mine) shouldBe false
        driver.state.floatingEffects shouldBe emptyList()
    }

    test("a creature that can't attack this combat is excused") {
        val driver = newDriver()
        // Summoning sick: it can't attack, so the empty declaration stands. The other creature can
        // attack, so the declare-attackers step is not skipped.
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        val other = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(other)
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.markAttack(mine, by = driver.player2)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
    }

    test("a creature marked to block this combat must block, and the requirement ends with the combat") {
        val driver = newDriver()
        val attacker = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(attacker)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.markBlock(blocker, by = driver.player1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, mapOf(attacker to driver.player2)).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(driver.player2).error shouldNotBe null
        driver.declareBlockers(driver.player2, mapOf(blocker to listOf(attacker))).error shouldBe null

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.state.projectedState.mustBlock(blocker) shouldBe false
    }

    test("a this-turn block mark outlives the combat phase") {
        val driver = newDriver()
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.markBlock(blocker, by = driver.player1, duration = Duration.EndOfTurn)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.state.projectedState.mustBlock(blocker) shouldBe true
    }

    test("the this-combat window names no defender, and block marks default to this turn") {
        shouldThrow<IllegalArgumentException> {
            MarkMustAttackEffect(EffectTarget.Self, MustAttackWindow.ThisCombat, defender = EffectTarget.Controller)
        }
        MarkMustBlockEffect(EffectTarget.Self).duration shouldBe Duration.EndOfTurn
    }
})
