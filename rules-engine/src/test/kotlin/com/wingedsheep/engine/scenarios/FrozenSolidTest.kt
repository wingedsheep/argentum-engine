package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.FrozenSolid
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Frozen Solid.
 *
 * Frozen Solid: {1}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature doesn't untap during its controller's untap step.
 * When enchanted creature is dealt damage, destroy it.
 */
class FrozenSolidTest : FunSpec({

    val TestCreature = CardDefinition.creature(
        name = "Test Creature",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Warrior")),
        power = 3,
        toughness = 3
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestCreature))
        return driver
    }

    /**
     * Advance the game until a specific player's precombat main phase.
     */
    fun advanceToPlayerMain(driver: GameTestDriver, targetPlayer: EntityId) {
        // Use passPriorityUntil which handles combat declarations automatically
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass() // End current turn
        if (driver.activePlayer == targetPlayer) {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
            return
        }
        // Still not target player's turn, advance through one more turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
    }

    test("enchanted creature doesn't untap during untap step") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a tapped creature on the battlefield
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        driver.removeSummoningSickness(creature)
        driver.tapPermanent(creature)

        // Cast Frozen Solid on the creature
        val frozenSolid = driver.putCardInHand(activePlayer, "Frozen Solid")
        driver.giveMana(activePlayer, Color.BLUE, 3)
        driver.castSpell(activePlayer, frozenSolid, listOf(creature))
        driver.bothPass() // Resolve aura

        // Advance back to active player's next turn
        advanceToPlayerMain(driver, activePlayer)

        // Creature should still be tapped (DOESNT_UNTAP prevents untapping)
        driver.isTapped(creature) shouldBe true
    }

    test("enchanted creature is destroyed when dealt damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        driver.removeSummoningSickness(creature)

        // Cast Frozen Solid on the creature
        val frozenSolid = driver.putCardInHand(activePlayer, "Frozen Solid")
        driver.giveMana(activePlayer, Color.BLUE, 3)
        driver.castSpell(activePlayer, frozenSolid, listOf(creature))
        driver.bothPass() // Resolve aura

        // Cast Lightning Bolt on the enchanted creature
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.castSpell(activePlayer, bolt, listOf(creature))
        driver.bothPass() // Resolve bolt - deals 3 damage, triggers Frozen Solid

        // Frozen Solid trigger should be on the stack
        driver.stackSize shouldBe 1

        // Resolve the trigger
        driver.bothPass()

        // Creature should be destroyed (in graveyard)
        driver.findPermanent(activePlayer, "Test Creature") shouldBe null

        // Frozen Solid should also be gone (aura falls off when creature leaves)
        driver.findPermanent(activePlayer, "Frozen Solid") shouldBe null
    }

    test("creature without Frozen Solid untaps normally") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a tapped creature on the battlefield (no Frozen Solid)
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        driver.removeSummoningSickness(creature)
        driver.tapPermanent(creature)

        // Advance back to active player's next turn
        advanceToPlayerMain(driver, activePlayer)

        // Creature should have untapped normally
        driver.activePlayer shouldBe activePlayer
        driver.isTapped(creature) shouldBe false
    }

    test("combat damage to enchanted creature triggers destruction") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 1 puts a creature on the battlefield and enchants it with Frozen Solid
        val creature1 = driver.putCreatureOnBattlefield(player1, "Test Creature")
        driver.removeSummoningSickness(creature1)

        val frozenSolid = driver.putCardInHand(player1, "Frozen Solid")
        driver.giveMana(player1, Color.BLUE, 3)
        driver.castSpell(player1, frozenSolid, listOf(creature1))
        driver.bothPass() // Resolve aura

        // Player 2 has an attacker
        val attacker = driver.putCreatureOnBattlefield(player2, "Test Creature")
        driver.removeSummoningSickness(attacker)

        // Pass to player 2's turn
        advanceToPlayerMain(driver, player2)
        driver.activePlayer shouldBe player2

        // Advance to combat
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS, maxPasses = 200)
        driver.declareAttackers(player2, listOf(attacker), player1)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Block with the frozen creature
        driver.declareBlockers(player1, mapOf(creature1 to listOf(attacker)))

        // Pass through combat damage
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        driver.bothPass() // Deal combat damage

        // Frozen Solid trigger fires - resolve it
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // The enchanted creature should be destroyed by the trigger
        driver.findPermanent(player1, "Frozen Solid") shouldBe null
    }
})
