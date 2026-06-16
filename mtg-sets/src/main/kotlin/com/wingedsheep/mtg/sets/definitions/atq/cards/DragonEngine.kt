package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon Engine
 * {3}
 * Artifact Creature — Construct
 * 1/3
 * {2}: This creature gets +1/+0 until end of turn.
 */
val DragonEngine = card("Dragon Engine") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 3
    oracleText = "{2}: This creature gets +1/+0 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Anson Maddocks"
        flavorText = "Those who believed the city of Kroog would never fall to Mishra's forces severely underestimated the might of his war machines."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07793a71-1106-4303-b620-e403bd378020.jpg?1562896584"
    }
}
