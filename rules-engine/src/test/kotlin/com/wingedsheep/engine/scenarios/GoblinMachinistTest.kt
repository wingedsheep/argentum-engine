package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Tests for Goblin Machinist:
 * {4}{R} - Creature — Goblin 0/5
 * "{2}{R}: Reveal cards from the top of your library until you reveal a nonland card.
 * Goblin Machinist gets +X/+0 until end of turn, where X is that card's mana value.
 * Put the revealed cards on the bottom of your library in any order."
 *
 * ## Covered Scenarios
 * - Nonland card on top: gets +X/+0 where X is the card's mana value
 * - Lands before nonland: reveals all, buffs for nonland's mana value, all go to bottom in chosen order
 * - Empty library: no buff, no crash
 * - All lands: reveals entire library, no buff, all go to bottom
 * - Multiple activations stack: two buffs both apply
 */
class GoblinMachinistTest : FunSpec({

    val machinistAbilityId = AbilityId(UUID.randomUUID().toString())

    val GoblinMachinist = CardDefinition(
        name = "Goblin Machinist",
        manaCost = ManaCost.parse("{4}{R}"),
        typeLine = TypeLine.creature(setOf(Subtype("Goblin"))),
        oracleText = "{2}{R}: Reveal cards from the top of your library until you reveal a nonland card. Goblin Machinist gets +X/+0 until end of turn, where X is that card's mana value. Put the revealed cards on the bottom of your library in any order.",
        creatureStats = CreatureStats(0, 5),
        script = CardScript.permanent(
            ActivatedAbility(
                id = machinistAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{2}{R}")),
                effect = EffectPatterns.revealUntilNonlandModifyStats()
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoblinMachinist))
        return driver
    }

    test("nonland on top gives +X/+0 where X is its mana value") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a known nonland card on top of the library (CMC 3)
        val topCard = driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser")

        val machinist = driver.putCreatureOnBattlefield(activePlayer, "Goblin Machinist")
        driver.removeSummoningSickness(machinist)
        driver.giveMana(activePlayer, Color.RED, 3)

        // Activate ability
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = machinist,
                abilityId = machinistAbilityId
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Only 1 card revealed (nonland was on top), so no reorder decision needed
        driver.isPaused shouldBe false

        // Should have revealed 1 card
        val revealEvents = driver.events.filterIsInstance<CardsRevealedEvent>()
        revealEvents.any { it.cardIds.contains(topCard) } shouldBe true

        // Goblin Machinist should be 3/5 (0+3/5+0) from floating effect
        val projected = projector.project(driver.state)
        projector.getProjectedPower(driver.state, machinist) shouldBe 3
        projector.getProjectedToughness(driver.state, machinist) shouldBe 5

        // The revealed card should be on the bottom of the library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val library = driver.state.getZone(libraryZone)
        library.last() shouldBe topCard
    }

    test("lands before nonland reveals all and buffs for the nonland mana value") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack cards on top: nonland first (deeper), then lands on top
        val nonlandCard = driver.putCardOnTopOfLibrary(activePlayer, "Force of Nature") // CMC 5
        val land2 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val land1 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        // Library top: Mountain, Forest, Force of Nature, ...rest

        val machinist = driver.putCreatureOnBattlefield(activePlayer, "Goblin Machinist")
        driver.removeSummoningSickness(machinist)
        driver.giveMana(activePlayer, Color.RED, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = machinist,
                abilityId = machinistAbilityId
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // 3 cards revealed, so should be paused for reorder decision
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()

        val reorderDecision = driver.pendingDecision as ReorderLibraryDecision
        reorderDecision.cards.size shouldBe 3
        reorderDecision.cards.toSet() shouldBe setOf(land1, land2, nonlandCard)

        // Buff should already be applied before reorder
        projector.getProjectedPower(driver.state, machinist) shouldBe 5
        projector.getProjectedToughness(driver.state, machinist) shouldBe 5

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

    test("empty library does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Clear the library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        driver.replaceState(
            driver.state.copy(zones = driver.state.zones + (libraryZone to emptyList()))
        )

        val machinist = driver.putCreatureOnBattlefield(activePlayer, "Goblin Machinist")
        driver.removeSummoningSickness(machinist)
        driver.giveMana(activePlayer, Color.RED, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = machinist,
                abilityId = machinistAbilityId
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe false

        // No buff - still 0/5
        projector.getProjectedPower(driver.state, machinist) shouldBe 0
        projector.getProjectedToughness(driver.state, machinist) shouldBe 5
    }

    test("all lands reveals entire library with no buff") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Replace library with just 3 lands
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        driver.replaceState(
            driver.state.copy(zones = driver.state.zones + (libraryZone to emptyList()))
        )
        val land1 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        val land2 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val land3 = driver.putCardOnTopOfLibrary(activePlayer, "Mountain")
        // Library: Mountain, Forest, Mountain

        val machinist = driver.putCreatureOnBattlefield(activePlayer, "Goblin Machinist")
        driver.removeSummoningSickness(machinist)
        driver.giveMana(activePlayer, Color.RED, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = machinist,
                abilityId = machinistAbilityId
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // 3 cards revealed, need to reorder for bottom
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()

        val decision = driver.pendingDecision as ReorderLibraryDecision
        decision.cards.size shouldBe 3

        // No buff applied - still 0/5
        projector.getProjectedPower(driver.state, machinist) shouldBe 0
        projector.getProjectedToughness(driver.state, machinist) shouldBe 5

        // Submit reorder
        driver.submitOrderedResponse(activePlayer, listOf(land3, land2, land1))

        driver.isPaused shouldBe false

        // Library should now have the 3 cards on bottom in the chosen order
        val library = driver.state.getZone(libraryZone)
        library shouldBe listOf(land3, land2, land1)
    }

    test("multiple activations stack buffs") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two nonland cards on top (CMC 3 and CMC 2)
        driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears") // CMC 2 - will be revealed second
        driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser") // CMC 3 - will be revealed first
        // Library top: Centaur Courser, Grizzly Bears, ...rest

        val machinist = driver.putCreatureOnBattlefield(activePlayer, "Goblin Machinist")
        driver.removeSummoningSickness(machinist)

        // First activation
        driver.giveMana(activePlayer, Color.RED, 3)
        val result1 = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = machinist,
                abilityId = machinistAbilityId
            )
        )
        result1.isSuccess shouldBe true
        driver.bothPass()

        // Should have +3/+0 from Centaur Courser (CMC 3)
        projector.getProjectedPower(driver.state, machinist) shouldBe 3
        projector.getProjectedToughness(driver.state, machinist) shouldBe 5

        // Second activation
        driver.giveMana(activePlayer, Color.RED, 3)
        val result2 = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = machinist,
                abilityId = machinistAbilityId
            )
        )
        result2.isSuccess shouldBe true
        driver.bothPass()

        // Should have +3/+0 + +2/+0 = +5/+0, so 5/5
        projector.getProjectedPower(driver.state, machinist) shouldBe 5
        projector.getProjectedToughness(driver.state, machinist) shouldBe 5
    }
})
