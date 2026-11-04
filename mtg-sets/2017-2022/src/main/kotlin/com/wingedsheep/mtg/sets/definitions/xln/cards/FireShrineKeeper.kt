package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val FireShrineKeeper = card("Fire Shrine Keeper") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "Menace\n{7}{R}, {T}, Sacrifice this creature: It deals 3 damage to each of up to two target creatures."
    power = 1
    toughness = 1

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{7}{R}"), Costs.Tap, Costs.SacrificeSelf)
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.DealDamage(3, EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Zoltan Boros"
        flavorText = "Though grimed with the dust of centuries, the ancient shrine was not unguarded."
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54135905-9b15-4bf9-8f4d-4ea7a0c75b77.jpg?1783935745"
        ruling("2017-09-29", "You can’t target the same creature twice to have Fire Shrine Keeper deal 6 damage to it.")
    }
}
