package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Tests for Rummaging Wizard:
 * {3}{U} - Creature — Human Wizard 2/2
 * "{2}{U}: Surveil 1. (Look at the top card of your library. You may put that card
 * into your graveyard.)"
 *
 * ## Covered Scenarios
 * - Surveil puts card into graveyard (player chooses graveyard)
 * - Surveil keeps card on top (player selects nothing for graveyard)
 * - Surveil with empty library does nothing
 */
class RummagingWizardTest : FunSpec({

    val wizardAbilityId = AbilityId(UUID.randomUUID().toString())

    val RummagingWizard = CardDefinition(
        name = "Rummaging Wizard",
        manaCost = ManaCost.parse("{3}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"), Subtype("Wizard"))),
        oracleText = "{2}{U}: Surveil 1. (Look at the top card of your library. You may put that card into your graveyard.)",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.permanent(
            ActivatedAbility(
                id = wizardAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{2}{U}")),
                effect = Effects.Surveil(1)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RummagingWizard))
        return driver
    }

    test("surveil 1 - put card into graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a known card on top of library
        val topCard = driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears")

        val wizard = driver.putCreatureOnBattlefield(activePlayer, "Rummaging Wizard")
        driver.removeSummoningSickness(wizard)
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Activate ability
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizard,
                abilityId = wizardAbilityId
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Should be paused for card selection (select cards to put in graveyard)
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        val decision = driver.pendingDecision as SelectCardsDecision
        decision.options.size shouldBe 1
        decision.options[0] shouldBe topCard

        // Select the card (put into graveyard)
        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(
                decisionId = decision.id,
                selectedCards = listOf(topCard)
            )
        )

        driver.isPaused shouldBe false

        // Card should be in graveyard
        val graveyardZone = ZoneKey(activePlayer, Zone.GRAVEYARD)
        val graveyard = driver.state.getZone(graveyardZone)
        graveyard.contains(topCard) shouldBe true

        // Card should NOT be in library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val library = driver.state.getZone(libraryZone)
        library.contains(topCard) shouldBe false
    }

    test("surveil 1 - keep card on top of library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val topCard = driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears")

        val wizard = driver.putCreatureOnBattlefield(activePlayer, "Rummaging Wizard")
        driver.removeSummoningSickness(wizard)
        driver.giveMana(activePlayer, Color.BLUE, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizard,
                abilityId = wizardAbilityId
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe true
        val decision = driver.pendingDecision as SelectCardsDecision

        // Select nothing (keep the card on top)
        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(
                decisionId = decision.id,
                selectedCards = emptyList()
            )
        )

        // Should get a reorder decision for the card going back on top
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        val reorderDecision = driver.pendingDecision as ReorderLibraryDecision
        reorderDecision.cards.size shouldBe 1
        driver.submitOrderedResponse(activePlayer, reorderDecision.cards)

        driver.isPaused shouldBe false

        // Card should still be on top of library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val library = driver.state.getZone(libraryZone)
        library.first() shouldBe topCard

        // Card should NOT be in graveyard
        val graveyardZone = ZoneKey(activePlayer, Zone.GRAVEYARD)
        val graveyard = driver.state.getZone(graveyardZone)
        graveyard.contains(topCard) shouldBe false
    }

    test("surveil with empty library does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Clear the library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        driver.replaceState(
            driver.state.copy(zones = driver.state.zones + (libraryZone to emptyList()))
        )

        val wizard = driver.putCreatureOnBattlefield(activePlayer, "Rummaging Wizard")
        driver.removeSummoningSickness(wizard)
        driver.giveMana(activePlayer, Color.BLUE, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizard,
                abilityId = wizardAbilityId
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Should NOT be paused - no cards to surveil
        driver.isPaused shouldBe false
    }
})
