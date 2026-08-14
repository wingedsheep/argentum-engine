package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.CheeringCrowd
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cheering Crowd (SPM #126)
 * {1}{R/G} Creature — Human Citizen 2/2
 * At the beginning of each player's first main phase, that player may put a +1/+1 counter on
 * this creature. If they do, they add {C} for each counter on it.
 *
 * The load-bearing property is that "that player" is the active player whose first (precombat)
 * main phase it is — even an opponent — and *that* player both makes the "may" choice and
 * receives the {C}. The mana lands in the triggering player's pool, never the controller's.
 */
class CheeringCrowdScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CheeringCrowd)
        return driver
    }

    fun counters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun colorless(driver: GameTestDriver, playerId: EntityId): Int =
        driver.state.getEntity(playerId)?.get<ManaPoolComponent>()?.colorless ?: 0

    // Resolve the beginning-of-main-phase trigger up to its yes/no prompt without letting
    // passPriorityUntil auto-decline it.
    fun advanceToTriggerPrompt(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.pendingDecision !is YesNoDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
    }

    test("controller's first main: that player may add a counter and adds {C} for each counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, startingPlayer = 0)

        val me = driver.player1
        val opponent = driver.getOpponent(me)

        val crowd = driver.putCreatureOnBattlefield(me, "Cheering Crowd")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        advanceToTriggerPrompt(driver)
        (driver.state.pendingDecision is YesNoDecision) shouldBe true
        driver.state.pendingDecision!!.playerId shouldBe me
        driver.submitYesNo(me, true)

        // One +1/+1 counter placed, and one {C} in the active player's pool (one counter on it).
        counters(driver, crowd) shouldBe 1
        colorless(driver, me) shouldBe 1
        colorless(driver, opponent) shouldBe 0
    }

    test("controller declines: no counter placed and no mana added") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, startingPlayer = 0)

        val me = driver.player1
        val crowd = driver.putCreatureOnBattlefield(me, "Cheering Crowd")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        advanceToTriggerPrompt(driver)
        (driver.state.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(me, false)

        counters(driver, crowd) shouldBe 0
        colorless(driver, me) shouldBe 0
    }

    test("opponent's first main: the opponent decides and the {C} goes to the opponent's pool") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, startingPlayer = 0)

        val controller = driver.player1
        val opponent = driver.player2

        // Cheering Crowd is controlled by player1; advance into player2's turn (its beginning-of-
        // first-main trigger will fire for player2 as the active/triggering player).
        while (driver.activePlayer != opponent) {
            driver.passPriorityUntil(Step.END)
            driver.bothPass()
        }

        val crowd = driver.putCreatureOnBattlefield(controller, "Cheering Crowd")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        advanceToTriggerPrompt(driver)
        (driver.state.pendingDecision is YesNoDecision) shouldBe true
        // The prompt is directed at the opponent (the triggering player), not the controller.
        driver.state.pendingDecision!!.playerId shouldBe opponent
        driver.submitYesNo(opponent, true)

        counters(driver, crowd) shouldBe 1
        // The mana lands in the triggering player's pool — never the controller's.
        colorless(driver, opponent) shouldBe 1
        colorless(driver, controller) shouldBe 0
    }

    test("adds {C} for EACH counter: a second activation scales with accumulated counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, startingPlayer = 0)

        val me = driver.player1
        val crowd = driver.putCreatureOnBattlefield(me, "Cheering Crowd")

        // First of my main phases: place the first counter → 1 counter, 1 {C}.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        advanceToTriggerPrompt(driver)
        driver.submitYesNo(me, true)
        counters(driver, crowd) shouldBe 1
        colorless(driver, me) shouldBe 1

        // Advance to my next turn's first main phase. passPriorityUntil auto-resolves the
        // intervening cleanup discards and auto-declines the opponent's own Cheering Crowd
        // trigger (a "may" it says no to) while walking through the opponent's turn.
        driver.passPriorityUntil(Step.END)             // leave my current turn's main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)  // opponent's first main (their trigger…)
        driver.passPriorityUntil(Step.END)             // …auto-declined as we cross their turn
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)  // my next first main — my trigger, unresolved

        advanceToTriggerPrompt(driver)
        (driver.state.pendingDecision is YesNoDecision) shouldBe true
        driver.state.pendingDecision!!.playerId shouldBe me
        driver.submitYesNo(me, true)

        // Now two counters on it, so "add {C} for each counter" adds 2 {C}.
        counters(driver, crowd) shouldBe 2
        colorless(driver, me) shouldBe 2
    }
})
