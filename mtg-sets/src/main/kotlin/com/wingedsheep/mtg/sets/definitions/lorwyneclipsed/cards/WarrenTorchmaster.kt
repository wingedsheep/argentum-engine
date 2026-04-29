package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Warren Torchmaster
 * {1}{R}
 * Creature — Goblin Warrior
 * 2/2
 *
 * At the beginning of combat on your turn, you may blight 1. When you do, target creature
 * gains haste until end of turn. (To blight 1, put a -1/-1 counter on a creature you control.)
 */
val WarrenTorchmaster = card("Warren Torchmaster") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Warrior"
    power = 2
    toughness = 2
    oracleText = "At the beginning of combat on your turn, you may blight 1. When you do, " +
        "target creature gains haste until end of turn. " +
        "(To blight 1, put a -1/-1 counter on a creature you control.)"

    triggeredAbility {
        trigger = Triggers.BeginCombat
        // The haste target is chosen at resolution of the reflexive trigger, not when this
        // ability triggers (per Scryfall ruling).
        effect = ReflexiveTriggerEffect(
            action = EffectPatterns.blight(1),
            optional = true,
            reflexiveEffect = Effects.GrantKeyword(
                keyword = Keyword.HASTE,
                target = EffectTarget.ContextTarget(0)
            ),
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    filter = TargetFilter.Creature,
                    id = "target creature to gain haste"
                )
            ),
            descriptionOverride = "You may blight 1. When you do, target creature gains haste until end of turn"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Ioannis Fiore"
        flavorText = "Auntie Soot's raids begin when her torch is dipped in the oil atop her head and end when the flame burns out."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f067a14-6667-4acf-b33d-e1149188a84d.jpg?1767732778"
    }
}
