package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ReadTheRunesEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Read the Runes.
 *
 * Read the Runes: {X}{U}
 * Instant
 * Draw X cards. For each card drawn this way, discard a card unless you sacrifice a permanent.
 */
class ReadTheRunesTest : FunSpec({

    val ReadTheRunes = CardDefinition.instant(
        name = "Read the Runes",
        manaCost = ManaCost.parse("{X}{U}"),
        oracleText = "Draw X cards. For each card drawn this way, discard a card unless you sacrifice a permanent.",
        script = CardScript.spell(
            effect = ReadTheRunesEffect
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ReadTheRunes))
        return driver
    }

    test("draw X cards then discard X cards when no permanents") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val startingHandSize = driver.getHandSize(activePlayer)

        // Put Read the Runes in hand and give mana for X=2
        val rtr = driver.putCardInHand(activePlayer, "Read the Runes")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast with X=2
        driver.castXSpell(activePlayer, rtr, xValue = 2)
        driver.bothPass()

        // Should have drawn 2 cards, now must discard 2 (no permanents to sacrifice)
        // First discard choice (auto if only 1 possible, otherwise pick)
        driver.isPaused shouldBe true
        val hand = driver.getHand(activePlayer)
        driver.submitCardSelection(activePlayer, listOf(hand.first()))

        // Second discard choice
        driver.isPaused shouldBe true
        val hand2 = driver.getHand(activePlayer)
        driver.submitCardSelection(activePlayer, listOf(hand2.first()))

        // Net result: drew 2, discarded 2, so hand size = starting + 1 (put RTR) - 1 (cast RTR) + 2 (drew) - 2 (discarded)
        driver.getHandSize(activePlayer) shouldBe startingHandSize
    }

    test("sacrifice permanents instead of discarding") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures on the battlefield
        val creature1 = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val creature2 = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val startingHandSize = driver.getHandSize(activePlayer)

        val rtr = driver.putCardInHand(activePlayer, "Read the Runes")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        driver.castXSpell(activePlayer, rtr, xValue = 2)
        driver.bothPass()

        // First choice: sacrifice creature1
        driver.isPaused shouldBe true
        driver.submitCardSelection(activePlayer, listOf(creature1))

        // Second choice: sacrifice creature2
        driver.isPaused shouldBe true
        driver.submitCardSelection(activePlayer, listOf(creature2))

        // Net result: drew 2, sacrificed 2 permanents, no discards
        // Hand size = starting + 1 (put RTR) - 1 (cast RTR) + 2 (drew) = starting + 2
        driver.getHandSize(activePlayer) shouldBe startingHandSize + 2

        // Both creatures should be gone
        val sacrificeEvents = driver.events.filterIsInstance<PermanentsSacrificedEvent>()
        sacrificeEvents.flatMap { it.permanentIds }.toSet() shouldBe setOf(creature1, creature2)
    }

    test("mix of sacrifice and discard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put one creature on the battlefield
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val startingHandSize = driver.getHandSize(activePlayer)

        val rtr = driver.putCardInHand(activePlayer, "Read the Runes")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        driver.castXSpell(activePlayer, rtr, xValue = 2)
        driver.bothPass()

        // First choice: sacrifice creature
        driver.isPaused shouldBe true
        driver.submitCardSelection(activePlayer, listOf(creature))

        // Second choice: no permanents left, must discard
        driver.isPaused shouldBe true
        val hand = driver.getHand(activePlayer)
        driver.submitCardSelection(activePlayer, listOf(hand.first()))

        // Net result: drew 2, sacrificed 1, discarded 1
        // Hand size = starting + 1 - 1 + 2 - 1 = starting + 1
        driver.getHandSize(activePlayer) shouldBe startingHandSize + 1
    }

    test("choose to discard instead of sacrificing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creature on the battlefield
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val rtr = driver.putCardInHand(activePlayer, "Read the Runes")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        driver.castXSpell(activePlayer, rtr, xValue = 1)
        driver.bothPass()

        // Player has a permanent but chooses to discard instead (select 0 permanents)
        driver.isPaused shouldBe true
        driver.submitCardSelection(activePlayer, emptyList())

        // Now must discard a card
        driver.isPaused shouldBe true
        val hand = driver.getHand(activePlayer)
        driver.submitCardSelection(activePlayer, listOf(hand.first()))

        // Creature should still be on battlefield
        val sacrificeEvents = driver.events.filterIsInstance<PermanentsSacrificedEvent>()
        sacrificeEvents.flatMap { it.permanentIds }.contains(creature) shouldBe false
    }

    test("X=0 does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val startingHandSize = driver.getHandSize(activePlayer)

        val rtr = driver.putCardInHand(activePlayer, "Read the Runes")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        driver.castXSpell(activePlayer, rtr, xValue = 0)
        driver.bothPass()

        // No draws, no discards
        driver.isPaused shouldBe false
        driver.getHandSize(activePlayer) shouldBe startingHandSize
    }
})
