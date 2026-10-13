package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val YavimayaSteelcrusher = card("Yavimaya Steelcrusher") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Ape Warrior"
    power = 2
    toughness = 2
    oracleText = "Enlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)\n{1}, Sacrifice this creature: Destroy target artifact."

    staticAbility { ability = Enlist }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        val artifact = target(TargetFilter.Artifact)
        effect = Effects.Destroy(artifact)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25175622-7b68-4916-adf2-3bf2d82e8356.jpg?1783921307"
    }
}
