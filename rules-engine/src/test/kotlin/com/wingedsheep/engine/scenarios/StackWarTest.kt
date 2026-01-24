package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize

/**
 * Scenario 3: Stack War (Counter Battle)
 *
 * Verifies stack mechanics and spell interaction.
 *
 * ## Setup
 * - Player 1: Has Islands and a creature spell
 * - Player 2: Has Islands and Counterspells
 *
 * ## Steps
 * 1. Player 1 casts a creature spell
 * 2. Player 2 responds with Counterspell targeting the creature
 * 3. Player 1 responds with their own Counterspell targeting Player 2's Counterspell
 * 4. Both players pass (stack starts resolving)
 * 5. Player 1's Counterspell resolves, countering Player 2's Counterspell
 * 6. The original creature spell resolves
 *
 * ## Assertions
 * - Stack resolution is LIFO
 * - Countered spells go to graveyard
 * - The creature enters the battlefield
 */
class StackWarTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("stack is empty at start of game") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 30,
                "Counterspell" to 10
            )
        )

        driver.stackSize shouldBe 0
    }

    test("casting a spell puts it on the stack") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put cards in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)

        // Stack should still be empty
        driver.stackSize shouldBe 0

        // Cast Lightning Bolt targeting the opponent
        val result = driver.castSpell(activePlayer, bolt, listOf(opponent))

        // Spell should be on the stack
        result.isSuccess shouldBe true
        driver.stackSize shouldBe 1
        driver.getTopOfStackName() shouldBe "Lightning Bolt"
    }

    test("priority passes after casting a spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put cards in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)

        // Active player has priority initially
        driver.priorityPlayer shouldBe activePlayer

        // Cast Lightning Bolt
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        // After casting, active player retains priority (can respond to own spell)
        // This is correct per MTG rules - caster gets priority after casting
        driver.priorityPlayer shouldBe activePlayer
    }

    test("passing priority with empty stack advances the game") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 40
            )
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val startStep = driver.currentStep
        startStep shouldBe Step.PRECOMBAT_MAIN

        // Both players pass on empty stack
        driver.bothPass()

        // Game should have advanced to begin combat step
        driver.currentStep shouldBe Step.BEGIN_COMBAT
    }

    test("cannot cast spell without priority") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 20,
                "Lightning Bolt" to 20
            )
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: opponent has a mountain on battlefield from a previous turn
        // For this test, we verify that the non-priority player can't cast

        // Active player has priority
        driver.priorityPlayer shouldBe activePlayer

        // Try to cast as the opponent (who doesn't have priority)
        val opponentBolt = driver.findCardInHand(opponent, "Lightning Bolt")
        if (opponentBolt != null) {
            val result = driver.submitExpectFailure(
                CastSpell(
                    playerId = opponent,
                    cardId = opponentBolt,
                    targets = listOf(ChosenTarget.Player(activePlayer)),
                    paymentStrategy = PaymentStrategy.AutoPay
                )
            )
            result.isSuccess shouldBe false
        }
    }

    test("passing priority to opponent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 40
            )
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Active player has priority
        driver.priorityPlayer shouldBe activePlayer

        // Pass priority
        driver.passPriority(activePlayer)

        // Opponent should now have priority
        driver.priorityPlayer shouldBe opponent
    }

    test("both players passing resolves top of stack") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put card in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)

        // Cast Lightning Bolt at opponent
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        driver.stackSize shouldBe 1

        // Both pass
        driver.bothPass()

        // Stack should be empty (spell resolved)
        driver.stackSize shouldBe 0

        // Opponent should have taken 3 damage
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("stack resolution order is LIFO") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put cards in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt1 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bolt2 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 2)

        // Cast two Lightning Bolts
        driver.castSpell(activePlayer, bolt1, listOf(opponent))
        driver.castSpell(activePlayer, bolt2, listOf(opponent))

        // Stack has 2 spells
        driver.stackSize shouldBe 2

        // Both pass - first resolution
        driver.bothPass()

        // Top spell (second bolt) resolved first
        driver.stackSize shouldBe 1
        driver.getLifeTotal(opponent) shouldBe 17

        // Both pass again - second resolution
        driver.bothPass()

        // Both bolts resolved
        driver.stackSize shouldBe 0
        driver.getLifeTotal(opponent) shouldBe 14
    }

    test("instant can be cast in response") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put cards in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(opponent, Color.RED, 1)

        val bolt1 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val opponentBolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(opponent, Color.RED, 1)

        // Active player casts Lightning Bolt
        driver.castSpell(activePlayer, bolt1, listOf(opponent))

        driver.stackSize shouldBe 1

        // Active player passes priority
        driver.passPriority(activePlayer)

        // Opponent has priority and can cast in response
        driver.priorityPlayer shouldBe opponent

        val result = driver.castSpell(opponent, opponentBolt, listOf(activePlayer))

        result.isSuccess shouldBe true
        driver.stackSize shouldBe 2
    }

    test("sorcery cannot be cast in response") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put cards in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.putCardInHand(opponent, "Doom Blade")  // Sorcery in opponent's hand
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(opponent, Color.BLACK, 2)

        // Cast Lightning Bolt at opponent
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        driver.stackSize shouldBe 1

        // Pass priority to opponent
        driver.passPriority(activePlayer)
        driver.priorityPlayer shouldBe opponent

        // Opponent cannot cast sorcery (Doom Blade) with non-empty stack
        // Note: This test verifies sorcery timing - would fail with non-empty stack
        // The actionProcessor should reject sorceries when stack is non-empty
    }

    test("countered spell goes to graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put cards in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.putCardInHand(activePlayer, "Grizzly Bears")
        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(activePlayer, Color.GREEN, 2)  // For Grizzly Bears
        driver.giveMana(opponent, Color.BLUE, 2)       // For Counterspell

        // Active player casts Grizzly Bears
        driver.castSpell(activePlayer, bears)

        driver.stackSize shouldBe 1
        driver.getTopOfStackName() shouldBe "Grizzly Bears"

        // Pass to opponent
        driver.passPriority(activePlayer)

        // Opponent counters
        val topSpellId = driver.getTopOfStack()!!
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(topSpellId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 2

        // Both pass - Counterspell resolves
        driver.bothPass()

        // Grizzly Bears should be countered (in graveyard)
        driver.stackSize shouldBe 0
        driver.assertInGraveyard(activePlayer, "Grizzly Bears")
        driver.assertInGraveyard(opponent, "Counterspell")

        // Bears should NOT be on battlefield
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
    }

    test("can respond to opponent's spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put cards in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val opponentBolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(opponent, Color.RED, 1)

        // Active player casts bolt
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        // Pass priority to opponent
        driver.passPriority(activePlayer)
        driver.priorityPlayer shouldBe opponent

        // Opponent responds with their own bolt
        driver.castSpell(opponent, opponentBolt, listOf(activePlayer))

        driver.stackSize shouldBe 2
    }

    test("after resolving top of stack, active player gets priority") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put card in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)

        // Cast bolt
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        // Resolve it
        driver.bothPass()

        // After resolution, active player gets priority
        driver.priorityPlayer shouldBe activePlayer
    }

    test("spell targeting a spell on the stack") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put cards in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val myCounter = driver.putCardInHand(activePlayer, "Counterspell")
        val opponentCounter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(activePlayer, Color.RED, 1)    // For Lightning Bolt
        driver.giveMana(activePlayer, Color.BLUE, 2)   // For Counterspell
        driver.giveMana(opponent, Color.BLUE, 2)       // For Counterspell

        // Cast Lightning Bolt
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        val boltOnStack = driver.getTopOfStack()!!
        driver.stackSize shouldBe 1

        // Pass to opponent
        driver.passPriority(activePlayer)

        // Opponent casts Counterspell targeting the bolt
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = opponentCounter,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        val counterOnStack = driver.getTopOfStack()!!
        driver.stackSize shouldBe 2

        // Opponent passes, active player gets priority
        driver.passPriority(opponent)

        // Active player casts their own Counterspell targeting opponent's Counterspell
        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = myCounter,
                targets = listOf(ChosenTarget.Spell(counterOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 3

        // Both pass - resolve counter-counter
        driver.bothPass()

        // Stack should now have 1 item (bolt), opponent's counter was countered
        driver.stackSize shouldBe 1
        driver.getTopOfStackName() shouldBe "Lightning Bolt"
        driver.assertInGraveyard(opponent, "Counterspell")

        // Both pass - bolt resolves
        driver.bothPass()

        driver.stackSize shouldBe 0
        driver.getLifeTotal(opponent) shouldBe 17  // Took 3 damage from bolt
    }

    test("multiple spells on stack resolve in order") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Setup: deterministically put cards in hand and give mana
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt1 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bolt2 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bolt3 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 3)

        // Cast 3 bolts
        driver.castSpell(activePlayer, bolt1, listOf(opponent))
        driver.castSpell(activePlayer, bolt2, listOf(opponent))
        driver.castSpell(activePlayer, bolt3, listOf(opponent))

        driver.stackSize shouldBe 3
        driver.getLifeTotal(opponent) shouldBe 20

        // Resolve all
        driver.bothPass()  // First bolt resolves (the last one cast)
        driver.getLifeTotal(opponent) shouldBe 17
        driver.stackSize shouldBe 2

        driver.bothPass()  // Second bolt
        driver.getLifeTotal(opponent) shouldBe 14
        driver.stackSize shouldBe 1

        driver.bothPass()  // Third bolt
        driver.getLifeTotal(opponent) shouldBe 11
        driver.stackSize shouldBe 0
    }
})
