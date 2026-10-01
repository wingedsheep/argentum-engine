package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Magmatic Sprinter
 * {2}{R}
 * Creature — Phyrexian Warrior
 * 3/2
 *
 * Haste
 * When this creature enters, put two oil counters on target artifact or creature you control.
 * At the beginning of your end step, return this creature to its owner's hand unless you remove
 * two oil counters from it.
 *
 * "Unless you remove two oil counters from it" is a resolution-time choice: when the creature
 * carries at least two oil counters its controller may remove them to keep it; otherwise (or on a
 * decline) it is returned to its owner's hand. With fewer than two oil counters the removal can't
 * be chosen, so it is returned without a prompt.
 */
val MagmaticSprinter = card("Magmatic Sprinter") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phyrexian Warrior"
    power = 3
    toughness = 2
    oracleText = "Haste\n" +
        "When this creature enters, put two oil counters on target artifact or creature you control.\n" +
        "At the beginning of your end step, return this creature to its owner's hand unless you " +
        "remove two oil counters from it."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.CreatureOrArtifact.youControl())
        effect = Effects.AddCounters(CounterType.OIL, 2, permanent)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.If(
            condition = Conditions.SourceCounterCountAtLeast(CounterType.OIL, 2),
            then = Effects.May(
                effect = Effects.RemoveCounters(CounterType.OIL, 2, EffectTarget.Self),
                otherwise = Effects.ReturnToHand(EffectTarget.Self),
                prompt = "Remove two oil counters from Magmatic Sprinter? (If you don't, return it to its owner's hand.)"
            ),
            otherwise = Effects.ReturnToHand(EffectTarget.Self)
        )
        description = "At the beginning of your end step, return this creature to its owner's hand " +
            "unless you remove two oil counters from it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Samuel Araya"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e6af42f-35ec-432f-9879-2a0fb938a9f6.jpg?1783918028"
    }
}
