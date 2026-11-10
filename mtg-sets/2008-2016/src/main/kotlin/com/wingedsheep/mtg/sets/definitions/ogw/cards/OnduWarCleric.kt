package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ondu War Cleric
 * {1}{W}
 * Creature — Human Cleric Ally
 * 2/2
 * Cohort — {T}, Tap an untapped Ally you control: You gain 2 life.
 */
val OnduWarCleric = card("Ondu War Cleric") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric Ally"
    power = 2
    toughness = 2
    oracleText = "Cohort — {T}, Tap an untapped Ally you control: You gain 2 life."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype(Subtype.ALLY)),
        )
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Ben Maier"
        flavorText = "\"I will follow the light of Iona into battle, and I will know no fear.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b255d34-04fa-4d3b-9fc1-8f980ef1f054.jpg"
    }
}
