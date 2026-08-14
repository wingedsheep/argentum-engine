package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Madame Web, Clairvoyant (SPM #36) — {4}{U}{U}
 * Legendary Creature — Mutant Advisor, 4/4.
 *
 *   You may look at the top card of your library any time.
 *   You may cast Spider spells and noncreature spells from the top of your library.
 *   Whenever you attack, you may mill a card.
 *
 * Exercises the filtered cast-from-top permission (a noncreature spell and a Spider spell are
 * castable from the top of the library, a non-Spider creature is not) and the optional attack-
 * triggered mill.
 */
class MadameWebClairvoyantScenarioTest : FunSpec({

    // Local Spider creature for the "can cast a Spider spell from the top" case.
    val TestSpider = card("Test Spider") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Spider"
        power = 1
        toughness = 3
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(com.wingedsheep.mtg.sets.definitions.spm.cards.MadameWebClairvoyant)
        driver.registerCard(TestSpider)
        return driver
    }

    test("can cast a noncreature spell from the top of the library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(activePlayer, "Madame Web, Clairvoyant")

        // Test Enchantment is {1}{W} with no abilities — a noncreature spell.
        val enchantmentOnTop = driver.putCardOnTopOfLibrary(activePlayer, "Test Enchantment")
        driver.giveMana(activePlayer, Color.WHITE, 2)

        val castResult = driver.castSpell(activePlayer, enchantmentOnTop)
        castResult.isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(activePlayer, "Test Enchantment") shouldNotBe null
    }

    test("can cast a Spider spell from the top of the library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(activePlayer, "Madame Web, Clairvoyant")

        val spiderOnTop = driver.putCardOnTopOfLibrary(activePlayer, "Test Spider")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        val castResult = driver.castSpell(activePlayer, spiderOnTop)
        castResult.isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(activePlayer, "Test Spider") shouldNotBe null
    }

    test("cannot cast a non-Spider creature spell from the top of the library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(activePlayer, "Madame Web, Clairvoyant")

        // Centaur Courser is a {2}{G} Centaur Warrior — a creature that is not a Spider.
        val centaurOnTop = driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser")
        driver.giveMana(activePlayer, Color.GREEN, 3)

        val castResult = driver.castSpell(activePlayer, centaurOnTop)
        castResult.isSuccess shouldBe false
    }

    test("attacking lets you mill a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val web = driver.putPermanentOnBattlefield(activePlayer, "Madame Web, Clairvoyant")
        driver.removeSummoningSickness(web)

        val milledCard = driver.putCardOnTopOfLibrary(activePlayer, "Island")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(web), opponent).isSuccess shouldBe true

        // The "Whenever you attack, you may mill a card" trigger resolves and offers a yes/no.
        driver.bothPass()
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        driver.submitYesNo(activePlayer, true)

        val graveyard = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD))
        graveyard.contains(milledCard) shouldBe true
    }

    test("declining the attack mill leaves the library intact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val web = driver.putPermanentOnBattlefield(activePlayer, "Madame Web, Clairvoyant")
        driver.removeSummoningSickness(web)

        val topCard = driver.putCardOnTopOfLibrary(activePlayer, "Island")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(web), opponent).isSuccess shouldBe true

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(activePlayer, false)

        val graveyard = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD))
        graveyard.contains(topCard) shouldBe false
    }
})
