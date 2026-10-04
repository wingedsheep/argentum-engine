package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A line with more than one clause in it: the full stop, the two joins, the anaphors that make a
 * later clause mean anything, and the one fold the whole thing rests on — that a target is declared
 * at its first mention.
 */
class SequencesTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    /** The slot one clause's effect reads, found the way a reader would: by looking at its target. */
    fun referencedSlot(effect: Effect): String? = when (effect) {
        is MoveToZoneEffect -> (effect.target as? EffectTarget.BoundVariable)?.name
        is DrawCardsEffect -> (effect.target as? EffectTarget.BoundVariable)?.name
        else -> null
    }

    "two sentences on one line are one composite" {
        fragment("Draw a card. You gain 2 life.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.DrawCards(1) then Effects.GainLife(2)
            )
        )
        roundTrips("Draw a card. You gain 2 life.")
        roundTrips("Scry 2. Draw a card. You gain 2 life.")
    }

    // The requirement belongs to the clause that introduces the referent, and the later clause reads
    // the slot without declaring one.
    "a target is declared at its first mention and referred to afterwards" {
        fragment("Target creature gets +1/+3 until end of turn. Untap that creature.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.ModifyStats(1, 3, Targets.bound()) then Effects.Untap(Targets.bound()),
                targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
            )
        )
        // "That creature" is the demonstrative; the pronoun is what prints. See
        // [Primitives.targetPronoun] — the choice of canonical is a corpus measurement, and untap is
        // the one verb where the two spellings are near even.
        Grammar.abilityLine.printLine(
            fragment("Target creature gets +1/+3 until end of turn. Untap that creature.")
        ) shouldBe "Target creature gets +1/+3 until end of turn. Untap it."
        roundTrips("Target creature gets +1/+3 until end of turn. Untap it.")
    }

    // The bug the differential found: the same four words mean the source in one position and the
    // target in the other, and reading the wrong one round-trips perfectly.
    "\"it\" is the source in a first clause and the target in a later one" {
        fragment("It gets +2/+0 until end of turn.") shouldBe CardFragment(
            script = CardScript(spellEffect = Effects.ModifyStats(2, 0, EffectTarget.Self))
        )
        fragment("Untap target creature. It gets +2/+4 until end of turn.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.Untap(Targets.bound()) then Effects.ModifyStats(2, 4, Targets.bound()),
                targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
            )
        )
        // The source form has two printed spellings — the card's own noun and the pronoun — and the
        // noun is canonical, so the pronoun comes back normalized. The *target* form has only the
        // pronoun, and round-trips.
        // `printLine` prints the abstracted token; restoring the card's own noun is
        // `NormalizedFace.restore`'s job, one layer out.
        Grammar.abilityLine.printLine(fragment("It gets +2/+0 until end of turn.")) shouldBe
            "~ gets +2/+0 until end of turn."
        roundTrips("Untap target creature. It gets +2/+4 until end of turn.")
    }

    // The joins denote the same composite the full stop does, so they parse and never print: the
    // model has no room for the conjunction and something has to be canonical.
    "the joins are alternate spellings of the full stop" {
        fragment("Draw a card and you gain 2 life.") shouldBe fragment("Draw a card. You gain 2 life.")
        fragment("Scry 2, then draw a card.") shouldBe fragment("Scry 2. Draw a card.")

        Grammar.abilityLine.printLine(fragment("Draw a card and you gain 2 life.")) shouldBe
            "Draw a card. You gain 2 life."
        Grammar.abilityLine.printLine(fragment("Scry 2, then draw a card.")) shouldBe
            "Scry 2. Draw a card."
    }

    // Mixing the separators has to fold to the same flat composite; a run with one separator could
    // not read the line, and a join rule that was itself a clause would have nested it.
    "one line can mix the joins" {
        fragment("Scry 2, then draw two cards. You lose 2 life.") shouldBe
            fragment("Scry 2. Draw two cards. You lose 2 life.")
    }

    // The elided second subject: "you" licenses "lose 1 life", so the rule spells the subject and is
    // an alternate of the full-stop run, which is what prints.
    "you draw and lose life is the full-stop run with its second subject elided" {
        fragment("You draw a card and lose 1 life.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
            )
        )
        fragment("You draw two cards and lose 2 life.") shouldBe fragment("Draw two cards. You lose 2 life.")
        Grammar.abilityLine.printLine(fragment("You draw three cards and lose 3 life.")) shouldBe
            "Draw three cards. You lose 3 life."
        // Only "you" licenses the elision: a bare "lose 1 life" is no clause of its own.
        Grammar.abilityLine.parseLine("Target player draws a card and lose 1 life.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "the X-count draw-and-lose reads with its second subject elided" {
        fragment("You draw X cards and lose X life, where X is the number of Zombies you control.") shouldBe
            fragment("You draw X cards and you lose X life, where X is the number of Zombies you control.")
    }

    // Two declared targets are numbered by the position their clause introduces them in, and the
    // first one keeps the bare name so a single-target line folds through unchanged.
    "two clauses that each declare a target are numbered by position" {
        val line = fragment("Destroy target land. Draw a card. Target player draws a card.").script
        line.targetRequirements.map { it.id } shouldBe listOf(Targets.slot(0), Targets.slot(1))
        val effects = (line.spellEffect as CompositeEffect).effects
        effects.map { referencedSlot(it) } shouldBe listOf(Targets.slot(0), null, Targets.slot(1))
        roundTrips("Destroy target land. Draw a card. Target player draws a card.")
        roundTrips("Destroy target land. ~ deals 13 damage to target creature.")
        roundTrips("Counter target spell. Return target permanent to its owner's hand.")
    }

    // Fail-closed, and now the only case that is: a pronoun clause beside a second declared target
    // would mean the most recent mention in English and the first slot in this grammar, and nothing
    // in the printed line chooses between them.
    "a pronoun clause beside a second target declines" {
        Grammar.abilityLine
            .parseLine("Destroy target land. ~ deals 13 damage to target creature. Untap that creature.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // A continuation cannot start a line: the thing it names has not been introduced.
    "a dangling anaphor is not a line" {
        Grammar.abilityLine.parseLine("Untap that creature.").shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // Crippling Chill's second sentence, and the reason it is a continuation: the "it" is the
    // creature the first clause tapped, not the source.
    "the doesn't-untap rider reads the creature the previous clause tapped" {
        fragment("Tap target creature. It doesn't untap during its controller's next untap step.") shouldBe
            CardFragment(
                script = CardScript(
                    spellEffect = Effects.Tap(Targets.bound()) then
                        Effects.GrantKeyword(
                            AbilityFlag.DOESNT_UNTAP,
                            Targets.bound(),
                            Duration.UntilAfterAffectedControllersNextUntap,
                        ),
                    targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
                )
            )
        roundTrips("Tap target creature. It doesn't untap during its controller's next untap step.")
        roundTrips(
            "When ~ enters, tap target creature an opponent controls. " +
                "It doesn't untap during its controller's next untap step."
        )
    }

    // The doesn't-untap band. The rider is a `SelfSteps.retargetable` member, so the demonstrative
    // reads in the later position (Stitched Mangler) and prints back as the pronoun.
    "the doesn't-untap rider reads the demonstrative and prints the pronoun" {
        val line = "Tap target creature. That creature doesn't untap during its controller's next untap step."
        fragment(line) shouldBe
            fragment("Tap target creature. It doesn't untap during its controller's next untap step.")
        Grammar.abilityLine.printLine(fragment(line)) shouldBe
            "Tap target creature. It doesn't untap during its controller's next untap step."
    }

    // …and about the source it says "your", which is the spelling the CHK pain-free duals print after
    // their coloured mana: one ability per colour, each `AddMana then` the same freeze.
    "the source doesn't untap during your next untap step" {
        fragment("~ doesn't untap during your next untap step.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    EffectTarget.Self,
                    Duration.UntilAfterAffectedControllersNextUntap,
                )
            )
        )
        roundTrips("{T}: Add {U} or {B}. ~ doesn't untap during your next untap step.")
        // The source's controller is "you", so the other possessive is not the source's spelling.
        Grammar.abilityLine.parseLine("~ doesn't untap during its controller's next untap step.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // The `.` decline band. The name is not an anaphor — it denotes the card in any sentence — so a
    // later clause can spell it, and ninety-four lines were dying on their own full stop for want of
    // that one membership.
    "the source's name reads in a later clause as well as a first one" {
        fragment("Draw a card. Put a +1/+1 counter on ~.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.DrawCards(1) then
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            )
        )
        roundTrips("Draw a card. Put a +1/+1 counter on ~.")
        roundTrips("{T}: Add {C}. Put a point counter on ~.")
        roundTrips("{2}, {T}: Draw a card. Transform ~.")
        // " and " is an alternate join, so the same line prints with the full stop the run canonicalizes on.
        Grammar.abilityLine.printLine(
            fragment("Whenever a land you control enters, draw a card and put a +1/+1 counter on ~.")
        ) shouldBe "Whenever a land you control enters, draw a card. Put a +1/+1 counter on ~."
    }

    // …and the pronoun in that position is the *target*, over the whole retargetable vocabulary
    // rather than the five sentences somebody had written out by hand.
    "a later clause's pronoun reaches every verb the source's does" {
        fragment("Put two +1/+1 counters on target creature. Untap it.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, Targets.bound()) then
                    Effects.Untap(Targets.bound()),
                targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
            )
        )
        roundTrips("Put two +1/+1 counters on target creature. Untap it.")
        roundTrips("Untap target creature. It gets +2/+4 and gains reach until end of turn.")
        roundTrips("Target creature gets +2/+0 until end of turn. Regenerate it.")
        roundTrips("Put a +1/+1 counter on target creature. It gains vigilance until end of turn.")
    }

    // The removal spell's death replacement is a member of the same vocabulary: its object is the
    // creature the damage or the shrink was aimed at, and Oracle's "that creature" is a variant of
    // the pronoun the run canonicalizes on.
    "the exile-instead rider marks the earlier clause's target" {
        fragment("~ deals 3 damage to target creature. If that creature would die this turn, exile it instead.")
            .script.spellEffect shouldBe (
            Effects.DealDamage(3, Targets.bound()) then Effects.MarkExileOnDeath(Targets.bound())
        )
        roundTrips("Target creature gets -5/-5 until end of turn. If it would die this turn, exile it instead.")
        Grammar.abilityLine.printLine(
            fragment("~ deals 2 damage to target creature. If that creature would die this turn, exile it instead.")
        ) shouldBe "~ deals 2 damage to target creature. If it would die this turn, exile it instead."
    }

    // A pronoun with nothing to point at is not a model — Creeping Tar Pit spells "it" about the
    // permanent the same clause animated, and reading it as a target would round-trip perfectly.
    "a run that reads the target slot without declaring it declines" {
        Grammar.abilityLine
            .parseLine("~ becomes a 3/3 Elemental creature until end of turn. Untap it.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // The four zone verbs the pronoun used to be frozen into. "Exile ~." is twenty-nine spells'
    // whole second line, and it was unreadable because the rule spelled its subject in the template.
    "the zone verbs take a subject like every other member" {
        fragment("Exile ~.") shouldBe CardFragment(
            script = CardScript(spellEffect = Effects.Move(EffectTarget.Self, Zone.EXILE))
        )
        roundTrips("Exile ~.")
        roundTrips("{2}{U}{U}: Return ~ to its owner's hand.")
        roundTrips("Shuffle ~ into its owner's library.")
        roundTrips("Put ~ on top of its owner's library.")
    }

    // The wrappers are clauses, so a trigger and an activated ability get sequences for free.
    "a sequence is the same clause wherever it lands" {
        roundTrips("When ~ enters, draw a card. You gain 2 life.")
        roundTrips("{T}: Draw a card. You gain 2 life.")
        roundTrips("You may draw a card.")
    }
})
