package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Homarid Shaman
 * {2}{U}{U}
 * Creature — Homarid Shaman
 * 2/1
 * {U}: Tap target green creature.
 */
val HomaridShaman = card("Homarid Shaman") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Homarid Shaman"
    oracleText = "{U}: Tap target green creature."
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{U}")
        val t = target(
            "target green creature",
            TargetCreature(filter = TargetFilter.Creature.withColor(Color.GREEN))
        )
        effect = Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Amy Weber"
        flavorText = "\"The ground grew swampy; hooves and claws sank into the marshy earth. Snarls of rage and bleats of despair echoed through the trees as the waters grew higher and higher.\"\n—Kyliki of Havenwood, \"Havenwood Remembered\""
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c17c6416-86d6-46ea-aea1-41b98a66b250.jpg?1783947911"
    }
}
