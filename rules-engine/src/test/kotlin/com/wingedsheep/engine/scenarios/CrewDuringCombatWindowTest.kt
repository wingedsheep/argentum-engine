package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Investigation: can the DEFENDING player crew a Vehicle at instant speed during the
 * declare-attackers priority window (after the active player declares attackers, before
 * the declare-blockers step)? Crew is an activated ability usable any time you have priority.
 */
class CrewDuringCombatWindowTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    test("defending player can crew during the declare-attackers priority window") {
        val driver = createDriver()
        val attacker = driver.player1   // active player
        val defender = driver.player2   // the user, defending

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Attacker has a creature to attack with.
        val beast = driver.putCreatureOnBattlefield(attacker, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(beast)

        // Defender controls Mobile Homestead (Vehicle, Crew 2) and a power-2 crewer.
        val homestead = driver.putPermanentOnBattlefield(defender, "Mobile Homestead")
        val crewer = driver.putCreatureOnBattlefield(defender, "Grizzly Bears") // 2/2

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(beast), defender)

        // Active player holds priority first in declare-attackers; pass it.
        driver.state.priorityPlayerId shouldBe attacker
        driver.submit(PassPriority(attacker))

        // Now the DEFENDING player should hold priority, still in declare-attackers.
        driver.currentStep shouldBe Step.DECLARE_ATTACKERS
        driver.state.priorityPlayerId shouldBe defender
        driver.state.pendingDecision shouldBe null

        // Reproduce the server's legal-action view for the defender.
        val services = EngineServices(driver.cardRegistry)
        val enumerator = LegalActionEnumerator(
            driver.cardRegistry, services.manaSolver, services.costCalculator,
            services.predicateEvaluator, services.conditionEvaluator, services.turnManager
        )
        val enricher = LegalActionEnricher(services.manaSolver, driver.cardRegistry)
        val legalActions = enricher.enrich(enumerator.enumerate(driver.state, defender), driver.state, defender)

        val crewActions = legalActions.filter { it.actionType == "CrewVehicle" }
        crewActions.shouldNotBeEmpty()
        crewActions.first().isAffordable shouldBe true

        // And actually crewing it should succeed.
        driver.submitSuccess(CrewVehicle(defender, homestead, listOf(crewer)))
    }
})
