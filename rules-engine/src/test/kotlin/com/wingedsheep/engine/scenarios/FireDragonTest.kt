package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
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
 * Tests for Fire Dragon's ETB triggered ability.
 *
 * Fire Dragon:
 * "When Fire Dragon enters the battlefield, it deals damage to target creature
 * equal to the number of Mountains you control."
 */
class FireDragonTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Fire Dragon ETB deals damage equal to number of Mountains controlled") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 3 Mountains on active player's battlefield
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")

        // Put a 5/5 creature on opponent's battlefield (won't die from 3 damage)
        val targetCreature = driver.putCreatureOnBattlefield(opponent, "Force of Nature")

        // Give active player Fire Dragon and mana to cast it
        val fireDragon = driver.putCardInHand(activePlayer, "Fire Dragon")
        driver.giveMana(activePlayer, Color.RED, 9)

        // Cast Fire Dragon (no targets needed for the spell itself)
        val castResult = driver.castSpell(activePlayer, fireDragon)
        castResult.isSuccess shouldBe true

        // Let the spell resolve (both players pass priority)
        driver.bothPass()

        // Fire Dragon should be on the battlefield
        driver.findPermanent(activePlayer, "Fire Dragon") shouldNotBe null

        // The ETB trigger should have fired and we should have a target selection decision
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldNotBeNull()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        targetDecision.playerId shouldBe activePlayer

        // Legal targets should include Force of Nature
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()
        legalTargets shouldContain targetCreature

        // Submit the target selection (choose Force of Nature)
        val targetResult = driver.submitTargetSelection(activePlayer, listOf(targetCreature))
        targetResult.isSuccess shouldBe true

        // The ability should now be on the stack - resolve it
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Target creature should have taken 3 damage (equal to number of Mountains)
        // Force of Nature is 5/5, so it survives and we can check the damage
        val damage = driver.state.getEntity(targetCreature)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 3
    }

    test("Fire Dragon ETB deals 0 damage with no Mountains") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put NO mountains - only forests
        driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(activePlayer, "Forest")

        // Put a creature on opponent's battlefield to target
        val targetCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Give active player Fire Dragon and mana to cast it
        val fireDragon = driver.putCardInHand(activePlayer, "Fire Dragon")
        driver.giveMana(activePlayer, Color.RED, 9)

        // Cast Fire Dragon
        driver.castSpell(activePlayer, fireDragon)

        // Let the spell resolve
        driver.bothPass()

        // Handle target selection for ETB trigger
        if (driver.isPaused && driver.pendingDecision is ChooseTargetsDecision) {
            driver.submitTargetSelection(activePlayer, listOf(targetCreature))
        }

        // Resolve the ETB trigger
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Target creature should have taken 0 damage (no Mountains)
        val damage = driver.state.getEntity(targetCreature)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 0
    }

    test("Fire Dragon ETB kills creature with lethal damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 5 Mountains on active player's battlefield (more than Grizzly Bears' 2 toughness)
        repeat(5) {
            driver.putLandOnBattlefield(activePlayer, "Mountain")
        }

        // Put Grizzly Bears (2/2) on opponent's battlefield
        val targetCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Give active player Fire Dragon and mana to cast it
        val fireDragon = driver.putCardInHand(activePlayer, "Fire Dragon")
        driver.giveMana(activePlayer, Color.RED, 9)

        // Cast Fire Dragon
        driver.castSpell(activePlayer, fireDragon)

        // Let the spell resolve
        driver.bothPass()

        // Handle target selection for ETB trigger
        if (driver.isPaused && driver.pendingDecision is ChooseTargetsDecision) {
            driver.submitTargetSelection(activePlayer, listOf(targetCreature))
        }

        // Resolve the ETB trigger
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // State-based actions should have destroyed the creature (5 damage to 2 toughness)
        // The creature should be in the graveyard
        driver.assertInGraveyard(opponent, "Grizzly Bears")
    }
})
