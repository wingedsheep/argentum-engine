package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ancestral Vengeance
 * {B}{B}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, put a +1/+1 counter on target creature you control.
 * Enchanted creature gets -1/-1.
 */
val AncestralVengeance = card("Ancestral Vengeance") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, put a +1/+1 counter on target creature you control.\n" +
        "Enchanted creature gets -1/-1."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = creature
        )
    }

    staticAbility {
        ability = ModifyStats(-1, -1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Yohann Schepacz"
        flavorText = "The defenses of the Abzan extend well beyond the walls of their fortresses."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0d0094e-07ff-4b47-8c6e-500adabf12e4.jpg"
    }
}
