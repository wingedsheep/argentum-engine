package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.HearthbornBattler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Hearthborn Battler's NthSpellCast trigger.
 *
 * Hearthborn Battler: {2}{R}
 * Creature — Lizard Warlock
 * 2/3
 * Haste
 * Whenever a player casts their second spell each turn,
 * this creature deals 2 damage to target opponent.
 */
class HearthbornBattlerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HearthbornBattler))
        return driver
    }

    test("triggers when controller casts their second spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Lightning Bolt" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player1, "Hearthborn Battler")

        // Cast first spell — no trigger expected
        val bolt1 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt1, listOf(player2))
        // Resolve: both players pass
        driver.passPriority(player1)
        driver.passPriority(player2)
        driver.getLifeTotal(player2) shouldBe 17

        // Cast second spell — trigger fires, deals 2 damage to target opponent
        val bolt2 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt2, listOf(player2))

        // Stack now has: trigger (top), Lightning Bolt (bottom)
        // Trigger auto-targets only opponent in 2-player game
        // Resolve trigger first (both pass)
        driver.passPriority(player1)
        driver.passPriority(player2)
        // Trigger resolved: 17 - 2 = 15

        // Resolve bolt (both pass)
        driver.passPriority(player1)
        driver.passPriority(player2)
        // Bolt resolved: 15 - 3 = 12

        driver.getLifeTotal(player2) shouldBe 12
    }

    test("triggers when opponent casts their second spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Lightning Bolt" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player1, "Hearthborn Battler")

        // Pass to player 2
        driver.passPriority(player1)

        // Player 2 casts first spell — no trigger
        val bolt1 = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.castSpell(player2, bolt1, listOf(player1))
        driver.passPriority(player2)
        driver.passPriority(player1)
        driver.getLifeTotal(player1) shouldBe 17

        // After bolt resolves, active player (player1) gets priority
        // Pass to player 2 so they can cast
        driver.passPriority(player1)

        // Player 2 casts second spell — trigger fires
        val bolt2 = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.castSpell(player2, bolt2, listOf(player1))

        // Stack: trigger (top) + bolt (bottom)
        // After casting, caster (player2) retains priority
        // Resolve trigger
        driver.passPriority(player2)
        driver.passPriority(player1)

        // After trigger resolves, active player (player1) gets priority
        driver.passPriority(player1)
        driver.passPriority(player2)

        driver.getLifeTotal(player1) shouldBe 14 // 17 - 3
        driver.getLifeTotal(player2) shouldBe 18 // 20 - 2
    }

    test("does not trigger on first spell only") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Lightning Bolt" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player1, "Hearthborn Battler")

        // Cast only one spell
        val bolt = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)

        // Only bolt damage
        driver.getLifeTotal(player2) shouldBe 17
    }

    test("triggers when Hearthborn Battler itself is the second spell cast") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Lightning Bolt" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast first spell (Lightning Bolt) — no trigger (HB not on battlefield yet)
        val bolt = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)
        driver.getLifeTotal(player2) shouldBe 17 // 20 - 3

        // Cast Hearthborn Battler as the second spell — its own trigger should fire
        val hb = driver.putCardInHand(player1, "Hearthborn Battler")
        driver.giveMana(player1, Color.RED, 1)
        driver.giveColorlessMana(player1, 2)
        driver.castSpell(player1, hb, emptyList())

        // Stack: trigger (top) + Hearthborn Battler (bottom)
        // Trigger auto-targets only opponent in 2-player game.
        driver.passPriority(player1)
        driver.passPriority(player2)
        // Trigger resolves: 17 - 2 = 15

        // Resolve HB (enters battlefield with haste)
        driver.passPriority(player1)
        driver.passPriority(player2)

        driver.getLifeTotal(player2) shouldBe 15
    }

    test("does not trigger on third spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Lightning Bolt" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player1, "Hearthborn Battler")

        // Cast first spell
        val bolt1 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt1, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)
        // 20 - 3 = 17

        // Cast second spell — trigger fires
        val bolt2 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt2, listOf(player2))
        // Resolve trigger
        driver.passPriority(player1)
        driver.passPriority(player2)
        // 17 - 2 = 15
        // Resolve bolt
        driver.passPriority(player1)
        driver.passPriority(player2)
        // 15 - 3 = 12

        // Cast third spell — no trigger
        val bolt3 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt3, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)
        // 12 - 3 = 9

        driver.getLifeTotal(player2) shouldBe 9
    }
})
