package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Razorkin Hordecaller (DSK #152) — {4}{R} Creature — Human Clown Berserker 4/4.
 *
 * "Haste. Whenever you attack, create a 1/1 red Gremlin creature token."
 *
 * The "whenever you attack" trigger fires once per combat (when attackers are declared), not once
 * per attacker, producing a single 1/1 Gremlin.
 */
class RazorkinHordecallerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("attacking creates a 1/1 red Gremlin token") {
        val driver = newDriver()
        val me = driver.player1

        val hordecaller = driver.putCreatureOnBattlefield(me, "Razorkin Hordecaller")
        // Haste lets it attack the turn it enters, but clear sickness explicitly for robustness.
        driver.removeSummoningSickness(hordecaller)

        val gremlinsBefore = driver.getCreatures(me).count { id ->
            driver.state.getEntity(id)?.let { e ->
                val card = e.get<CardComponent>()
                card?.typeLine?.hasSubtype(Subtype.GREMLIN) == true
            } == true
        }
        gremlinsBefore shouldBe 0

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(hordecaller), driver.player2)

        // Resolve the "whenever you attack" trigger.
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        val gremlins = driver.getCreatures(me).filter { id ->
            val card = driver.state.getEntity(id)
                ?.get<CardComponent>()
            card?.typeLine?.hasSubtype(Subtype.GREMLIN) == true
        }
        gremlins.size shouldBe 1
    }
})
