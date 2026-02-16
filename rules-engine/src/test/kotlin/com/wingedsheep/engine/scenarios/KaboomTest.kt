package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.RevealUntilNonlandDealDamageEachTargetEffect
import com.wingedsheep.sdk.targeting.TargetPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Kaboom!:
 * {4}{R} - Sorcery
 * "Choose any number of target players or planeswalkers. For each of them,
 * reveal cards from the top of your library until you reveal a nonland card,
 * Kaboom! deals damage equal to that card's mana value to that player or
 * planeswalker, then you put the revealed cards on the bottom of your library
 * in any order."
 *
 * ## Covered Scenarios
 * - Single target, nonland on top: deals damage, card goes to bottom
 * - Single target, lands before nonland: reveals all, reorder for bottom
 * - Two targets: separate reveals for each, damage dealt independently
 * - Two targets with reordering: pauses for each reorder
 * - Zero targets: no effect
 * - Empty library: no damage
 */
class KaboomTest : FunSpec({

    val KaboomCard = CardDefinition(
        name = "Kaboom!",
        manaCost = ManaCost.parse("{4}{R}"),
        typeLine = TypeLine.sorcery(),
        oracleText = "Choose any number of target players or planeswalkers. For each of them, reveal cards from the top of your library until you reveal a nonland card, Kaboom! deals damage equal to that card's mana value to that player or planeswalker, then you put the revealed cards on the bottom of your library in any order.",
        script = CardScript.spell(
            effect = RevealUntilNonlandDealDamageEachTargetEffect,
            TargetPlayer(count = 99, optional = true)
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(KaboomCard)
        return driver
    }

    test("single target, nonland on top deals damage equal to mana value") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a known nonland card on top (CMC 3)
        val topCard = driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser")

        val kaboom = driver.putCardInHand(activePlayer, "Kaboom!")
        driver.giveMana(activePlayer, Color.RED, 5)

        driver.castSpellWithTargets(activePlayer, kaboom, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        // Only 1 card revealed (nonland on top), no reorder needed
        driver.isPaused shouldBe false

        // Should have revealed the card
        val revealEvents = driver.events.filterIsInstance<CardsRevealedEvent>()
        revealEvents.any { it.cardIds.contains(topCard) } shouldBe true

        // Opponent takes 3 damage (Centaur Courser CMC = 3)
        driver.getLifeTotal(opponent) shouldBe 17

        // Card goes to bottom of library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val library = driver.state.getZone(libraryZone)
        library.last() shouldBe topCard
    }

    test("single target, lands before nonland reveals all and needs reorder") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack: nonland deep, lands on top
        val nonlandCard = driver.putCardOnTopOfLibrary(activePlayer, "Force of Nature") // CMC 5
        val land2 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val land1 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        // Library top: Mountain, Forest, Force of Nature, ...

        val kaboom = driver.putCardInHand(activePlayer, "Kaboom!")
        driver.giveMana(activePlayer, Color.RED, 5)

        driver.castSpellWithTargets(activePlayer, kaboom, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        // 3 cards revealed, needs reorder
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()

        val reorderDecision = driver.pendingDecision as ReorderLibraryDecision
        reorderDecision.cards.size shouldBe 3

        // Damage already dealt
        driver.getLifeTotal(opponent) shouldBe 15 // Force of Nature CMC = 5

        // Submit order for bottom
        driver.submitOrderedResponse(activePlayer, listOf(land1, nonlandCard, land2))

        driver.isPaused shouldBe false

        // Cards on bottom in chosen order
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val library = driver.state.getZone(libraryZone)
        val sz = library.size
        library[sz - 3] shouldBe land1
        library[sz - 2] shouldBe nonlandCard
        library[sz - 1] shouldBe land2
    }

    test("two targets, both nonland on top, deals separate damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two nonland cards on top: first for target 1, second for target 2
        driver.putCardOnTopOfLibrary(activePlayer, "Force of Nature") // CMC 5, for second target
        driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser") // CMC 3, for first target
        // Library top: Centaur Courser, Force of Nature, ...

        val kaboom = driver.putCardInHand(activePlayer, "Kaboom!")
        driver.giveMana(activePlayer, Color.RED, 5)

        // Target both players
        driver.castSpellWithTargets(
            activePlayer, kaboom,
            listOf(ChosenTarget.Player(opponent), ChosenTarget.Player(activePlayer))
        )
        driver.bothPass()

        // Each reveal has only 1 card (nonland on top), no reorder needed
        driver.isPaused shouldBe false

        // Opponent takes 3 (Centaur Courser), active player takes 5 (Force of Nature)
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(activePlayer) shouldBe 15
    }

    test("two targets with reorder pauses for each") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Set up library with lands before nonlands for both targets
        // After first target reveals: Mountain, Forest, Centaur Courser
        // After second target reveals from remaining: Mountain, Force of Nature
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        driver.replaceState(
            driver.state.copy(zones = driver.state.zones + (libraryZone to emptyList()))
        )

        // Build library from bottom to top
        val nonland2 = driver.putCardOnTopOfLibrary(activePlayer, "Force of Nature") // CMC 5
        val land3 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        val nonland1 = driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser") // CMC 3
        val land2 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val land1 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        // Library: Mountain, Forest, Centaur Courser, Mountain, Force of Nature

        val kaboom = driver.putCardInHand(activePlayer, "Kaboom!")
        driver.giveMana(activePlayer, Color.RED, 5)

        driver.castSpellWithTargets(
            activePlayer, kaboom,
            listOf(ChosenTarget.Player(opponent), ChosenTarget.Player(activePlayer))
        )
        driver.bothPass()

        // First target: reveals Mountain, Forest, Centaur Courser (3 cards → reorder)
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()

        val reorder1 = driver.pendingDecision as ReorderLibraryDecision
        reorder1.cards.size shouldBe 3

        // Opponent already took 3 damage
        driver.getLifeTotal(opponent) shouldBe 17

        // Submit first reorder
        driver.submitOrderedResponse(activePlayer, listOf(land1, land2, nonland1))

        // Second target: reveals Mountain, Force of Nature (2 cards → reorder)
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()

        val reorder2 = driver.pendingDecision as ReorderLibraryDecision
        reorder2.cards.size shouldBe 2

        // Active player took 5 damage
        driver.getLifeTotal(activePlayer) shouldBe 15

        // Submit second reorder
        driver.submitOrderedResponse(activePlayer, listOf(land3, nonland2))

        driver.isPaused shouldBe false

        // All cards on bottom: first reorder then second reorder
        val library = driver.state.getZone(libraryZone)
        val sz = library.size
        library[sz - 5] shouldBe land1
        library[sz - 4] shouldBe land2
        library[sz - 3] shouldBe nonland1
        library[sz - 2] shouldBe land3
        library[sz - 1] shouldBe nonland2
    }

    test("zero targets does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kaboom = driver.putCardInHand(activePlayer, "Kaboom!")
        driver.giveMana(activePlayer, Color.RED, 5)

        // Cast with no targets
        driver.castSpellWithTargets(activePlayer, kaboom, emptyList())
        driver.bothPass()

        driver.isPaused shouldBe false
        driver.getLifeTotal(opponent) shouldBe 20
        driver.getLifeTotal(activePlayer) shouldBe 20
    }

    test("empty library deals no damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty the library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        driver.replaceState(
            driver.state.copy(zones = driver.state.zones + (libraryZone to emptyList()))
        )

        val kaboom = driver.putCardInHand(activePlayer, "Kaboom!")
        driver.giveMana(activePlayer, Color.RED, 5)

        driver.castSpellWithTargets(activePlayer, kaboom, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.isPaused shouldBe false
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
