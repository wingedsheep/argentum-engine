package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * `spell { selfPutOnBottomOfLibrary() }` — the card-intrinsic "Put <card name> on the bottom of its
 * owner's library." clause (Sanguine Sacrament). It is the `ZonePlacement.Bottom` value of
 * `CardScript.selfLibraryPlacementOnResolve`, whose `Shuffled` value is the Zenith cycle's clause
 * ([SelfShuffleIntoLibraryOnResolveScenarioTest] covers that one and the seam's precedence in depth).
 *
 * What this file pins is the difference: the card lands on the **bottom** of the library and the
 * library is **not** shuffled — on both of `StackResolver`'s resolve paths — plus the shared
 * negative cases (countered spells go to the graveyard, flashback's "anywhere else" still wins).
 */
class SelfBottomOfLibraryOnResolveScenarioTest : FunSpec({

    val plainBottom = card("Test Bottom Plain") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            effect = Effects.GainLife(2)
            selfPutOnBottomOfLibrary()
        }
    }

    /** A library search pauses resolution, so the spell leaves the stack via the paused path. */
    val searchingBottom = card("Test Bottom Searching") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature,
                count = 1,
                destination = SearchDestination.HAND,
            )
            selfPutOnBottomOfLibrary()
        }
    }

    val flashbackBottom = card("Test Bottom Flashback") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            effect = Effects.GainLife(2)
            selfPutOnBottomOfLibrary()
        }
        keywordAbility(KeywordAbility.flashback("{1}"))
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(plainBottom, searchingBottom, flashbackBottom))
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
        giveMana(you, Color.BLUE, 4)
        submit(CastSpell(you, cardId, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        return cardId
    }

    fun GameTestDriver.shuffles(player: EntityId) =
        events.count { it is LibraryShuffledEvent && it.playerId == player }

    test("a resolved spell goes to the bottom of its owner's library, unshuffled") {
        val driver = newDriver()
        val you = driver.player1
        val libraryBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY))
        val shufflesBefore = driver.shuffles(you)
        val lifeBefore = driver.getLifeTotal(you)

        val cardId = driver.cast(you, "Test Bottom Plain")
        driver.settle()

        withClue("the spell resolved — its effect happened") {
            driver.getLifeTotal(you) shouldBe lifeBefore + 2
        }
        withClue("CR 608.2n's graveyard destination was replaced by the bottom of the library") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)) shouldBe libraryBefore + cardId
            driver.getGraveyardCardNames(you).contains("Test Bottom Plain") shouldBe false
        }
        withClue("the bottom clause does not shuffle — the rest of the library keeps its order") {
            driver.shuffles(you) shouldBe shufflesBefore
        }
    }

    test("a spell that pauses mid-resolution still goes to the bottom — the paused-resolve path") {
        val driver = newDriver()
        val you = driver.player1
        val creatureId = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val shufflesBefore = driver.shuffles(you)

        val cardId = driver.cast(you, "Test Bottom Searching")
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.submitCardSelection(you, listOf(creatureId))
        driver.settle()

        withClue("the search resolved — the creature is in hand") {
            driver.state.getZone(ZoneKey(you, Zone.HAND)).contains(creatureId) shouldBe true
        }
        withClue("the spell is the library's bottom card") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).last() shouldBe cardId
            driver.getGraveyardCardNames(you).contains("Test Bottom Searching") shouldBe false
        }
        withClue("only the search's own shuffle happened, not a second one for the spell") {
            driver.shuffles(you) shouldBe shufflesBefore + 1
        }
    }

    test("a countered spell goes to the graveyard — CR 701.6a, the effect never happened") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)

        val cardId = driver.cast(you, "Test Bottom Plain")
        driver.passPriority(you)
        driver.submit(
            CastSpell(
                opponent, counter,
                targets = listOf(ChosenTarget.Spell(cardId)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.settle()

        driver.getLifeTotal(you) shouldBe 20
        driver.getGraveyardCardNames(you).contains("Test Bottom Plain") shouldBe true
        driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe false
    }

    test("flashback outranks the printed clause — CR 702.34a's \"anywhere else\"") {
        val driver = newDriver()
        val you = driver.player1
        val cardId = driver.putCardInGraveyard(you, "Test Bottom Flashback")
        driver.giveMana(you, Color.BLUE, 4)
        driver.submit(
            CastSpell(you, cardId, paymentStrategy = PaymentStrategy.FromPool, useAlternativeCost = true)
        ).error shouldBe null
        driver.settle()

        driver.getExile(you).contains(cardId) shouldBe true
        driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(cardId) shouldBe false
    }

    test("a card can't print both the exile clause and the bottom clause") {
        val ex = shouldThrow<IllegalArgumentException> {
            card("Test Bottom And Exile") {
                manaCost = "{1}"
                typeLine = "Instant"
                spell {
                    effect = Effects.GainLife(2)
                    selfExile()
                    selfPutOnBottomOfLibrary()
                }
            }
        }
        ex.message shouldContain "Test Bottom And Exile"
    }

    test("a CardScript built directly only takes the shuffled and bottom placements") {
        shouldThrow<IllegalArgumentException> {
            CardScript(selfLibraryPlacementOnResolve = ZonePlacement.Top)
        }.message shouldContain "selfLibraryPlacementOnResolve"
        shouldThrow<IllegalArgumentException> {
            CardScript(selfExileOnResolve = true, selfLibraryPlacementOnResolve = ZonePlacement.Bottom)
        }.message shouldContain "CR 608.2n destination"
    }
})
