package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SpellCounteredEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ExileCounteredSpellInstead
import com.wingedsheep.sdk.scripting.effects.CounterCondition
import com.wingedsheep.sdk.scripting.effects.CounterDestination
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine tests for `CounterDestination.Library` — "counter target spell … put that card on the
 * top/bottom of its owner's library instead of into that player's graveyard" (Memory Lapse,
 * Spell Crumple, Hinder).
 *
 * The rules this pins down:
 * - it is a real counter (CR 701.6a): the spell leaves the stack for its owner's library and a
 *   `SpellCounteredEvent` fires;
 * - a single position is placed without a prompt; several are a choice for the *counter's*
 *   controller, not the countered spell's (Hinder's 2020-08-07 ruling);
 * - an uncounterable spell is untouched and no pointless choice is asked;
 * - a counter replacement (Guile's "instead exile that spell") wins over the library, again
 *   without a prompt;
 * - the "unless its controller pays" path reaches the same destination and the same choice.
 *
 * The victim is the active player (a creature spell is sorcery-speed); the counterer responds.
 */
class CounterToLibraryTest : FunSpec({

    fun counterCard(name: String, effect: com.wingedsheep.sdk.scripting.effects.Effect) = card(name) {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            this.effect = effect
        }
    }

    val lapse = counterCard("Test Lapse", Effects.CounterSpellToLibrary(LibraryChoicePosition.Top))
    val crumple = counterCard("Test Crumple", Effects.CounterSpellToLibrary(LibraryChoicePosition.Bottom))
    val hinder = counterCard(
        "Test Hinder",
        Effects.CounterSpellToLibrary(LibraryChoicePosition.Top, LibraryChoicePosition.Bottom)
    )
    val taxingHinder = counterCard(
        "Test Taxing Hinder",
        CounterEffect(
            condition = CounterCondition.UnlessPaysMana(ManaCost.parse("{1}")),
            counterDestination = CounterDestination.Library(
                listOf(LibraryChoicePosition.Top, LibraryChoicePosition.Bottom)
            )
        )
    )
    val exiler = card("Test Counter Exiler") {
        manaCost = "{3}{U}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 1
        replacementEffect(ExileCounteredSpellInstead())
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(lapse, crumple, hinder, taxingHinder, exiler))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.library(player: EntityId) = state.getZone(ZoneKey(player, Zone.LIBRARY))

    /** The active player casts Centaur Courser; returns its spell id. */
    fun GameTestDriver.castCourser(victim: EntityId): EntityId {
        giveMana(victim, Color.GREEN, 3)
        val courser = putCardInHand(victim, "Centaur Courser")
        castSpell(victim, courser).outcome shouldBe Outcome.Done
        return courser
    }

    /** [caster] casts [counterName] at [spell] and both players pass once so it resolves. */
    fun GameTestDriver.counterWith(caster: EntityId, counterName: String, spell: EntityId) {
        if (priorityPlayer != caster) passPriority(getOpponent(caster))
        giveMana(caster, Color.BLUE, 1)
        val counter = putCardInHand(caster, counterName)
        val cast = castSpellWithTargets(caster, counter, listOf(ChosenTarget.Spell(spell)))
        withClue(cast.error ?: "casting $counterName failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
    }

    fun GameTestDriver.resolveOut() {
        var guard = 0
        while (stackSize > 0 && pendingDecision == null && guard++ < 20) bothPass()
    }

    test("a single fixed position — top — counters without a prompt and fires SpellCounteredEvent") {
        val d = driver()
        val victim = d.player1
        val courser = d.castCourser(victim)

        d.counterWith(d.player2, "Test Lapse", courser)

        d.pendingDecision.shouldBeNull()
        withClue("the countered card is on top of its owner's library") {
            d.library(victim).first() shouldBe courser
        }
        d.getGraveyardCardNames(victim).contains("Centaur Courser") shouldBe false
        withClue("it was countered, not merely moved") {
            d.events.any { it is SpellCounteredEvent && it.spellEntityId == courser } shouldBe true
        }
    }

    test("a single fixed position — bottom — goes to the bottom") {
        val d = driver()
        val victim = d.player1
        val courser = d.castCourser(victim)

        d.counterWith(d.player2, "Test Crumple", courser)

        d.pendingDecision.shouldBeNull()
        d.library(victim).last() shouldBe courser
    }

    test("with several positions the counter's controller — not the spell's owner — chooses") {
        val d = driver()
        val victim = d.player1
        val counterer = d.player2
        val courser = d.castCourser(victim)

        d.counterWith(counterer, "Test Hinder", courser)

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("Hinder's controller chooses (2020-08-07 ruling)") { decision.playerId shouldBe counterer }
        val bottom = decision.options.indexOf(LibraryChoicePosition.Bottom.label)
        d.submitDecision(counterer, OptionChosenResponse(decision.id, bottom))

        d.library(victim).last() shouldBe courser
        d.findPermanent(victim, "Centaur Courser").shouldBeNull()
        d.events.any { it is SpellCounteredEvent && it.spellEntityId == courser } shouldBe true
    }

    test("choosing top puts it on top") {
        val d = driver()
        val victim = d.player1
        val counterer = d.player2
        val courser = d.castCourser(victim)

        d.counterWith(counterer, "Test Hinder", courser)
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(counterer, OptionChosenResponse(decision.id, decision.options.indexOf(LibraryChoicePosition.Top.label)))

        d.library(victim).first() shouldBe courser
    }

    test("an uncounterable spell is untouched and nobody is asked top or bottom") {
        val d = driver()
        val victim = d.player1
        val courser = d.castCourser(victim)
        d.addComponent(courser, CantBeCounteredComponent)

        d.counterWith(d.player2, "Test Hinder", courser)
        withClue("no pointless top-or-bottom prompt") { d.pendingDecision.shouldBeNull() }
        d.resolveOut()

        d.findPermanent(victim, "Centaur Courser").shouldNotBeNull()
        (courser in d.library(victim)) shouldBe false
    }

    test("a counter replacement that exiles the spell instead wins over the library, without a prompt") {
        val d = driver()
        val victim = d.player1
        val counterer = d.player2
        d.putCreatureOnBattlefield(counterer, "Test Counter Exiler")
        val courser = d.castCourser(victim)

        d.counterWith(counterer, "Test Hinder", courser)

        d.pendingDecision.shouldBeNull()
        (courser in d.state.getZone(ZoneKey(victim, Zone.EXILE))) shouldBe true
        (courser in d.library(victim)) shouldBe false
    }

    test("a spell's own on-counter exile rider (flashback) wins over the library, without a prompt") {
        val d = driver()
        val victim = d.player1
        val counterer = d.player2
        val courser = d.castCourser(victim)
        d.addComponent(courser, AfterResolveDestinationComponent(AfterResolveDestination.EXILE))

        d.counterWith(counterer, "Test Hinder", courser)

        d.pendingDecision.shouldBeNull()
        (courser in d.state.getZone(ZoneKey(victim, Zone.EXILE))) shouldBe true
        (courser in d.library(victim)) shouldBe false
    }

    test("counter-unless-pays: when the controller declines, the counterer still chooses top or bottom") {
        val d = driver()
        val victim = d.player1
        val counterer = d.player2
        val courser = d.castCourser(victim)
        d.giveMana(victim, Color.GREEN, 1) // the victim *could* pay {1}

        d.counterWith(counterer, "Test Taxing Hinder", courser)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(victim, false)

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe counterer
        d.submitDecision(counterer, OptionChosenResponse(decision.id, decision.options.indexOf(LibraryChoicePosition.Top.label)))

        d.library(victim).first() shouldBe courser
        d.events.any { it is SpellCounteredEvent && it.spellEntityId == courser } shouldBe true
    }
})
