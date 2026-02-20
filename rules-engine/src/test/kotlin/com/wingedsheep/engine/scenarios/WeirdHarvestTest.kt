package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Weird Harvest.
 *
 * Weird Harvest: {X}{G}{G}
 * Sorcery
 * Each player may search their library for up to X creature cards, reveal those cards,
 * put them into their hand, then shuffle.
 */
class WeirdHarvestTest : FunSpec({

    val WeirdHarvest = CardDefinition.sorcery(
        name = "Weird Harvest",
        manaCost = ManaCost.parse("{X}{G}{G}"),
        oracleText = "Each player may search their library for up to X creature cards, reveal those cards, put them into their hand, then shuffle.",
        script = CardScript.spell(
            effect = EffectPatterns.eachPlayerSearchesLibrary(
                filter = Filters.Creature,
                count = DynamicAmount.XValue
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WeirdHarvest))
        return driver
    }

    test("Weird Harvest with X=1 allows each player to search for a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val startingActiveHandSize = driver.state.getHand(activePlayer).size
        val startingOpponentHandSize = driver.state.getHand(opponent).size

        // Put Weird Harvest in hand
        val weirdHarvest = driver.putCardInHand(activePlayer, "Weird Harvest")

        // Give mana for X=1: {1}{G}{G} = 1 generic + 2 green = 3 mana
        driver.giveMana(activePlayer, Color.GREEN, 3)

        // Cast Weird Harvest with X=1
        driver.castXSpell(activePlayer, weirdHarvest, xValue = 1)
        driver.bothPass()

        // Active player should get a search library decision first (APNAP order)
        val decision1 = driver.pendingDecision
        decision1.shouldBeInstanceOf<SelectCardsDecision>()
        decision1.playerId shouldBe activePlayer
        decision1.maxSelections shouldBe 1

        // Find a Grizzly Bears in the options
        val activeCreature = decision1.options.firstOrNull { cardId ->
            decision1.cardInfo?.get(cardId)?.name == "Grizzly Bears"
        }
        activeCreature shouldNotBe null

        // Select it
        driver.submitDecision(activePlayer, CardsSelectedResponse(decision1.id, listOf(activeCreature!!)))

        // Now opponent should get their search library decision
        val decision2 = driver.pendingDecision
        decision2.shouldBeInstanceOf<SelectCardsDecision>()
        decision2.playerId shouldBe opponent
        decision2.maxSelections shouldBe 1

        // Opponent selects a creature
        val opponentCreature = decision2.options.firstOrNull { cardId ->
            decision2.cardInfo?.get(cardId)?.name == "Grizzly Bears"
        }
        opponentCreature shouldNotBe null

        driver.submitDecision(opponent, CardsSelectedResponse(decision2.id, listOf(opponentCreature!!)))

        // Both players should have gained a card in hand
        // Active player: +1 (put weird harvest) -1 (cast it) +1 (searched) = +1
        driver.state.getHand(activePlayer).size shouldBe startingActiveHandSize + 1
        // Opponent: +1 (searched)
        driver.state.getHand(opponent).size shouldBe startingOpponentHandSize + 1

        // The found creatures should be in hand
        driver.findCardInHand(activePlayer, "Grizzly Bears") shouldNotBe null
        driver.findCardInHand(opponent, "Grizzly Bears") shouldNotBe null
    }

    test("Weird Harvest with X=2 allows each player to search for up to 2 creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Count bears already in hand from initial draw
        val startingActiveBears = driver.state.getHand(activePlayer).count { cardId ->
            driver.getCardName(cardId) == "Grizzly Bears"
        }

        val weirdHarvest = driver.putCardInHand(activePlayer, "Weird Harvest")
        // X=2: {2}{G}{G} = 4 mana
        driver.giveMana(activePlayer, Color.GREEN, 4)

        driver.castXSpell(activePlayer, weirdHarvest, xValue = 2)
        driver.bothPass()

        // Active player search
        val decision1 = driver.pendingDecision
        decision1.shouldBeInstanceOf<SelectCardsDecision>()
        decision1.maxSelections shouldBe 2

        // Select 2 creatures
        val creatures1 = decision1.options.filter { cardId ->
            decision1.cardInfo?.get(cardId)?.name == "Grizzly Bears"
        }.take(2)
        creatures1.size shouldBe 2

        driver.submitDecision(activePlayer, CardsSelectedResponse(decision1.id, creatures1))

        // Opponent search
        val decision2 = driver.pendingDecision
        decision2.shouldBeInstanceOf<SelectCardsDecision>()
        decision2.maxSelections shouldBe 2

        val creatures2 = decision2.options.filter { cardId ->
            decision2.cardInfo?.get(cardId)?.name == "Grizzly Bears"
        }.take(2)
        creatures2.size shouldBe 2

        driver.submitDecision(opponent, CardsSelectedResponse(decision2.id, creatures2))

        // Active player should have 2 more Grizzly Bears in hand than before
        val activeBearsInHand = driver.state.getHand(activePlayer).count { cardId ->
            driver.getCardName(cardId) == "Grizzly Bears"
        }
        activeBearsInHand shouldBe startingActiveBears + 2
    }

    test("player can fail to find even with matching cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val startingActiveHandSize = driver.state.getHand(activePlayer).size

        val weirdHarvest = driver.putCardInHand(activePlayer, "Weird Harvest")
        driver.giveMana(activePlayer, Color.GREEN, 3)

        driver.castXSpell(activePlayer, weirdHarvest, xValue = 1)
        driver.bothPass()

        // Active player fails to find
        val decision1 = driver.pendingDecision
        decision1.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(activePlayer, CardsSelectedResponse(decision1.id, emptyList()))

        // Opponent still gets to search
        val decision2 = driver.pendingDecision
        decision2.shouldBeInstanceOf<SelectCardsDecision>()
        decision2.playerId shouldBe opponent

        // Opponent also fails to find
        driver.submitDecision(opponent, CardsSelectedResponse(decision2.id, emptyList()))

        // Active player hand size: +1 (put weird harvest) -1 (cast) = same
        driver.state.getHand(activePlayer).size shouldBe startingActiveHandSize
    }

    test("Weird Harvest with X=0 does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val startingActiveHandSize = driver.state.getHand(activePlayer).size

        val weirdHarvest = driver.putCardInHand(activePlayer, "Weird Harvest")
        // X=0: just {G}{G} = 2 mana
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castXSpell(activePlayer, weirdHarvest, xValue = 0)
        driver.bothPass()

        // No search decisions should be pending - spell resolves with no effect
        // Hand size: +1 (put card) -1 (cast) = same
        driver.state.getHand(activePlayer).size shouldBe startingActiveHandSize
    }

    test("only creature cards appear in search results") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 5, "Forest" to 35),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val weirdHarvest = driver.putCardInHand(activePlayer, "Weird Harvest")
        driver.giveMana(activePlayer, Color.GREEN, 3)

        driver.castXSpell(activePlayer, weirdHarvest, xValue = 1)
        driver.bothPass()

        // All options should be creature cards (Grizzly Bears), not Forests
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()

        for (cardId in decision.options) {
            decision.cardInfo?.get(cardId)?.name shouldBe "Grizzly Bears"
        }
    }
})
