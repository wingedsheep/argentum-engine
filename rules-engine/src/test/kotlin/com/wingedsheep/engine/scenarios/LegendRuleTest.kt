package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for the legend rule (704.5j).
 *
 * When a player controls two or more legendary permanents with the same name,
 * that player chooses one to keep and the rest are put into the graveyard.
 */
class LegendRuleTest : FunSpec({

    test("legend rule asks player to choose which legendary to keep") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Test Hasty Prospector" to 4)
        )

        val p1 = driver.player1

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put first Ragavan directly on the battlefield
        val firstRagavan = driver.putCreatureOnBattlefield(p1, "Test Hasty Prospector")

        // Cast second Ragavan through the normal spell flow (SBAs checked after resolution)
        driver.giveMana(p1, Color.RED, 1)
        val ragavanInHand = driver.putCardInHand(p1, "Test Hasty Prospector")
        driver.castSpell(p1, ragavanInHand)

        // Resolve the spell - SBAs are checked after resolution, triggering legend rule
        val resolveResult = driver.bothPass()

        // Get the second Ragavan's entity ID (should be on battlefield now)
        val ragavansOnField = driver.getPermanents(p1).filter {
            driver.getCardName(it) == "Test Hasty Prospector"
        }
        ragavansOnField.size shouldBe 2
        val secondRagavan = ragavansOnField.first { it != firstRagavan }

        // There should be a pending SelectCardsDecision for the legend rule
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.prompt.contains("legend rule") shouldBe true
        decision.options.size shouldBe 2
        decision.options.toSet() shouldBe setOf(firstRagavan, secondRagavan)
        decision.minSelections shouldBe 1
        decision.maxSelections shouldBe 1
        decision.useTargetingUI shouldBe true
        decision.context.phase shouldBe DecisionPhase.STATE_BASED

        // Choose to keep the second Ragavan
        driver.submitCardSelection(p1, listOf(secondRagavan))

        // First Ragavan should be in the graveyard
        driver.getGraveyard(p1).any { it == firstRagavan } shouldBe true

        // Second Ragavan should still be on the battlefield
        val remaining = driver.getPermanents(p1)
        remaining.any { it == secondRagavan } shouldBe true
        remaining.none { it == firstRagavan } shouldBe true
    }

    test("legend rule lets player keep the older legendary") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Test Hasty Prospector" to 4)
        )

        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put first Ragavan directly on the battlefield
        val firstRagavan = driver.putCreatureOnBattlefield(p1, "Test Hasty Prospector")

        // Cast second Ragavan through normal spell flow
        driver.giveMana(p1, Color.RED, 1)
        val ragavanInHand = driver.putCardInHand(p1, "Test Hasty Prospector")
        driver.castSpell(p1, ragavanInHand)
        driver.bothPass()

        // Choose to keep the first (older) Ragavan
        driver.submitCardSelection(p1, listOf(firstRagavan))

        // Second Ragavan should be in graveyard
        val graveyardNames = driver.getGraveyardCardNames(p1)
        graveyardNames.count { it == "Test Hasty Prospector" } shouldBe 1

        // Only one Ragavan on battlefield
        val remaining = driver.getPermanents(p1).filter {
            driver.getCardName(it) == "Test Hasty Prospector"
        }
        remaining.size shouldBe 1
        remaining.first() shouldBe firstRagavan
    }

    test("legend rule does not trigger for different legendary names") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 10, "Forest" to 10)
        )

        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two different legendary creatures on the battlefield
        driver.putCreatureOnBattlefield(p1, "Test Hasty Prospector")
        driver.putCreatureOnBattlefield(p1, "Ghalta, Primal Hunger")

        // Both pass - should NOT trigger legend rule (different names)
        driver.bothPass()

        // No pending decision
        driver.pendingDecision shouldBe null
    }

    test("legend rule only affects the same player") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Test Hasty Prospector" to 4)
        )

        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Ragavan on each player's battlefield
        driver.putCreatureOnBattlefield(p1, "Test Hasty Prospector")
        driver.putCreatureOnBattlefield(p2, "Test Hasty Prospector")

        // Both pass - should NOT trigger legend rule (different controllers)
        driver.bothPass()

        // No pending decision
        driver.pendingDecision shouldBe null
    }
})
