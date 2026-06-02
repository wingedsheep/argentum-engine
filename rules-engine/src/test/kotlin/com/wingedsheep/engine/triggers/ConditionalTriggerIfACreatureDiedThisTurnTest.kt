package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.event.GlobalGrantedTriggeredAbility
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull

/**
 * BDD test: intervening-if 'a creature died this turn' gates a triggered ability on resolution check.
 *
 * Covers Rule 603.4: the condition is checked both when the trigger would fire (trigger time)
 * and again when the ability resolves (resolution time). The per-turn tracker is reset at cleanup.
 */
class ConditionalTriggerIfACreatureDiedThisTurnTest : FunSpec({

    fun buildDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun injectEndStepAbility(driver: GameTestDriver): GameTestDriver {
        val you = driver.player1
        val ability = TriggeredAbility.create(
            trigger = EventPattern.StepEvent(Step.END, Player.You),
            effect = GainLifeEffect(1),
            triggerCondition = CreatureDiedThisTurnCondition
        )
        val global = GlobalGrantedTriggeredAbility(
            ability = ability,
            controllerId = you,
            sourceId = you,
            sourceName = "Test Intervening-If Ability",
            duration = Duration.Permanent
        )
        driver.replaceState(driver.state.copy(globalGrantedTriggeredAbilities = listOf(global)))
        return driver
    }

    test("intervening-if predicate is false when no creature died this turn: ability does NOT go on the stack") {
        val driver = buildDriver()
        injectEndStepAbility(driver)

        // No creature has died this turn — CreaturesDiedThisTurnComponent is absent on player1
        driver.state.getEntity(driver.player1)?.get<CreaturesDiedThisTurnComponent>().shouldBeNull()

        driver.passPriorityUntil(Step.END)

        // Rule 603.4: trigger-time check fails → ability must NOT go on the stack
        driver.stackSize shouldBe 0
    }

    test("intervening-if predicate is true when a creature died this turn: ability goes on stack and the effect resolves exactly once") {
        val driver = buildDriver()
        injectEndStepAbility(driver)
        val you = driver.player1
        val lifeBefore = driver.getLifeTotal(you)

        // Simulate a creature having been put into the graveyard from the battlefield this turn
        driver.replaceState(
            driver.state.updateEntity(you) { container ->
                container.with(CreaturesDiedThisTurnComponent(count = 1))
            }
        )

        driver.passPriorityUntil(Step.END)

        // Rule 603.4: trigger-time check passes → ability must be on the stack
        driver.stackSize shouldBe 1

        // Resolve the triggered ability — condition re-checked at resolution (still true) → effect fires
        driver.bothPass()

        // GainLifeEffect(1) must have resolved exactly once
        driver.getLifeTotal(you) shouldBe lifeBefore + 1
    }

    test("intervening-if predicate is true when only an opponent's creature died this turn") {
        val driver = buildDriver()
        injectEndStepAbility(driver)
        val you = driver.player1
        val opponent = driver.player2
        val lifeBefore = driver.getLifeTotal(you)

        // Only the opponent has had a creature die — "a creature died this turn" is global
        driver.replaceState(
            driver.state.updateEntity(opponent) { container ->
                container.with(CreaturesDiedThisTurnComponent(count = 1))
            }
        )
        driver.state.getEntity(you)?.get<CreaturesDiedThisTurnComponent>().shouldBeNull()

        driver.passPriorityUntil(Step.END)

        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe lifeBefore + 1
    }

    test("per-turn creature died tracker is reset at cleanup so a subsequent turn re-evaluates to false") {
        val driver = buildDriver()
        val you = driver.player1

        // Set the tracker as if a creature died during player 1's turn
        driver.replaceState(
            driver.state.updateEntity(you) { container ->
                container.with(CreaturesDiedThisTurnComponent(count = 1))
            }
        )
        driver.state.getEntity(you)?.get<CreaturesDiedThisTurnComponent>()?.count shouldBe 1

        // Advance through end-of-turn cleanup into player 2's turn
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN) // crosses CLEANUP → player 2's main

        // CleanupPhaseManager must have stripped the component from player 1's entity
        driver.state.getEntity(you)?.get<CreaturesDiedThisTurnComponent>().shouldBeNull()
    }
})
