package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.Peppersmoke
import com.wingedsheep.mtg.sets.definitions.lrw.cards.Pestermite
import com.wingedsheep.mtg.sets.definitions.lrw.cards.SecludedGlen
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome

/**
 * Tests for Secluded Glen.
 *
 * Secluded Glen
 * Land
 * As this land enters, you may reveal a Faerie card from your hand. If you don't, this land
 * enters tapped.
 * {T}: Add {U} or {B}.
 *
 * Stands in for the whole Lorwyn cycle (Ancient Amphitheater, Auntie's Hovel, Gilt-Leaf Palace,
 * Wanderwine Hub) — the five share one script and differ only in creature type and mana. What
 * these tests pin down is the thing that is *not* shared with the SOI shadowlands the shape was
 * borrowed from: the reveal filter reads a **creature type on any card type**, so Lorwyn's Kindred
 * noncreature cards satisfy it. Peppersmoke is a Kindred Instant — Faerie and must count.
 */
class SecludedGlenScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SecludedGlen, Pestermite, Peppersmoke))
        return driver
    }

    fun setup(driver: GameTestDriver) {
        driver.initMirrorMatch(deck = Deck.of("Island" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("revealing a Faerie creature lets the land enter untapped") {
        val driver = createDriver()
        setup(driver)
        val player = driver.activePlayer!!

        val faerie = driver.putCardInHand(player, "Pestermite")
        val glen = driver.putCardInHand(player, "Secluded Glen")

        // The reveal is an as-enters replacement, so playing the land pauses on a decision.
        (driver.playLand(player, glen).outcome is Outcome.Paused) shouldBe true

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldBe listOf(faerie)

        driver.submitCardSelection(player, listOf(faerie)).outcome shouldBe Outcome.Done

        driver.isTapped(glen) shouldBe false
    }

    test("a Kindred noncreature Faerie card counts for the reveal") {
        val driver = createDriver()
        setup(driver)
        val player = driver.activePlayer!!

        // Peppersmoke is a Kindred Instant — Faerie: it is not a creature, but it has the type.
        val peppersmoke = driver.putCardInHand(player, "Peppersmoke")
        val glen = driver.putCardInHand(player, "Secluded Glen")

        (driver.playLand(player, glen).outcome is Outcome.Paused) shouldBe true

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldBe listOf(peppersmoke)

        driver.submitCardSelection(player, listOf(peppersmoke)).outcome shouldBe Outcome.Done

        driver.isTapped(glen) shouldBe false
    }

    test("declining the reveal taps the land even with a Faerie in hand") {
        val driver = createDriver()
        setup(driver)
        val player = driver.activePlayer!!

        driver.putCardInHand(player, "Pestermite")
        val glen = driver.putCardInHand(player, "Secluded Glen")

        (driver.playLand(player, glen).outcome is Outcome.Paused) shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        // An empty selection is the decline.
        driver.submitCardSelection(player, emptyList()).outcome shouldBe Outcome.Done

        driver.isTapped(glen) shouldBe true
    }

    test("no Faerie in hand still asks, offering nothing, and the land enters tapped") {
        val driver = createDriver()
        setup(driver)
        val player = driver.activePlayer!!

        val glen = driver.putCardInHand(player, "Secluded Glen")

        // Asked anyway, so the question doesn't reveal the hand (#3340); nothing can be picked.
        (driver.playLand(player, glen).outcome is Outcome.Paused) shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe emptyList()
        driver.submitCardSelection(player, emptyList()).outcome shouldBe Outcome.Done

        driver.isTapped(glen) shouldBe true
    }
})
