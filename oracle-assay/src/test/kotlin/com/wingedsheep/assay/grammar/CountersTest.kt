package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The counter sentences — "Put a +1/+1 counter on target creature.", "…on ~.", "…on it.", and
 * "~ enters with two +1/+1 counters on it."
 *
 * The interesting assertions here are the ones about what *refuses* to read or print: the three
 * sentences differ only in who the counter lands on, and all three round-trip byte-perfectly under
 * the wrong reading, which is the class only this kind of test and the differential can see.
 */
class CountersTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "the singular sentence carries its quantity in the article" {
        fragment("Put a +1/+1 counter on target creature.").script.spellEffect shouldBe
            AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, Targets.bound())
        roundTrips("Put a +1/+1 counter on target creature.")
    }

    "the plural sentence spells its count as a word" {
        fragment("Put two -1/-1 counters on target creature you control.").script.spellEffect shouldBe
            AddCountersEffect(Counters.MINUS_ONE_MINUS_ONE, 2, Targets.bound())
        roundTrips("Put two -1/-1 counters on target creature you control.")
        roundTrips("Put three +1/+1 counters on target Sliver creature.")
    }

    // The commonest effect shape in the whole hand-written corpus: 363 of the 951 AddCounters a
    // golden carries are this one.
    "a counter on the source names the source and not a target" {
        fragment("Put a +1/+1 counter on ~.").script shouldBe
            CardScript(spellEffect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
        roundTrips("Put a +1/+1 counter on ~.")
        roundTrips("Put two +1/+1 counters on ~.")
    }

    // The two anaphors. "It" is the source in a first clause and the target once a clause has chosen
    // one, and both readings round-trip byte-perfectly — which is exactly why they are two
    // vocabularies reachable from disjoint positions rather than one rule.
    "\"on it\" is the source in a first clause and the chosen target in a later one" {
        fragment("Put a +1/+1 counter on it.").script.spellEffect shouldBe
            AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

        // The counter lands on the creature the first clause untapped, never on the source.
        val sequence = fragment("Untap target creature. Put a +1/+1 counter on it.")
        val steps = (sequence.script.spellEffect as CompositeEffect).effects
        steps.last() shouldBe AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, Targets.bound())
        (steps.last() == AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)) shouldBe false
        roundTrips("Untap target creature. Put a +1/+1 counter on it.")
    }

    "the entry replacement reads both quantities" {
        fragment("~ enters with a +1/+1 counter on it.").script.replacementEffects shouldBe
            listOf(EntersWithCounters(CounterTypeFilter.PlusOnePlusOne, 1, selfOnly = true))
        roundTrips("~ enters with a +1/+1 counter on it.")
        roundTrips("~ enters with three -1/-1 counters on it.")
    }

    // Gnarlid Colony and the other kicker creatures. The condition is the clause that makes the card
    // worth playing, and a rule that printed the value without it would be byte-perfect and wrong.
    "an entry replacement carrying a condition refuses to print rather than dropping it" {
        val kicked = CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersWithCounters(CounterTypeFilter.PlusOnePlusOne, 2, selfOnly = true, condition = WasKicked)
                )
            )
        )
        Grammar.abilityLine.printLine(kicked) shouldBe null
    }

    // Hardened Scales' shape: the same type with selfOnly false says something about *other*
    // permanents, and this sentence names the source.
    "an entry replacement that is not about the source refuses to print" {
        val others = CardFragment(
            script = CardScript(
                replacementEffects = listOf(EntersWithCounters(CounterTypeFilter.PlusOnePlusOne, 1))
            )
        )
        Grammar.abilityLine.printLine(others) shouldBe null
    }

    // The leaf is gated on the SDK's own list for creatureSubtype's reason: an ungated one would
    // read any lowercase word as a counter kind and round-trip a counter Magic does not have.
    "a word the SDK does not name as a counter is not a counter kind" {
        CounterType.fromName("growing") shouldBe null
        declines("Put a growing counter on target creature.")
    }

    "the two-word kinds read despite the leaf taking a single regex match" {
        fragment("Put a first strike counter on target creature.").script.spellEffect shouldBe
            AddCountersEffect(Counters.FIRST_STRIKE, 1, Targets.bound())
        roundTrips("Put a first strike counter on target creature.")
    }

    // No counter kind in all 34,882 Oracle texts is ever spelled both ways, so the article is a
    // total function of the kind and the leaf can be its inverse. "an hourglass" is the silent-h
    // case the letter rule alone would get wrong.
    "the indefinite article follows the kind, silent h included" {
        roundTrips("Put an aim counter on target creature.")
        roundTrips("Put an hourglass counter on target creature.")
        roundTrips("Put a stun counter on target creature.")
        declines("Put an stun counter on target creature.")
        declines("Put a aim counter on target creature.")
    }

    // CounterTypeFilter.Named can hold the same string the dedicated cases do, so the two are one
    // value written twice. The grammar emits one and refuses to print the other, which is what keeps
    // a card written the minority way reporting as a divergence instead of quietly agreeing.
    "the Named spelling of a kind that has a dedicated case never prints" {
        Primitives.counterFilter(Counters.PLUS_ONE_PLUS_ONE) shouldBe CounterTypeFilter.PlusOnePlusOne
        Primitives.counterKindOf(CounterTypeFilter.Named(Counters.PLUS_ONE_PLUS_ONE)) shouldBe null
        Primitives.counterKindOf(CounterTypeFilter.Named(Counters.STUN)) shouldBe Counters.STUN

        val named = CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersWithCounters(CounterTypeFilter.Named(Counters.PLUS_ONE_PLUS_ONE), 1, selfOnly = true)
                )
            )
        )
        Grammar.abilityLine.printLine(named) shouldBe null
    }

    // ---------------------------------------------------------------------------------------
    // A count that is not a number word
    // ---------------------------------------------------------------------------------------

    // The bare letter is the announced X and nothing else. `EntersWithDynamicCounters` is self by
    // default — the flag its fixed sibling spells as `selfOnly` — so the whole value is the two
    // fields the sentence names.
    "the announced X is the count a permanent brings on with it" {
        fragment("~ enters with X +1/+1 counters on it.") shouldBe CardFragment(
            script = CardScript(
                replacementEffects = listOf(
                    EntersWithDynamicCounters(CounterTypeFilter.PlusOnePlusOne, DynamicAmount.XValue)
                )
            )
        )
        roundTrips("~ enters with X +1/+1 counters on it.")
    }

    // The kind is a slot in the dynamic rules exactly as it is in the fixed ones, which is why this
    // band reached four tail families rather than the one it was named for: 18 of the 70 printed
    // "enters with X … counters" lines name a kind other than +1/+1.
    //
    // Six of those 18 still decline, and on nothing this band owns: `oil`, `study`, `echo`, `void`,
    // `scream` and `isolation` are counter kinds `CounterType` does not name, so
    // `Primitives.counterKind`'s gate rejects the word. That gate is the right place for it —
    // `CounterTypeFilter.Named` fails open to +1/+1 — so the fix is SDK vocabulary, one justified
    // enum entry per kind, and not a wider regex here.
    "the announced X reads every counter kind, not just the stat ones" {
        roundTrips("~ enters with X charge counters on it.")
        roundTrips("~ enters with X fire counters on it.")
        roundTrips("~ enters with X +1/+0 counters on it.")
        fragment("~ enters with X ice counters on it.").script.replacementEffects.single()
            .shouldBeInstanceOf<EntersWithDynamicCounters>()
            .counterType shouldBe CounterTypeFilter.Named(Counters.ICE)
        declines("~ enters with X oil counters on it.")
    }

    // Stag Beetle's sentence, with the amount vocabulary this band does not own. What matters here is
    // that the clause is read and the letter in front of it is the same value.
    "a trailing where-clause names the count the letter stands for" {
        fragment("~ enters with X +1/+1 counters on it, where X is the number of lands you control.")
            .script.replacementEffects.single()
            .shouldBeInstanceOf<EntersWithDynamicCounters>()
            .count shouldBe DynamicAmounts.landsYouControl()
        roundTrips("~ enters with X +1/+1 counters on it, where X is the number of lands you control.")
    }

    // Undergrowth Scavenger's spelling of the same model. One rule with two surfaces via
    // `alsoSpelled`, so the reader and the reconstruction cannot drift apart — and the minority
    // spelling parses and never prints, which is what makes the card a variant and not a decline.
    "the count behind the noun is the same rule, and prints as the comma spelling" {
        val behindTheNoun =
            "~ enters with a number of +1/+1 counters on it equal to the number of lands you control."
        val comma = "~ enters with X +1/+1 counters on it, where X is the number of lands you control."
        fragment(behindTheNoun) shouldBe fragment(comma)
        Grammar.abilityLine.printLine(fragment(behindTheNoun)) shouldBe comma
    }

    // The derivation is owned in one place, so a template it does not apply to fails at construction
    // rather than silently registering a spelling no card prints.
    "the equal-to spelling is derived from the comma one, and refuses a template without it" {
        Amounts.equalTo("put X {kind} counters on {self}${Amounts.WHERE_X}") shouldBe
            "put a number of {kind} counters on {self} equal to {amount}"
        shouldThrow<IllegalArgumentException> { Amounts.equalTo("put {n} {kind} counters on {self}") }
    }

    // The two SDK types read the same sentence position, so they partition `DynamicAmount` rather
    // than being tried in order. A `Fixed` amount belongs to the number word and the announced X
    // belongs to the bare row; a dynamic effect holding either must not come back as a second
    // reading of a sentence the fixed rules already print.
    "a fixed amount in the dynamic effect never prints" {
        listOf(DynamicAmount.Fixed(2), DynamicAmount.XValue).forEach { amount ->
            Amounts.namesX(amount) shouldBe false
            Grammar.abilityLine.printLine(
                CardFragment(
                    script = CardScript(
                        spellEffect = Effects.AddDynamicCounters(
                            Counters.PLUS_ONE_PLUS_ONE, amount, EffectTarget.ContextTarget(0)
                        ),
                        targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
                    )
                )
            ) shouldBe null
        }
    }

    // The step positions take the defined clauses and *not* the bare letter, because `Triggers` lifts
    // them and the announced X is silently zero anywhere but a spell. This is the assertion that
    // keeps that decision from being quietly widened later.
    "a step reads a defined count and refuses the bare letter" {
        roundTrips("Put X +1/+1 counters on target creature, where X is the number of lands you control.")
        roundTrips("Put X +1/+1 counters on ~, where X is the number of lands you control.")
        declines("Put X +1/+1 counters on target creature.")
        declines("Put X +1/+1 counters on ~.")
    }

    // The dynamic reader checks the target for `countersAdded`'s reason: the three counter sentences
    // build the same effect with a different EffectTarget, so a reader that ignored it would let each
    // rule print the others' sentence.
    "the dynamic counter sentences stay told apart by their target" {
        val onTarget =
            fragment("Put X +1/+1 counters on target creature, where X is the number of lands you control.")
        val onSelf = fragment("Put X +1/+1 counters on ~, where X is the number of lands you control.")
        onTarget shouldNotBe onSelf
        Grammar.abilityLine.printLine(onSelf) shouldBe
            "Put X +1/+1 counters on ~, where X is the number of lands you control."
    }

    // Servant of the Scale's dies trigger, and the reason it must not read. `countersOnSelf` resolves
    // from live state, so in the position this clause is usually printed in the source is already
    // gone and the amount is zero. The SDK's reading for that position is `LastKnownSourceCounters`
    // and only the trigger lift knows which is meant, so the rule refuses the live tally rather than
    // emitting a model that evaluates to nothing.
    "the source's own live counter tally is not a count a counter clause may name" {
        Amounts.namesX(DynamicAmounts.countersOnSelf(CounterTypeFilter.PlusOnePlusOne)) shouldBe false
        Amounts.namesX(DynamicAmounts.landsYouControl()) shouldBe true
        declines(
            "Put X +1/+1 counters on target creature you control, " +
                "where X is the number of +1/+1 counters on ~."
        )
    }
})
