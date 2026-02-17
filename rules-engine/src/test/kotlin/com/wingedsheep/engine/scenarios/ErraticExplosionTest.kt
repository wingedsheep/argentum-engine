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
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.AnyTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Erratic Explosion:
 * {2}{R} - Sorcery
 * "Choose any target. Reveal cards from the top of your library until you reveal a nonland card.
 * Erratic Explosion deals damage equal to that card's mana value to that permanent or player.
 * Put the revealed cards on the bottom of your library in any order."
 *
 * ## Covered Scenarios
 * - Nonland card on top: deals damage equal to its mana value, card goes to bottom
 * - Lands before nonland: reveals all, deals damage for nonland, all go to bottom in chosen order
 * - Target creature: deals damage to a creature
 * - Empty library: no damage, no crash
 * - All lands: reveals entire library, no damage, all go to bottom
 */
class ErraticExplosionTest : FunSpec({

    val ErraticExplosion = CardDefinition(
        name = "Erratic Explosion",
        manaCost = ManaCost.parse("{2}{R}"),
        typeLine = TypeLine.sorcery(),
        oracleText = "Choose any target. Reveal cards from the top of your library until you reveal a nonland card. Erratic Explosion deals damage equal to that card's mana value to that permanent or player. Put the revealed cards on the bottom of your library in any order.",
        script = CardScript.spell(
            effect = EffectPatterns.revealUntilNonlandDealDamage(EffectTarget.ContextTarget(0)),
            AnyTarget()
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ErraticExplosion)
        return driver
    }

    test("nonland on top deals damage equal to its mana value to player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a known nonland card on top of the library (CMC 3)
        val topCard = driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser")

        val explosion = driver.putCardInHand(activePlayer, "Erratic Explosion")
        driver.giveMana(activePlayer, Color.RED, 3)

        // Cast targeting opponent
        driver.castSpellWithTargets(activePlayer, explosion, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        // Only 1 card revealed (nonland was on top), so no reorder decision needed
        driver.isPaused shouldBe false

        // Should have revealed 1 card
        val revealEvents = driver.events.filterIsInstance<CardsRevealedEvent>()
        revealEvents.any { it.cardIds.contains(topCard) } shouldBe true

        // Opponent should have taken 3 damage (Centaur Courser has CMC 3)
        driver.getLifeTotal(opponent) shouldBe 17

        // The revealed card should be on the bottom of the library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val library = driver.state.getZone(libraryZone)
        library.last() shouldBe topCard
    }

    test("lands before nonland reveals all and deals damage for the nonland") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack cards on top: nonland first (will be deeper), then lands on top
        // putCardOnTopOfLibrary prepends, so we add in reverse order
        val nonlandCard = driver.putCardOnTopOfLibrary(activePlayer, "Force of Nature") // CMC 5
        val land2 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val land1 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        // Library top: Mountain, Forest, Force of Nature, ...rest

        val explosion = driver.putCardInHand(activePlayer, "Erratic Explosion")
        driver.giveMana(activePlayer, Color.RED, 3)

        driver.castSpellWithTargets(activePlayer, explosion, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        // 3 cards revealed, so should be paused for reorder decision
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()

        val reorderDecision = driver.pendingDecision as ReorderLibraryDecision
        reorderDecision.cards.size shouldBe 3
        reorderDecision.cards.toSet() shouldBe setOf(land1, land2, nonlandCard)

        // Damage should already be dealt before reorder
        driver.getLifeTotal(opponent) shouldBe 15

        // Submit an order for bottom of library
        val orderedCards = listOf(land1, nonlandCard, land2)
        driver.submitOrderedResponse(activePlayer, orderedCards)

        driver.isPaused shouldBe false

        // All 3 cards should be on the bottom of the library in the chosen order
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val library = driver.state.getZone(libraryZone)
        val librarySize = library.size
        library[librarySize - 3] shouldBe land1
        library[librarySize - 2] shouldBe nonlandCard
        library[librarySize - 1] shouldBe land2
    }

    test("deals damage to a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a CMC 2 creature on top of library
        driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears")

        // Put a creature on the battlefield to target
        val targetCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        val explosion = driver.putCardInHand(activePlayer, "Erratic Explosion")
        driver.giveMana(activePlayer, Color.RED, 3)

        driver.castSpellWithTargets(activePlayer, explosion, listOf(ChosenTarget.Permanent(targetCreature)))
        driver.bothPass()

        driver.isPaused shouldBe false

        // Creature should have 2 damage (Grizzly Bears CMC = 2)
        val damageEvents = driver.events.filterIsInstance<DamageDealtEvent>()
        damageEvents.any { it.targetId == targetCreature && it.amount == 2 } shouldBe true
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

        // Clear the library by replacing it with an empty list
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        driver.replaceState(
            driver.state.copy(zones = driver.state.zones + (libraryZone to emptyList()))
        )

        val explosion = driver.putCardInHand(activePlayer, "Erratic Explosion")
        driver.giveMana(activePlayer, Color.RED, 3)

        driver.castSpellWithTargets(activePlayer, explosion, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.isPaused shouldBe false

        // No damage dealt
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("all lands reveals entire library with no damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Replace library with just 3 lands (all lands, no nonland)
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        driver.replaceState(
            driver.state.copy(zones = driver.state.zones + (libraryZone to emptyList()))
        )
        val land1 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        val land2 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val land3 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        // Library: Mountain, Forest, Mountain

        val explosion = driver.putCardInHand(activePlayer, "Erratic Explosion")
        driver.giveMana(activePlayer, Color.RED, 3)

        driver.castSpellWithTargets(activePlayer, explosion, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        // 3 cards revealed, need to reorder for bottom
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()

        val decision = driver.pendingDecision as ReorderLibraryDecision
        decision.cards.size shouldBe 3

        // No damage dealt
        driver.getLifeTotal(opponent) shouldBe 20

        // Submit reorder
        driver.submitOrderedResponse(activePlayer, listOf(land3, land2, land1))

        driver.isPaused shouldBe false

        // Library should now have the 3 cards on bottom in the chosen order
        val library = driver.state.getZone(libraryZone)
        library shouldBe listOf(land3, land2, land1)
    }
})
