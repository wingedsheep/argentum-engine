package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The fight band (CR 701.14): "Target creature you control fights target creature you don't
 * control.", "When ~ enters, it fights up to one target creature an opponent controls.", and the
 * later clause "It fights target creature you don't control." after a clause that chose the first.
 *
 * `Effects.Fight` names both fighters, so what these assert is *who* each one is in each position —
 * the reading a byte-perfect round trip cannot check.
 */
class FightTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    val self = Normalizer.SELF

    "two targets one sentence declares fight each other" {
        val script = fragment("Target creature you control fights target creature you don't control.").script
        script.spellEffect shouldBe Effects.Fight(Targets.bound(0), Targets.bound(1))
        script.targetRequirements.size shouldBe 2
        script.targetRequirements[1].optional shouldBe false
        roundTrips("Target creature you control fights target creature you don't control.")
        roundTrips("Target creature you control fights target creature an opponent controls.")
        roundTrips("Target creature you control fights up to one target creature you don't control.")
    }

    // "Another" here contrasts the second target with the first (`TargetOther`), which the
    // quantifier rows' `excludeSelf` does not say — so it declines rather than reading as that.
    "another target after a first one declines" {
        Grammar.abilityLine.parseLine("Target creature fights another target creature.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "the source fights a target it declares, whichever way it is named" {
        val effect = fragment("When $self enters, it fights up to one target creature an opponent controls.")
            .script.triggeredAbilities.single().effect
        effect shouldBe Effects.Fight(EffectTarget.Self, Targets.bound())
        roundTrips("When $self enters, $self fights up to one target creature an opponent controls.")
        roundTrips("When $self enters, $self fights another target creature.")
    }

    // The fighter is the attached creature, not the Aura — Pitiless Fists.
    "the attached creature fights a target" {
        val text = "When $self enters, enchanted ${Normalizer.ATTACHED_NOUN} fights up to one target " +
            "creature an opponent controls."
        fragment(text).script.triggeredAbilities.single().effect shouldBe
            Effects.Fight(EffectTarget.EnchantedCreature, Targets.bound())
        roundTrips(text)
    }

    // "It" is the first clause's target and the object is a second one: the later clause names
    // the target declared before its own, and the numbering puts it in slot 0.
    "a later clause's it fights a second target" {
        val text = "Target creature you control gets +1/+0 until end of turn. It fights target creature you don't control."
        val effects = (fragment(text).script.spellEffect as CompositeEffect).effects
        effects[1] shouldBe Effects.Fight(Targets.bound(0), Targets.bound(1))
        fragment(text).script.targetRequirements.size shouldBe 2
        roundTrips(text)
        roundTrips(
            "Put a +1/+1 counter on target creature you control. It fights target creature an opponent controls."
        )
    }

    // With no earlier target the prior slot has nothing to name, so the line declines rather than
    // leaving the placeholder in a model.
    "a fight against an earlier target needs one" {
        Grammar.abilityLine.parseLine("Draw a card. It fights target creature you don't control.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }
})
