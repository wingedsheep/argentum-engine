package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val DualShot = card("Dual Shot") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Dual Shot deals 1 damage to each of up to two target creatures."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.DealDamage(1, EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Joseph Meehan"
        flavorText = "There is an eagerness among the Quiver of Kessig to prove they are the equal of the other cathar orders."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7ac4fa4-4a03-41a9-b7e4-c3a6da89472f.jpg?1783937755"
        ruling("2017-09-29", "You can’t target the same creature twice to have Dual Shot deal 2 damage to it.")
    }
}
