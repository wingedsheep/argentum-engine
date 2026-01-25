package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Serpent Assassin's ETB triggered ability.
 *
 * Serpent Assassin: {3}{B}{B}
 * Creature — Snake Assassin
 * 2/2
 * When Serpent Assassin enters the battlefield, you may destroy target nonblack creature.
 */
class SerpentAssassinTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Serpent Assassin ETB trigger can destroy a nonblack creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a nonblack creature (green) on opponent's battlefield
        val grizzlyBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null

        // Give active player Serpent Assassin and mana to cast it
        val serpentAssassin = driver.putCardInHand(activePlayer, "Serpent Assassin")
        driver.giveMana(activePlayer, Color.BLACK, 5)

        // Cast Serpent Assassin
        val castResult = driver.castSpell(activePlayer, serpentAssassin)
        castResult.isSuccess shouldBe true

        // Let the creature spell resolve (both players pass priority)
        driver.bothPass()

        // Serpent Assassin should be on the battlefield
        driver.findPermanent(activePlayer, "Serpent Assassin") shouldNotBe null

        // The ETB trigger should have fired and we should have a target selection decision
        // Since the ability has a targetRequirement, the engine should pause for target selection
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldNotBeNull()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        targetDecision.playerId shouldBe activePlayer

        // Legal targets should include Grizzly Bears (nonblack creature)
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()
        legalTargets shouldContain grizzlyBears

        // Submit the target selection (choose Grizzly Bears)
        val targetResult = driver.submitTargetSelection(activePlayer, listOf(grizzlyBears))
        targetResult.isSuccess shouldBe true

        // The ability should now be on the stack - resolve it
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Grizzly Bears should now be destroyed (in graveyard)
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
    }

    test("Serpent Assassin ETB trigger cannot target black creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put another Serpent Assassin (black creature) on opponent's battlefield
        val opponentSerpent = driver.putCreatureOnBattlefield(opponent, "Serpent Assassin")

        // Give active player Serpent Assassin and mana to cast it
        val serpentAssassin = driver.putCardInHand(activePlayer, "Serpent Assassin")
        driver.giveMana(activePlayer, Color.BLACK, 5)

        // Cast Serpent Assassin
        driver.castSpell(activePlayer, serpentAssassin)

        // Let the creature spell resolve
        driver.bothPass()

        // Serpent Assassin should be on the battlefield
        driver.findPermanent(activePlayer, "Serpent Assassin") shouldNotBe null

        // The ETB trigger fires, but with no valid targets (only black creature on battlefield)
        // For "may" abilities with no legal targets, the ability should fizzle without pausing
        // Actually, the ability should not go on the stack at all if there are no legal targets

        // Opponent's Serpent Assassin (black) should NOT be a legal target
        // The ability should have fizzled or not triggered due to no legal targets
        if (driver.isPaused) {
            val targetDecision = driver.pendingDecision as? ChooseTargetsDecision
            if (targetDecision != null) {
                val legalTargets = targetDecision.legalTargets[0] ?: emptyList()
                // The opponent's black Serpent Assassin should NOT be in legal targets
                legalTargets.contains(opponentSerpent) shouldBe false
            }
        }

        // The opponent's Serpent Assassin should still be on the battlefield
        driver.findPermanent(opponent, "Serpent Assassin") shouldNotBe null
    }

    test("Serpent Assassin ETB trigger requires target selection when used") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a nonblack creature on opponent's battlefield
        val grizzlyBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Give active player Serpent Assassin and mana
        val serpentAssassin = driver.putCardInHand(activePlayer, "Serpent Assassin")
        driver.giveMana(activePlayer, Color.BLACK, 5)

        // Cast Serpent Assassin
        driver.castSpell(activePlayer, serpentAssassin)
        driver.bothPass()

        // Serpent Assassin should be on the battlefield
        driver.findPermanent(activePlayer, "Serpent Assassin") shouldNotBe null

        // The ability fires and we must select a target
        // "you may destroy" means the destruction is optional at resolution,
        // but target selection is still required when the ability goes on the stack
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val minTargets = targetDecision.targetRequirements.firstOrNull()?.minTargets ?: 0

        // Target selection is mandatory (min 1 target)
        minTargets shouldBe 1

        // Submit the target selection
        driver.submitTargetSelection(activePlayer, listOf(grizzlyBears))

        // Resolve the ability
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Grizzly Bears should be destroyed
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
    }
})
