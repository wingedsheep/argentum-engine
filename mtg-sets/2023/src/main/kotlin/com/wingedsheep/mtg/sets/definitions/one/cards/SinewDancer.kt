package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sinew Dancer
 * {W}
 * Creature — Phyrexian Soldier
 * 1/1
 * {3}{W}, {T}: Tap target creature.
 * Corrupted — {W}, {T}: Tap target creature. Activate only if an opponent has three or more
 * poison counters.
 */
val SinewDancer = card("Sinew Dancer") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Soldier"
    power = 1
    toughness = 1
    oracleText = "{3}{W}, {T}: Tap target creature.\n" +
        "Corrupted — {W}, {T}: Tap target creature. Activate only if an opponent has three or more poison counters."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{W}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.Corrupted))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Campbell White"
        flavorText = "He spreads word of the Argent Etchings to a captive audience."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebad4fcc-4f78-48dc-b236-c78c22edc1e9.jpg?1783918073"
    }
}
