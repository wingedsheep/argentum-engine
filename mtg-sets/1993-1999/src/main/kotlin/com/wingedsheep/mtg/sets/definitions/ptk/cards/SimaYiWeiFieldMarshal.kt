package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sima Yi, Wei Field Marshal
 * {5}{B}
 * Legendary Creature — Human Soldier
 * Power is the number of Swamps you control; toughness 4.
 *
 * A characteristic-defining ability: the power lives in `creatureStats`, not in a static ability,
 * so it applies in every zone. `dynamicPower` with a zero offset builds the plain
 * `CharacteristicValue.Dynamic` over the Swamps you control.
 */
val SimaYiWeiFieldMarshal = card("Sima Yi, Wei Field Marshal") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Soldier"
    dynamicPower(
        DynamicAmount.AggregateBattlefield(
            Player.You,
            GameObjectFilter.Land.withSubtype(Subtype.SWAMP)
        )
    )
    toughness = 4
    oracleText = "Sima Yi's power is equal to the number of Swamps you control."

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Gao Yan"
        flavorText = "Sima Yi fought for four generations of the Cao family before his own grandson became emperor and united the three kingdoms."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6500b690-d601-4c6d-baea-552e366ea242.jpg"
    }
}
