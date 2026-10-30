package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Efficient Construction
 * {3}{U}
 * Enchantment
 *
 * Whenever you cast an artifact spell, create a 1/1 colorless Thopter artifact creature token
 * with flying.
 */
val EfficientConstruction = card("Efficient Construction") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an artifact spell, create a 1/1 colorless Thopter artifact " +
        "creature token with flying."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Artifact)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/f/d/fd03a2b9-bfd7-4e1d-b660-e9f945699b78.jpg?1783937238",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Jonas De Ro"
        flavorText = "For some of Ghirapur's aerowrights, building thopters is a byproduct of larger construction efforts."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abfbe1d5-beb7-49b8-a504-f1cc47ee4731.jpg?1783936773"
        ruling("2017-02-09", "Efficient Construction's triggered ability resolves before the spell that caused it to trigger. The ability will resolve even if that spell is countered.")
    }
}
