package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rtr.cards.DesecrationDemon
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Desecration Demon (RTR) — {2}{B}{B} Creature — Demon, 6/6, Flying.
 *
 * "At the beginning of each combat, any opponent may sacrifice a creature of their choice.
 * If a player does, tap this creature and put a +1/+1 counter on it."
 *
 * Exercises the `eligiblePlayers = Player.EachOpponent` scoping on `AnyPlayerMayPayEffect`:
 * only opponents are offered the sacrifice, and the reflexive tap + single +1/+1 counter fires
 * once when an opponent sacrifices.
 */
class DesecrationDemonScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DesecrationDemon)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("an opponent sacrifices a creature: the Demon is tapped and gets one +1/+1 counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30))
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val demon = driver.putCreatureOnBattlefield(controller, "Desecration Demon")
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Advance to the beginning of combat — the "each combat" trigger fires.
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        if (driver.stackSize > 0) driver.bothPass()

        // Only the opponent is offered the sacrifice.
        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe opponent
        decision.prompt shouldBe "You may sacrifice a creature. If you do, tap Desecration Demon. " +
            "Put a +1/+1 counter on Desecration Demon."

        driver.submitCardSelection(opponent, listOf(opponentCreature))

        // The opponent's creature is sacrificed; the Demon is tapped with one +1/+1 counter.
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.isTapped(demon) shouldBe true
        plusOneCounters(driver, demon) shouldBe 1
    }

    test("an opponent declines: the Demon is untapped with no counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30))
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val demon = driver.putCreatureOnBattlefield(controller, "Desecration Demon")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        if (driver.stackSize > 0) driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe opponent

        // Decline.
        driver.submitCardSelection(opponent, emptyList())

        driver.isTapped(demon) shouldBe false
        plusOneCounters(driver, demon) shouldBe 0
    }

    test("the controller is never offered the choice — only opponents may sacrifice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30))
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only the controller has a creature to sacrifice; the opponent has none.
        val demon = driver.putCreatureOnBattlefield(controller, "Desecration Demon")
        driver.putCreatureOnBattlefield(controller, "Centaur Courser")

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        if (driver.stackSize > 0) driver.bothPass()

        // No opponent can pay, so no decision is raised (the controller's creature is off-limits),
        // and the Demon stays untapped with no counter.
        driver.pendingDecision shouldBe null
        driver.isTapped(demon) shouldBe false
        plusOneCounters(driver, demon) shouldBe 0
        driver.findPermanent(controller, "Centaur Courser").shouldNotBeNull()
    }
})
