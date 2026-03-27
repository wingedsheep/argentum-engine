package com.wingedsheep.mtg.sets.definitions.custom

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sekshaas, Early Sleeper — {1}{G}{W}
 * Legendary Creature — Human Rabbit Rogue
 * 2/3
 *
 * Early to Rest — At the beginning of each end step, exile Sekshaas, Early Sleeper.
 * Return him to the battlefield at the beginning of your next upkeep.
 *
 * {T}: Create a Food token named "Just One Glass." Activate only once each turn.
 *
 * Climb {1}{G} ({1}{G}: Put a +1/+1 counter on this creature. Activate only as a sorcery.)
 */
/**
 * "Just One Glass" — a named Food token with custom art, created by Sekshaas.
 */
val JustOneGlassToken = card("Just One Glass") {
    typeLine = "Artifact - Food"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.GainLife(3)
    }

    metadata {
        imageUri = "/images/custom/just-one-glass.jpeg"
    }
}

val SekshaasEarlySleeper = card("Sekshaas, Early Sleeper") {
    manaCost = "{1}{G}{W}"
    typeLine = "Legendary Creature - Human Rabbit Rogue"
    power = 2
    toughness = 3
    oracleText = "Early to Rest — At the beginning of each end step, exile Sekshaas, Early Sleeper. " +
        "Return him to the battlefield at the beginning of your next upkeep. " +
        "(He'll make an exception for draft night. Otherwise, he went to bed — " +
        "but don't worry, he'll be back well-rested.)\n" +
        "{T}: Create a Food token named \"Just One Glass.\" Activate only once each turn. " +
        "Seriously. Just one.\n" +
        "Climb {1}{G} ({1}{G}: Put a +1/+1 counter on this creature. Activate only as a sorcery.)"

    // Early to Rest — exile at each end step, return at your next upkeep with haste
    triggeredAbility {
        trigger = Triggers.EachEndStep
        effect = CompositeEffect(
            listOf(
                MoveToZoneEffect(EffectTarget.Self, Zone.EXILE),
                CreateDelayedTriggerEffect(
                    step = Step.UPKEEP,
                    effect = CompositeEffect(listOf(
                        MoveToZoneEffect(EffectTarget.Self, Zone.BATTLEFIELD),
                        GrantKeywordEffect(Keyword.HASTE, EffectTarget.Self)
                    )),
                    fireOnlyOnControllersTurn = true
                )
            )
        )
    }

    // {T}: Create a Food token named "Just One Glass" — once per turn
    activatedAbility {
        cost = Costs.Tap
        effect = CreatePredefinedTokenEffect("Just One Glass", 1)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "Create a Food token named \"Just One Glass.\""
    }

    // Climb {1}{G} — +1/+1 counter, sorcery speed
    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        imageUri = "/images/custom/sekshaas-early-sleeper.jpeg"
        artist = "Rippo Reizen"
        collectorNumber = "1"
        flavorText = "\"You can only really imagine what it is like to be busy when you have kids.\" " +
            "— Sekshaas, 21:30 on New Year's Eve, already in pajamas"
    }
}
