package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Waterwind Scout — {2}{U}
 * Creature — Merfolk Scout
 * 2/2
 * Flying
 * When this creature enters, create a Map token.
 */
val WaterwindScout = card("Waterwind Scout") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Scout"
    oracleText = "Flying\nWhen this creature enters, create a Map token. (It's an artifact with \"{1}, {T}, Sacrifice this token: Target creature you control explores. Activate only as a sorcery.\")"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateMapToken()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a7738fb-0a1b-4010-b8c0-e1129739c765.jpg?1782694543"
    }
}
