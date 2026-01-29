package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameEndedEvent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Rule 704.5c: A player who was unable to draw a card
 * from an empty library loses the game.
 */
class EmptyLibraryLossTest : FunSpec({

    // Define a simple draw spell for testing
    val Divination = CardDefinition.sorcery(
        name = "Divination",
        manaCost = ManaCost.parse("{2}{U}"),
        oracleText = "Draw two cards.",
        script = CardScript.spell(
            effect = DrawCardsEffect(
                count = DynamicAmount.Fixed(2),
                target = EffectTarget.Controller
            )
        )
    )

    /**
     * Helper to empty a player's library.
     */
    fun GameTestDriver.emptyLibrary(playerId: com.wingedsheep.sdk.model.EntityId) {
        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
        val library = state.getZone(libraryZone)

        // Remove all cards from the library
        var newState = state
        for (cardId in library) {
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.withoutEntity(cardId)
        }

        // Update state via reflection to access private field
        val stateField = this::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        stateField.set(this, newState)
    }

    test("player loses when attempting to draw from empty library") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Divination))

        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty the active player's library
        driver.emptyLibrary(activePlayer)

        // Verify library is empty
        driver.state.getLibrary(activePlayer).size shouldBe 0

        // Put Divination in hand and give mana
        val divination = driver.putCardInHand(activePlayer, "Divination")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast Divination (trying to draw 2 cards from empty library)
        driver.castSpell(activePlayer, divination)
        driver.bothPass()

        // The game should be over with opponent winning
        driver.state.gameOver.shouldBeTrue()
        driver.state.winnerId shouldBe opponent
    }

    test("player marked with PlayerLostComponent when draw fails") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Divination))

        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty the active player's library
        driver.emptyLibrary(activePlayer)

        // Put Divination in hand and give mana
        val divination = driver.putCardInHand(activePlayer, "Divination")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast Divination
        driver.castSpell(activePlayer, divination)
        driver.bothPass()

        // Check that player is marked as lost due to empty library
        val lostComponent = driver.state.getEntity(activePlayer)?.get<PlayerLostComponent>()
        lostComponent shouldNotBe null
        lostComponent!!.reason shouldBe LossReason.EMPTY_LIBRARY
    }

    test("DrawFailedEvent is emitted when drawing from empty library") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Divination))

        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty the active player's library
        driver.emptyLibrary(activePlayer)

        // Put Divination in hand and give mana
        val divination = driver.putCardInHand(activePlayer, "Divination")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast Divination
        driver.castSpell(activePlayer, divination)
        driver.bothPass()

        // Check that DrawFailedEvent was emitted
        val drawFailedEvent = driver.events.filterIsInstance<DrawFailedEvent>()
            .find { it.playerId == activePlayer }
        drawFailedEvent shouldNotBe null
        drawFailedEvent!!.reason shouldBe "Empty library"
    }

    test("GameEndedEvent has correct reason for empty library loss") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Divination))

        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty the active player's library
        driver.emptyLibrary(activePlayer)

        // Put Divination in hand and give mana
        val divination = driver.putCardInHand(activePlayer, "Divination")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast Divination
        driver.castSpell(activePlayer, divination)
        driver.bothPass()

        // Check GameEndedEvent
        val gameEndedEvent = driver.events.filterIsInstance<GameEndedEvent>().firstOrNull()
        gameEndedEvent shouldNotBe null
        gameEndedEvent!!.winnerId shouldBe opponent
        gameEndedEvent.reason shouldBe GameEndReason.DECK_EMPTY
    }

    test("partial draw succeeds then fails on empty library") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Divination))

        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Leave exactly 1 card in library (Divination draws 2, so it will fail on second draw)
        val libraryZone = ZoneKey(activePlayer, ZoneType.LIBRARY)
        val library = driver.state.getLibrary(activePlayer)

        // Remove all but one card
        var newState = driver.state
        for (i in 0 until library.size - 1) {
            val cardId = library[i]
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.withoutEntity(cardId)
        }

        // Update state via reflection
        val stateField = driver::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        stateField.set(driver, newState)

        // Verify library has exactly 1 card
        driver.state.getLibrary(activePlayer).size shouldBe 1
        val startingHandSize = driver.state.getHand(activePlayer).size

        // Put Divination in hand and give mana
        val divination = driver.putCardInHand(activePlayer, "Divination")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast Divination (draws 1 card successfully, then fails on second)
        driver.castSpell(activePlayer, divination)
        driver.bothPass()

        // Game should be over - player tried to draw from empty library
        driver.state.gameOver.shouldBeTrue()
        driver.state.winnerId shouldBe opponent

        // Player should have drawn exactly 1 card before the loss
        // Hand = starting + 1 (put divination) - 1 (cast) + 1 (drew 1 before fail)
        driver.state.getHand(activePlayer).size shouldBe startingHandSize + 1
    }

    test("player loses during draw step when library is empty") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)

        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty the opponent's library (they will draw next turn)
        driver.emptyLibrary(opponent)
        driver.state.getLibrary(opponent).size shouldBe 0

        // Pass through to the next player's turn by advancing through all phases
        // This will eventually reach the opponent's draw step
        driver.passPriorityUntil(Step.END)
        driver.bothPass() // End step, priority passes, then cleanup triggers next turn

        // Keep passing until the game ends (opponent draws from empty library)
        var safetyCount = 0
        while (!driver.state.gameOver && safetyCount < 20) {
            driver.passPriority(driver.state.priorityPlayerId ?: break)
            safetyCount++
        }

        // The game should be over with the original active player winning
        driver.state.gameOver.shouldBeTrue()
        driver.state.winnerId shouldBe activePlayer

        // Verify the loss reason
        val lostComponent = driver.state.getEntity(opponent)?.get<PlayerLostComponent>()
        lostComponent shouldNotBe null
        lostComponent!!.reason shouldBe LossReason.EMPTY_LIBRARY
    }

    test("TurnManager.drawCards marks player as lost when library is empty") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)

        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val player1 = driver.activePlayer!!

        // Empty the player's library
        driver.emptyLibrary(player1)
        driver.state.getLibrary(player1).size shouldBe 0

        // Use TurnManager.drawCards directly (this is what performDrawStep calls)
        val turnManager = com.wingedsheep.engine.core.TurnManager()
        val result = turnManager.drawCards(driver.state, player1, 1)

        // The draw should succeed (return success) but mark the player as lost
        result.isSuccess.shouldBeTrue()

        // Check that player is marked as lost due to empty library
        val lostComponent = result.newState.getEntity(player1)?.get<PlayerLostComponent>()
        lostComponent shouldNotBe null
        lostComponent!!.reason shouldBe LossReason.EMPTY_LIBRARY

        // Check that DrawFailedEvent was emitted
        val drawFailedEvent = result.events.filterIsInstance<DrawFailedEvent>()
            .find { it.playerId == player1 }
        drawFailedEvent shouldNotBe null
        drawFailedEvent!!.reason shouldBe "Library is empty"
    }
})
