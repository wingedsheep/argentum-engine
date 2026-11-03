package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A card-intrinsic `RedirectZoneChange(newDestination = LIBRARY, shuffleIntoLibrary = true)` —
 * "If <this> would be put into a graveyard from anywhere, … shuffle it into its owner's library
 * instead" (Progenitus, Nexus of Fate) — on an instant or sorcery that **resolves**.
 *
 * The countered and fizzled paths already shuffled; the resolution path put the card into the
 * library without shuffling, so it sat on top where the caster could draw it again next turn.
 * Both the full-resolve and the paused-resolve (a library search mid-resolution) paths are checked.
 */
class RedirectShuffleIntoLibraryOnResolveScenarioTest : FunSpec({

    val graveyardToLibrary = RedirectZoneChange(
        newDestination = Zone.LIBRARY,
        appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
        selfOnly = true,
        shuffleIntoLibrary = true,
        reveal = true,
    )

    val plain = card("Test Nexus Plain") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(2) }
        replacementEffect(graveyardToLibrary)
    }

    val searching = card("Test Nexus Searching") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
            )
        }
        replacementEffect(graveyardToLibrary)
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(plain, searching))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.cast(you: EntityId, cardName: String): EntityId {
        val cardId = putCardInHand(you, cardName)
        giveMana(you, Color.BLUE, 1)
        submit(CastSpell(you, cardId, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        return cardId
    }

    fun GameTestDriver.shuffles(player: EntityId) =
        events.count { it is LibraryShuffledEvent && it.playerId == player }

    test("a resolved spell redirected to its owner's library is shuffled in") {
        val driver = newDriver()
        val you = driver.player1
        val librarySizeBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size
        val shufflesBefore = driver.shuffles(you)
        val lifeBefore = driver.getLifeTotal(you)

        val cardId = driver.cast(you, "Test Nexus Plain")
        driver.settle()

        withClue("the spell resolved") { driver.getLifeTotal(you) shouldBe lifeBefore + 2 }
        withClue("the replacement sent it to the library, not the graveyard") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe true
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size shouldBe librarySizeBefore + 1
            driver.getGraveyardCardNames(you).contains("Test Nexus Plain") shouldBe false
        }
        withClue("its owner's library was shuffled once, and the shuffle announced") {
            driver.shuffles(you) shouldBe shufflesBefore + 1
        }
    }

    test("a spell that pauses mid-resolution is shuffled in too") {
        val driver = newDriver()
        val you = driver.player1
        val creatureId = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val shufflesBefore = driver.shuffles(you)

        val cardId = driver.cast(you, "Test Nexus Searching")
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        withClue("the search pauses for a card selection") { driver.isPaused shouldBe true }
        driver.submitCardSelection(you, listOf(creatureId))
        driver.settle()

        withClue("the search resolved") {
            driver.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(creatureId) shouldBe true
        }
        withClue("the spell is in the library, not the graveyard") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe true
            driver.getGraveyardCardNames(you).contains("Test Nexus Searching") shouldBe false
        }
        withClue("two shuffles: the search's own, then the redirect's") {
            driver.shuffles(you) shouldBe shufflesBefore + 2
        }
    }
})
