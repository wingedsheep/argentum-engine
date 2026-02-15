package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithDiscardEffect
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Words of Waste.
 *
 * Words of Waste: {2}{B}
 * Enchantment
 * {1}: The next time you would draw a card this turn, each opponent discards a card instead.
 */
class WordsOfWasteTest : FunSpec({

    val WordsOfWaste = card("Words of Waste") {
        manaCost = "{2}{B}"
        typeLine = "Enchantment"

        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = ReplaceNextDrawWithDiscardEffect
        }
    }

    // A simple draw spell for testing
    val Inspiration = CardDefinition.instant(
        name = "Inspiration",
        manaCost = ManaCost.parse("{3}{U}"),
        oracleText = "Draw two cards.",
        script = CardScript.spell(effect = DrawCardsEffect(2))
    )

    val abilityId = WordsOfWaste.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WordsOfWaste, Inspiration))
        return driver
    }

    /**
     * Helper to resolve all pending discard decisions by always selecting the first option.
     */
    fun GameTestDriver.resolveAllDiscardDecisions() {
        while (pendingDecision is SelectCardsDecision) {
            val decision = pendingDecision as SelectCardsDecision
            submitCardSelection(decision.playerId, listOf(decision.options.first()))
        }
    }

    test("activating Words of Waste replaces next draw with opponent discard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of Waste")

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana for activation ({1}) and for Inspiration ({3}{U})
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Waste")!!
        val initialHandSize = driver.getHandSize(activePlayer)
        val opponentInitialHandSize = driver.getHandSize(opponent)

        // Activate Words of Waste
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration to draw 2 cards
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // First draw was replaced - opponent must choose a card to discard
        val decision = driver.pendingDecision as SelectCardsDecision
        decision.playerId shouldBe opponent
        driver.submitCardSelection(opponent, listOf(decision.options.first()))

        // Second draw proceeds normally
        // Controller: initialHandSize + 1 (put Inspiration) - 1 (cast Inspiration) + 1 (normal 2nd draw) = initialHandSize + 1
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
        // Opponent: lost 1 card from discard
        driver.getHandSize(opponent) shouldBe opponentInitialHandSize - 1
    }

    test("Words of Waste shield only replaces one draw from a multi-draw spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of Waste")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Waste")!!

        // Activate once
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) - only 1st draw is replaced
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        val handSizeBeforeCast = driver.getHandSize(activePlayer)
        val opponentHandSizeBeforeCast = driver.getHandSize(opponent)
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Opponent must choose a card to discard
        driver.resolveAllDiscardDecisions()

        // 1st draw replaced with discard, 2nd draw normal
        // handSizeBeforeCast - 1 (cast Inspiration) + 1 (normal 2nd draw) = handSizeBeforeCast
        driver.getHandSize(activePlayer) shouldBe handSizeBeforeCast
        // Opponent lost 1 card
        driver.getHandSize(opponent) shouldBe opponentHandSizeBeforeCast - 1
    }

    test("activating multiple times stacks shields for multiple draws") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of Waste")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Waste")!!

        // Activate twice to create two shields
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) - both draws are replaced
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        val handSizeBeforeCast = driver.getHandSize(activePlayer)
        val opponentHandSizeBeforeCast = driver.getHandSize(opponent)
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Resolve all discard decisions
        driver.resolveAllDiscardDecisions()

        // Both draws replaced with opponent discards
        // handSizeBeforeCast - 1 (cast Inspiration) + 0 (both draws replaced) = handSizeBeforeCast - 1
        driver.getHandSize(activePlayer) shouldBe handSizeBeforeCast - 1
        // Opponent lost 2 cards
        driver.getHandSize(opponent) shouldBe opponentHandSizeBeforeCast - 2
    }

    test("Words of Waste shield expires at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Waste")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Activate during main phase
        driver.giveMana(activePlayer, Color.BLACK, 1)
        val wordsId = driver.findPermanent(activePlayer, "Words of Waste")!!
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Verify shield exists
        driver.state.floatingEffects.size shouldBe 1

        // Advance past current turn to the next player's turn
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // That was opponent's main. Now advance to our next main.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Shield should have expired at the end of our first turn
        driver.state.floatingEffects.size shouldBe 0
    }

    test("Words of Waste does nothing if opponent has no cards in hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of Waste")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty opponent's hand by moving all cards to graveyard
        val opponentHand = driver.getHand(opponent)
        val handZone = ZoneKey(opponent, Zone.HAND)
        val graveyardZone = ZoneKey(opponent, Zone.GRAVEYARD)
        var newState = driver.state
        for (cardId in opponentHand) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }
        driver.replaceState(newState)
        driver.getHandSize(opponent) shouldBe 0

        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Waste")!!
        val initialHandSize = driver.getHandSize(activePlayer)

        // Activate Words of Waste
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration to draw 2 cards
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // First draw was replaced (opponent had no cards, so nothing happened to them)
        // Second draw was normal
        // initialHandSize + 1 (put Inspiration) - 1 (cast Inspiration) + 1 (normal 2nd draw) = initialHandSize + 1
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
        // Opponent still has 0 cards
        driver.getHandSize(opponent) shouldBe 0
    }

    test("Words of Waste with opponent having exactly one card auto-discards it") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of Waste")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Leave opponent with exactly 1 card in hand
        val opponentHand = driver.getHand(opponent)
        val handZone = ZoneKey(opponent, Zone.HAND)
        val graveyardZone = ZoneKey(opponent, Zone.GRAVEYARD)
        var newState = driver.state
        for (cardId in opponentHand.drop(1)) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }
        driver.replaceState(newState)
        driver.getHandSize(opponent) shouldBe 1

        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Waste")!!
        val initialHandSize = driver.getHandSize(activePlayer)

        // Activate Words of Waste
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration to draw 2 cards
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Opponent had exactly 1 card, so it was auto-discarded (no decision needed)
        // Second draw was normal
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
        driver.getHandSize(opponent) shouldBe 0
    }
})
