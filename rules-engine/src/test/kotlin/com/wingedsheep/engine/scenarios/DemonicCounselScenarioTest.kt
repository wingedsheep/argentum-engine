package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.DemonicCounsel
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Demonic Counsel (DSK #92) — {1}{B} Sorcery.
 *
 * "Search your library for a Demon card, reveal it, put it into your hand, then shuffle.
 *  Delirium — If there are four or more card types among cards in your graveyard, instead search
 *  your library for any card, put it into your hand, then shuffle."
 *
 * Exercises a [Conditions.Delirium]-gated [ConditionalEffect] choosing between a Demon-only tutor
 * (default) and an unrestricted tutor (Delirium active).
 */
class DemonicCounselScenarioTest : FunSpec({

    // A Demon creature usable as a search target.
    val TestDemon = CardDefinition(
        name = "Test Demon",
        manaCost = ManaCost.parse("{3}{B}{B}"),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE), subtypes = setOf(Subtype("Demon"))),
        creatureStats = CreatureStats(6, 6),
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DemonicCounsel)
        driver.registerCard(TestDemon)
        return driver
    }

    test("without Delirium, can only find a Demon card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Library has a Demon and a non-Demon on top.
        val demon = driver.putCardOnTopOfLibrary(activePlayer, "Test Demon")
        driver.putCardOnTopOfLibrary(activePlayer, "Island")

        val spell = driver.putCardInHand(activePlayer, "Demonic Counsel")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        driver.castSpell(activePlayer, spell).isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe true
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // Only the Demon is a legal choice (no Demons besides the one we seeded).
        decision.options.size shouldBe 1
        decision.options.contains(demon) shouldBe true

        driver.submitCardSelection(activePlayer, listOf(demon))

        driver.state.getZone(ZoneKey(activePlayer, Zone.HAND)).contains(demon) shouldBe true
    }

    test("with Delirium (four card types in graveyard), can find any card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Four distinct card types in the graveyard: creature, instant, sorcery, land.
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.putCardInGraveyard(activePlayer, "Lightning Bolt")
        driver.putCardInGraveyard(activePlayer, "Demonic Counsel")
        driver.putCardInGraveyard(activePlayer, "Forest")

        // A non-Demon card on top of the library; Delirium lets us grab it.
        val island = driver.putCardOnTopOfLibrary(activePlayer, "Island")

        val spell = driver.putCardInHand(activePlayer, "Demonic Counsel")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        driver.castSpell(activePlayer, spell).isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe true
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // Delirium active — every library card is a legal choice (not just Demons).
        decision.options.contains(island) shouldBe true

        driver.submitCardSelection(activePlayer, listOf(island))

        driver.state.getZone(ZoneKey(activePlayer, Zone.HAND)).contains(island) shouldBe true
    }

    test("may fail to find") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val demon = driver.putCardOnTopOfLibrary(activePlayer, "Test Demon")

        val spell = driver.putCardInHand(activePlayer, "Demonic Counsel")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        driver.castSpell(activePlayer, spell).isSuccess shouldBe true
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision shouldNotBe null

        // Decline to find anything.
        driver.submitCardSelection(activePlayer, emptyList())

        driver.state.getZone(ZoneKey(activePlayer, Zone.HAND)).contains(demon) shouldBe false
    }
})
