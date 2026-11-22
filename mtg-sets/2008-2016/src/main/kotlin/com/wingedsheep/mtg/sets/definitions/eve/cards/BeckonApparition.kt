package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Beckon Apparition
 * {W/B}
 * Instant
 * Exile target card from a graveyard. Create a 1/1 white and black Spirit creature token with flying.
 */
val BeckonApparition = card("Beckon Apparition") {
    manaCost = "{W/B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Exile target card from a graveyard. Create a 1/1 white and black Spirit creature token with flying."

    spell {
        val t = target(TargetFilter.CardInGraveyard)
        effect = Effects.Exile(t) then
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE, Color.BLACK),
                creatureTypes = setOf("Spirit"),
                keywords = setOf(Keyword.FLYING)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Larry MacDougall"
        flavorText = "When a graveyard is full, the gwyllions rattle the tombs and unleash the spirits into the sky."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bae1a3b-881b-4b10-ac5f-822c809edc36.jpg"
    }
}
