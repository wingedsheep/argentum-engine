package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Read the Runes.
 *
 * Read the Runes: {X}{U}
 * Instant
 * Draw X cards. For each card drawn this way, discard a card unless you sacrifice a permanent.
 *
 * Per the SDK's atomic-effect composition: each iteration presents a [ChooseOptionDecision]
 * with "Sacrifice a permanent" (index 0) and "Discard a card" (index 1). When only one
 * option is feasible, the choice auto-resolves and the sub-pipeline's
 * [SelectCardsDecision] is presented directly.
 */
class ReadTheRunesTest : FunSpec({

    val sacrificeOption = 0
    val discardOption = 1

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
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

        val rtr = driver.putCardInHand(activePlayer, "Read the Runes")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        driver.castXSpell(activePlayer, rtr, xValue = 2)
        driver.bothPass()

        // No permanents → only Discard is feasible → auto-resolves to SelectCardsDecision.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val hand = driver.getHand(activePlayer)
        driver.submitCardSelection(activePlayer, listOf(hand.first()))

        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val hand2 = driver.getHand(activePlayer)
        driver.submitCardSelection(activePlayer, listOf(hand2.first()))

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

        val creature1 = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val creature2 = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val startingHandSize = driver.getHandSize(activePlayer)

        val rtr = driver.putCardInHand(activePlayer, "Read the Runes")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        driver.castXSpell(activePlayer, rtr, xValue = 2)
        driver.bothPass()

        // Iteration 1: both feasible → ChooseOption → Sacrifice → pick creature1.
        val choice1 = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(choice1.id, sacrificeOption))
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(activePlayer, listOf(creature1))

        // Iteration 2: both feasible → ChooseOption → Sacrifice → auto-picks creature2
        // (SelectFromCollection.ChooseExactly(1) auto-selects when only one eligible card remains).
        val choice2 = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(choice2.id, sacrificeOption))

        driver.getHandSize(activePlayer) shouldBe startingHandSize + 2

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

        val creature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val startingHandSize = driver.getHandSize(activePlayer)

        val rtr = driver.putCardInHand(activePlayer, "Read the Runes")
        driver.giveMana(activePlayer, Color.BLUE, 3)

        driver.castXSpell(activePlayer, rtr, xValue = 2)
        driver.bothPass()

        // Iteration 1: both feasible → Sacrifice. The lone creature is auto-selected.
        val choice1 = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(choice1.id, sacrificeOption))

        // Iteration 2: no permanents left → only Discard feasible → auto-resolves to a
        // SelectCardsDecision asking which card from hand to discard.
        val discardPrompt = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val hand = driver.getHand(activePlayer)
        driver.submitDecision(
            activePlayer,
            com.wingedsheep.engine.core.CardsSelectedResponse(discardPrompt.id, listOf(hand.first()))
        )

        driver.getHandSize(activePlayer) shouldBe startingHandSize + 1
        val sacrificeEvents = driver.events.filterIsInstance<PermanentsSacrificedEvent>()
        sacrificeEvents.flatMap { it.permanentIds }.toSet() shouldBe setOf(creature)
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

        val creature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val rtr = driver.putCardInHand(activePlayer, "Read the Runes")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        driver.castXSpell(activePlayer, rtr, xValue = 1)
        driver.bothPass()

        // Both feasible → pick Discard instead.
        val choice = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(choice.id, discardOption))
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val hand = driver.getHand(activePlayer)
        driver.submitCardSelection(activePlayer, listOf(hand.first()))

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

        driver.isPaused shouldBe false
        driver.getHandSize(activePlayer) shouldBe startingHandSize
    }
})
