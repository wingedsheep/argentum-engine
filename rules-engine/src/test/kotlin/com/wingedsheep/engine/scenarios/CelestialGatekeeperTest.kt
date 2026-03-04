package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.legions.cards.AvenEnvoy
import com.wingedsheep.mtg.sets.definitions.legions.cards.CelestialGatekeeper
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.BattlefieldMedic
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CelestialGatekeeperTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CelestialGatekeeper, AvenEnvoy))
        return driver
    }

    test("Celestial Gatekeeper dies trigger exiles itself and returns Bird/Cleric cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Celestial Gatekeeper on the battlefield
        val gatekeeper = driver.putCreatureOnBattlefield(activePlayer, "Celestial Gatekeeper")

        // Put a Bird creature in the graveyard
        val bird = driver.putCardInGraveyard(activePlayer, "Aven Envoy")

        // Kill the Gatekeeper with Lightning Bolt (2/2 dies to 3 damage)
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val castResult = driver.castSpell(activePlayer, bolt, listOf(gatekeeper))
        castResult.isSuccess shouldBe true

        // Resolve the bolt
        driver.bothPass()

        // Gatekeeper should have died, trigger should be on stack
        // The trigger needs targets — should pause for target selection
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()

        // Bird should be a legal target
        legalTargets shouldContain bird

        // Select the bird as target
        driver.submitTargetSelection(activePlayer, listOf(bird))

        // Resolve the triggered ability
        driver.bothPass()

        // Celestial Gatekeeper should be in exile, not graveyard
        val exileCards = driver.state.getExile(activePlayer)
        val graveyardCards = driver.getGraveyard(activePlayer)

        exileCards shouldContain gatekeeper
        graveyardCards shouldNotContain gatekeeper

        // The bird should be on the battlefield now
        driver.findPermanent(activePlayer, "Aven Envoy") shouldNotBe null
    }

    test("Celestial Gatekeeper trigger only allows Bird and Cleric targets, not other creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Celestial Gatekeeper on the battlefield
        val gatekeeper = driver.putCreatureOnBattlefield(activePlayer, "Celestial Gatekeeper")

        // Put a non-Bird non-Cleric creature in the graveyard
        val bear = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        // Also put a Cleric in the graveyard
        val cleric = driver.putCardInGraveyard(activePlayer, "Battlefield Medic")

        // Kill the Gatekeeper with Lightning Bolt
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val castResult = driver.castSpell(activePlayer, bolt, listOf(gatekeeper))
        castResult.isSuccess shouldBe true

        // Resolve the bolt
        driver.bothPass()

        // Trigger should be on stack, paused for target selection
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()

        // Cleric should be a legal target
        legalTargets shouldContain cleric

        // Grizzly Bears (a Bear, not Bird/Cleric) should NOT be a legal target
        legalTargets shouldNotContain bear
    }
})
