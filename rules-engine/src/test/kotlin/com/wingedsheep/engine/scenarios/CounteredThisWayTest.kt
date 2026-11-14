package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterAllOnStackEffect
import com.wingedsheep.sdk.scripting.effects.CounterCondition
import com.wingedsheep.sdk.scripting.effects.CounterDestination
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Countered this way" — [CounterEffect.storeCounteredAs] records the spell only if the counter
 * actually countered it, on every path a single counter takes (unconditional, unless-pays with the
 * payer unable / declining / paying, a choice-bearing library destination), and the stack-wide
 * [CounterAllOnStackEffect.storeCountAs] counts only what it actually countered. Each test card
 * draws a card when its collection is non-empty, so the draw is the observable rider.
 */
class CounteredThisWayTest : FunSpec({

    val unstoppableBolt = TestCards.LightningBolt.copy(
        name = "Unstoppable Bolt",
        script = TestCards.LightningBolt.script.copy(cantBeCountered = true)
    )

    fun recordingCounter(name: String, counter: (String) -> Effect) = card(name) {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.Pipeline {
                val countered = runStoringCollection(counter)
                run(Effects.If(Conditions.CollectionContainsMatch(countered), Effects.DrawCards(1)))
            }
        }
    }

    val plain = recordingCounter("Recording Counter") { CounterEffect(storeCounteredAs = it) }
    val tax = recordingCounter("Recording Tax") {
        CounterEffect(condition = CounterCondition.UnlessPaysMana(ManaCost.parse("{2}")), storeCounteredAs = it)
    }
    val hinder = recordingCounter("Recording Hinder") {
        CounterEffect(
            counterDestination = CounterDestination.Library(listOf(LibraryChoicePosition.Top, LibraryChoicePosition.Bottom)),
            storeCounteredAs = it
        )
    }
    val sweep = card("Recording Sweep") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val countered = runStoringCollection { CounterAllOnStackEffect(storeCountAs = it) }
                run(Effects.DrawCards(countered.count))
            }
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(unstoppableBolt, plain, tax, hinder, sweep))
        initMirrorMatch(deck = Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** The active player bolts the opponent and passes; the opponent answers with [counter]. */
    fun GameTestDriver.boltThenAnswer(boltName: String, counter: String): EntityId {
        val active = state.activePlayerId!!
        val opponent = getOpponent(active)
        giveMana(active, Color.RED, 1)
        castSpell(active, putCardInHand(active, boltName), listOf(opponent)).outcome shouldBe Outcome.Done
        val bolt = state.stack.last()
        passPriority(active)
        giveMana(opponent, Color.BLUE, 1)
        val answer = putCardInHand(opponent, counter)
        if (counter == "Recording Sweep") castSpell(opponent, answer).outcome shouldBe Outcome.Done
        else castSpellWithTargets(opponent, answer, listOf(ChosenTarget.Spell(bolt))).outcome shouldBe Outcome.Done
        return bolt
    }

    test("an unconditional counter records the spell it countered") {
        val d = driver()
        val opponent = d.getOpponent(d.state.activePlayerId!!)
        val bolt = d.boltThenAnswer("Lightning Bolt", "Recording Counter")
        val hand = d.getHandSize(opponent)
        d.bothPass()
        d.state.stack.contains(bolt) shouldBe false
        d.getHandSize(opponent) shouldBe hand + 1
    }

    test("a spell that can't be countered is not recorded") {
        val d = driver()
        val opponent = d.getOpponent(d.state.activePlayerId!!)
        val bolt = d.boltThenAnswer("Unstoppable Bolt", "Recording Counter")
        val hand = d.getHandSize(opponent)
        d.bothPass()
        d.state.stack.contains(bolt) shouldBe true
        d.getHandSize(opponent) shouldBe hand
    }

    test("unless-pays: a controller who can't pay is countered and recorded") {
        val d = driver()
        val opponent = d.getOpponent(d.state.activePlayerId!!)
        d.boltThenAnswer("Lightning Bolt", "Recording Tax")
        val hand = d.getHandSize(opponent)
        d.bothPass()
        d.state.pendingDecision shouldBe null
        d.getHandSize(opponent) shouldBe hand + 1
    }

    test("unless-pays: declining to pay is countered and recorded") {
        val d = driver()
        val active = d.state.activePlayerId!!
        val opponent = d.getOpponent(active)
        d.boltThenAnswer("Lightning Bolt", "Recording Tax")
        d.giveColorlessMana(active, 2)
        val hand = d.getHandSize(opponent)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(active, false).error shouldBe null
        d.getHandSize(opponent) shouldBe hand + 1
    }

    test("unless-pays: paying leaves nothing countered and nothing recorded") {
        val d = driver()
        val active = d.state.activePlayerId!!
        val opponent = d.getOpponent(active)
        val bolt = d.boltThenAnswer("Lightning Bolt", "Recording Tax")
        d.giveColorlessMana(active, 2)
        val hand = d.getHandSize(opponent)
        d.bothPass()
        d.submitYesNo(active, true).error shouldBe null
        withClue("floating mana pays the tax outright") { d.state.stack.contains(bolt) shouldBe true }
        d.getHandSize(opponent) shouldBe hand
    }

    test("a library destination that asks top or bottom records the spell once it lands") {
        val d = driver()
        val opponent = d.getOpponent(d.state.activePlayerId!!)
        d.boltThenAnswer("Lightning Bolt", "Recording Hinder")
        val hand = d.getHandSize(opponent)
        d.bothPass()
        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(opponent, OptionChosenResponse(choice.id, 1)).error shouldBe null
        d.getHandSize(opponent) shouldBe hand + 1
    }

    test("a stack-wide counter counts only the spells it actually countered") {
        val d = driver()
        val active = d.state.activePlayerId!!
        val opponent = d.getOpponent(active)
        // Two opposing bolts, one of which can't be countered.
        d.giveMana(active, Color.RED, 2)
        d.castSpell(active, d.putCardInHand(active, "Lightning Bolt"), listOf(opponent)).outcome shouldBe Outcome.Done
        d.castSpell(active, d.putCardInHand(active, "Unstoppable Bolt"), listOf(opponent)).outcome shouldBe Outcome.Done
        d.passPriority(active)
        d.giveMana(opponent, Color.BLUE, 1)
        d.castSpell(opponent, d.putCardInHand(opponent, "Recording Sweep")).outcome shouldBe Outcome.Done
        val hand = d.getHandSize(opponent)
        d.bothPass()
        d.getHandSize(opponent) shouldBe hand + 1
    }

    test("a stack-wide EachOnStack counter can't record") {
        shouldThrow<IllegalArgumentException> {
            CounterEffect(
                targetSource = CounterTargetSource.EachOnStack(),
                condition = CounterCondition.UnlessPaysMana(ManaCost.parse("{1}")),
                storeCounteredAs = "x"
            )
        }
    }
})
