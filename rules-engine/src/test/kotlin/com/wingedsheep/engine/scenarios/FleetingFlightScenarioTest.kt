package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.FleetingFlight
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fleeting Flight — {W} instant:
 * "Put a +1/+1 counter on target creature. It gains flying until end of turn.
 *  Prevent all combat damage that would be dealt to it this turn."
 *
 * Exercises [com.wingedsheep.sdk.dsl.Effects.PreventAllCombatDamageTo]: the shield is
 * combat-only (noncombat damage still lands) and one-sided (the shielded creature still
 * deals its own combat damage).
 */
class FleetingFlightScenarioTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FleetingFlight))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun GameTestDriver.castFleetingFlight(player: EntityId, creature: EntityId) {
        giveMana(player, Color.WHITE, 1)
        val spell = putCardInHand(player, "Fleeting Flight")
        castSpellWithTargets(player, spell, listOf(ChosenTarget.Permanent(creature)))
        bothPass()
        resolveStack(this)
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.markedDamage(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("puts a +1/+1 counter on the target and grants flying until end of turn") {
        val (driver, you) = newGame()
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3

        driver.castFleetingFlight(you, courser)

        driver.plusOneCounters(courser) shouldBe 1
        driver.state.projectedState.hasKeyword(courser, Keyword.FLYING) shouldBe true

        // Flying is gone after the turn ends; the counter persists.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.projectedState.hasKeyword(courser, Keyword.FLYING) shouldBe false
        driver.plusOneCounters(courser) shouldBe 1
    }

    test("prevents combat damage dealt to the target, but the target still deals combat damage") {
        val (driver, you) = newGame()
        val opponent = driver.state.turnOrder.first { it != you }
        val attacker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        val blocker = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(attacker)

        // Advance to the opponent's turn and have them attack.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(opponent, listOf(attacker), defendingPlayer = you).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(you, mapOf(blocker to listOf(attacker)))

        // With blockers declared, shield the blocker (4/4 flying, combat damage to it prevented).
        while (driver.state.priorityPlayerId != null && driver.state.priorityPlayerId != you) {
            driver.passPriority(driver.state.priorityPlayerId!!)
        }
        driver.castFleetingFlight(you, blocker)

        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        resolveStack(driver)

        // The shielded blocker takes no combat damage; the attacker takes 4 and dies.
        driver.markedDamage(blocker) shouldBe 0
        driver.state.getBattlefield().contains(blocker) shouldBe true
        driver.state.getBattlefield().contains(attacker) shouldBe false
    }

    test("does not prevent noncombat damage") {
        val (driver, you) = newGame()
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3

        driver.castFleetingFlight(you, courser) // 4/4, combat-only shield

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Permanent(courser)))
        driver.bothPass()
        resolveStack(driver)

        driver.markedDamage(courser) shouldBe 3 // noncombat damage still lands (4/4 survives)
    }
})
