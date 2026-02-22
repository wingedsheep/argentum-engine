package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.GangrenousGoliath
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Gangrenous Goliath:
 * {3}{B}{B}
 * Creature — Zombie Giant
 * 4/4
 * Tap three untapped Clerics you control: Return Gangrenous Goliath from your graveyard to your hand.
 */
class GangrenousGoliathTest : FunSpec({

    val abilityId = GangrenousGoliath.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("return Gangrenous Goliath from graveyard by tapping three Clerics") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Gangrenous Goliath in graveyard
        val goliath = driver.putCardInGraveyard(activePlayer, "Gangrenous Goliath")

        // Put three Clerics on the battlefield
        val cleric1 = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")
        val cleric2 = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")
        val cleric3 = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")

        // Activate the graveyard ability by tapping three Clerics
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = goliath,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(cleric1, cleric2, cleric3)
                )
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Gangrenous Goliath should now be in hand
        driver.getHand(activePlayer).contains(goliath) shouldBe true

        // Goliath should no longer be in graveyard
        driver.state.getGraveyard(activePlayer).contains(goliath) shouldBe false

        // Clerics should be tapped
        driver.isTapped(cleric1) shouldBe true
        driver.isTapped(cleric2) shouldBe true
        driver.isTapped(cleric3) shouldBe true
    }

    test("cannot activate with fewer than three Clerics") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goliath = driver.putCardInGraveyard(activePlayer, "Gangrenous Goliath")

        // Only two Clerics
        val cleric1 = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")
        val cleric2 = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = goliath,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(cleric1, cleric2)
                )
            )
        )
        result.isSuccess shouldBe false
    }

    test("cannot activate from battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Goliath on battlefield (not graveyard)
        val goliath = driver.putCreatureOnBattlefield(activePlayer, "Gangrenous Goliath")

        val cleric1 = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")
        val cleric2 = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")
        val cleric3 = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = goliath,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(cleric1, cleric2, cleric3)
                )
            )
        )
        result.isSuccess shouldBe false
    }

    test("cannot tap non-Cleric creatures to pay cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goliath = driver.putCardInGraveyard(activePlayer, "Gangrenous Goliath")

        // Use non-Cleric creatures (Bears are 2/2 vanilla creatures)
        val bear1 = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val bear2 = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val bear3 = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = goliath,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(bear1, bear2, bear3)
                )
            )
        )
        result.isSuccess shouldBe false
    }
})
