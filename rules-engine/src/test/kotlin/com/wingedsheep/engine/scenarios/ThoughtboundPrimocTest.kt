package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.APlayerControlsMostOfSubtype
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GainControlByMostOfSubtypeEffect
import com.wingedsheep.sdk.scripting.triggers.OnUpkeep
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Thoughtbound Primoc's control-changing triggered ability.
 *
 * Thoughtbound Primoc: {2}{R}
 * Creature — Bird Beast
 * 2/3
 * Flying
 * At the beginning of your upkeep, if a player controls more Wizards
 * than each other player, that player gains control of Thoughtbound Primoc.
 *
 * Key rules tested:
 * - "your upkeep" refers to the CONTROLLER's upkeep, not the owner's.
 * - After control changes via a floating effect, the projected controller
 *   should be used for determining valid attackers/blockers.
 */
class ThoughtboundPrimocTest : FunSpec({

    // Simple Wizard creature for testing
    val TestWizard = CardDefinition.creature(
        name = "Test Wizard",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype("Human"), Subtype("Wizard")),
        power = 1,
        toughness = 1
    )

    // Recreate Thoughtbound Primoc for rules-engine tests (no mtg-sets dependency)
    val ThoughtboundPrimoc = CardDefinition.creature(
        name = "Thoughtbound Primoc",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype("Bird"), Subtype("Beast")),
        keywords = setOf(Keyword.FLYING),
        power = 2,
        toughness = 3,
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnUpkeep(controllerOnly = true),
                effect = ConditionalEffect(
                    condition = APlayerControlsMostOfSubtype(Subtype("Wizard")),
                    effect = GainControlByMostOfSubtypeEffect(Subtype("Wizard"))
                )
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThoughtboundPrimoc, TestWizard))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )
        return driver
    }

    /**
     * Advance to targetPlayer's next upkeep from the current position
     * (must be at or past PRECOMBAT_MAIN of some player's turn).
     *
     * Strategy: use DRAW as an intermediate waypoint. DRAW comes after UPKEEP
     * in the turn, so reaching a DRAW step means we've passed that player's UPKEEP.
     * We ensure we're at the non-target player's DRAW, then advance to the next
     * UPKEEP which will be the target player's.
     */
    fun advanceToPlayerUpkeep(driver: GameTestDriver, targetPlayer: EntityId) {
        // Step 1: advance to the next DRAW step
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)

        // Step 2: if we landed at the target player's DRAW, their UPKEEP already passed.
        // We need to go through the rest of their turn + opponent's turn to reach
        // the target player's UPKEEP again. Go to the next DRAW (opponent's).
        if (driver.activePlayer == targetPlayer) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        }

        // Now we're at the opponent's DRAW step. The next UPKEEP is the target player's.
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe targetPlayer
    }

    test("control changes to player with more Wizards on upkeep") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val primocOwner = driver.activePlayer!!
        val opponent = driver.getOpponent(primocOwner)

        val primoc = driver.putCreatureOnBattlefield(primocOwner, "Thoughtbound Primoc")
        driver.removeSummoningSickness(primoc)

        // Opponent has more Wizards (2 vs 0)
        driver.putCreatureOnBattlefield(opponent, "Test Wizard")
        driver.putCreatureOnBattlefield(opponent, "Test Wizard")

        // Advance to primocOwner's next upkeep
        advanceToPlayerUpkeep(driver, primocOwner)

        // The trigger should be on the stack
        driver.stackSize shouldBe 1

        // Resolve the trigger
        driver.bothPass()

        // Control should have changed to opponent
        val projected = projector.project(driver.state)
        projected.getController(primoc) shouldBe opponent
    }

    test("new controller can attack with stolen Thoughtbound Primoc") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val primocOwner = driver.activePlayer!!
        val opponent = driver.getOpponent(primocOwner)

        val primoc = driver.putCreatureOnBattlefield(primocOwner, "Thoughtbound Primoc")
        driver.removeSummoningSickness(primoc)

        // Opponent has more Wizards
        val wizard = driver.putCreatureOnBattlefield(opponent, "Test Wizard")
        driver.removeSummoningSickness(wizard)

        // Advance to primocOwner's upkeep - trigger fires
        advanceToPlayerUpkeep(driver, primocOwner)
        driver.stackSize shouldBe 1
        driver.bothPass() // resolve the trigger

        // Verify control changed
        val projected = projector.project(driver.state)
        projected.getController(primoc) shouldBe opponent

        // Advance to opponent's DECLARE_ATTACKERS
        // From primocOwner's UPKEEP, go to opponent's DRAW (past opponent's upkeep),
        // then to DECLARE_ATTACKERS
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS, maxPasses = 200)
        driver.activePlayer shouldBe opponent

        // Opponent should be able to declare Primoc as an attacker
        val attackResult = driver.declareAttackers(
            opponent,
            listOf(primoc),
            primocOwner
        )
        attackResult.isSuccess shouldBe true
    }

    test("new controller can block with stolen Thoughtbound Primoc") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val primocOwner = driver.activePlayer!!
        val opponent = driver.getOpponent(primocOwner)

        val primoc = driver.putCreatureOnBattlefield(primocOwner, "Thoughtbound Primoc")
        driver.removeSummoningSickness(primoc)

        // Opponent has more Wizards
        driver.putCreatureOnBattlefield(opponent, "Test Wizard")

        // PrimocOwner has an attacker
        val attacker = driver.putCreatureOnBattlefield(primocOwner, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        // Advance to primocOwner's upkeep - trigger fires
        advanceToPlayerUpkeep(driver, primocOwner)
        driver.bothPass() // resolve the trigger

        // Verify control changed
        val projected = projector.project(driver.state)
        projected.getController(primoc) shouldBe opponent

        // Continue primocOwner's turn to combat - primocOwner attacks with Grizzly Bears
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(primocOwner, listOf(attacker), opponent)
        driver.bothPass()

        // Opponent should be able to block with the stolen Primoc
        val blockResult = driver.declareBlockers(
            opponent,
            mapOf(primoc to listOf(attacker))
        )
        blockResult.isSuccess shouldBe true
    }

    test("no control change when Wizard counts are tied") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val primocOwner = driver.activePlayer!!
        val opponent = driver.getOpponent(primocOwner)

        val primoc = driver.putCreatureOnBattlefield(primocOwner, "Thoughtbound Primoc")
        driver.removeSummoningSickness(primoc)

        // Both players have 1 Wizard (tied)
        driver.putCreatureOnBattlefield(primocOwner, "Test Wizard")
        driver.putCreatureOnBattlefield(opponent, "Test Wizard")

        // Advance to primocOwner's upkeep
        advanceToPlayerUpkeep(driver, primocOwner)

        // Trigger fires but condition evaluates to false (tied), so stack resolves with no effect
        driver.bothPass()

        // Control should NOT have changed (tie)
        val projected = projector.project(driver.state)
        projected.getController(primoc) shouldBe primocOwner
    }

    test("no control change when no players have Wizards") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val primocOwner = driver.activePlayer!!

        val primoc = driver.putCreatureOnBattlefield(primocOwner, "Thoughtbound Primoc")
        driver.removeSummoningSickness(primoc)

        // No Wizards on the battlefield

        // Advance to primocOwner's upkeep
        advanceToPlayerUpkeep(driver, primocOwner)

        // Trigger fires but condition evaluates to false (no wizards), so stack resolves with no effect
        driver.bothPass()

        // Control should NOT have changed
        val projected = projector.project(driver.state)
        projected.getController(primoc) shouldBe primocOwner
    }
})
