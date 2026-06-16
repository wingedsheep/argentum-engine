package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.mtg.sets.definitions.otj.cards.LuxuriousLocomotive
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Luxurious Locomotive — {5} Artifact — Vehicle, 6/5.
 *
 * - Whenever this Vehicle attacks, create a Treasure token for each creature that crewed it
 *   this turn.
 * - Crew 1. Activate only once each turn.
 *
 * Exercises the once-each-turn crew cap (new `crew(1, onceEachTurn = true)`) and the
 * Treasure-per-crewer attack payoff.
 */
class OtjLuxuriousLocomotiveScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LuxuriousLocomotive)
        driver.registerCard(PredefinedTokens.Treasure)
        driver.initMirrorMatch(Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.treasures(playerId: EntityId): Int =
        getPermanents(playerId).count {
            state.getEntity(it)?.get<CardComponent>()?.name == "Treasure"
        }

    test("attacking creates a Treasure for each creature that crewed it this turn") {
        val driver = newDriver()
        val loco = driver.putPermanentOnBattlefield(driver.player1, "Luxurious Locomotive")
        val bear1 = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val bear2 = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(loco)

        driver.treasures(driver.player1) shouldBe 0

        driver.submitSuccess(CrewVehicle(driver.player1, loco, listOf(bear1, bear2)))
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(loco), driver.player2)
        driver.bothPass()

        // Two creatures crewed it → two Treasure tokens.
        driver.treasures(driver.player1) shouldBe 2
    }

    test("Crew can only be activated once each turn") {
        val driver = newDriver()
        val loco = driver.putPermanentOnBattlefield(driver.player1, "Luxurious Locomotive")
        val bear1 = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val bear2 = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        // First crew succeeds.
        driver.submitSuccess(CrewVehicle(driver.player1, loco, listOf(bear1)))
        driver.bothPass()

        // Second crew this turn is rejected by the once-each-turn cap, even with an untapped crewer.
        driver.submitExpectFailure(CrewVehicle(driver.player1, loco, listOf(bear2)))
    }
})
