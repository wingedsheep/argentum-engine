package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.answerForcedPayments
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scg.cards.DragonTyrant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Dragon Tyrant's upkeep trigger:
 * "At the beginning of your upkeep, sacrifice Dragon Tyrant unless you pay {R}{R}{R}{R}."
 */
class DragonTyrantTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DragonTyrant))
        return driver
    }

    fun advanceToPlayerUpkeep(driver: GameTestDriver, targetPlayer: EntityId) {
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        if (driver.activePlayer == targetPlayer) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        }
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe targetPlayer
    }

    test("pay mana to keep Dragon Tyrant") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Dragon Tyrant on the battlefield
        val dragon = driver.putCreatureOnBattlefield(activePlayer, "Dragon Tyrant")

        // Put 4 untapped Mountains for paying the upkeep cost
        repeat(4) { driver.putLandOnBattlefield(activePlayer, "Mountain") }

        // Advance to active player's upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger goes on the stack
        driver.stackSize shouldBe 1

        // Resolve the trigger
        driver.bothPass()

        // Should get a yes/no decision to pay {R}{R}{R}{R}
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        // Choose to pay
        driver.submitYesNo(activePlayer, true)

        // Dragon Tyrant should still be on the battlefield
        driver.findPermanent(activePlayer, "Dragon Tyrant") shouldNotBe null
    }

    test("sacrifice Dragon Tyrant when declining to pay") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Dragon Tyrant on the battlefield
        val dragon = driver.putCreatureOnBattlefield(activePlayer, "Dragon Tyrant")

        // Put 4 untapped Mountains
        repeat(4) { driver.putLandOnBattlefield(activePlayer, "Mountain") }

        // Advance to upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        // Decline to pay
        driver.submitYesNo(activePlayer, false)

        // Dragon Tyrant should be sacrificed
        driver.findPermanent(activePlayer, "Dragon Tyrant") shouldBe null
    }

    test("auto-sacrifice when unable to pay mana cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Dragon Tyrant on the battlefield with no lands to pay
        driver.putCreatureOnBattlefield(activePlayer, "Dragon Tyrant")

        // Advance to upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger goes on the stack
        driver.stackSize shouldBe 1

        // Resolve the trigger — refusing is the only answer, so it's sacrificed
        driver.bothPass()
        driver.answerForcedPayments()

        // Dragon Tyrant should be gone
        driver.findPermanent(activePlayer, "Dragon Tyrant") shouldBe null
    }
})
