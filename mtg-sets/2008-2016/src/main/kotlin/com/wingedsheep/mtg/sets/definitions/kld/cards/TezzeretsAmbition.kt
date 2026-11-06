package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tezzeret's Ambition
 * {3}{U}{U}
 * Sorcery
 * Draw three cards. If you control no artifacts, discard a card.
 */
val TezzeretsAmbition = card("Tezzeret's Ambition") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards. If you control no artifacts, discard a card."

    spell {
        effect = Effects.DrawCards(3) then Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Artifact, negate = true),
            then = Effects.Discard(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Tyler Jacobson"
        flavorText = "\"To achieve what none have achieved before, one must dare what none have dared before.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c3700b0-d6b5-4967-add8-3adc2bc8ca86.jpg?1783937214"
    }
}
