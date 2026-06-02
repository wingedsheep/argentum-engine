package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
/**
 * Cremate
 * {B}
 * Instant
 * Exile target card from a graveyard.
 * Draw a card.
 */
val Cremate = card("Cremate") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target card from a graveyard.\nDraw a card."

    spell {
        val t = target("target", Targets.CardInGraveyard)
        effect = Effects.Move(t, Zone.EXILE)
            .then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Andrew Goldhawk"
        flavorText = "Death's embrace need not be cold."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1095cdfe-8060-4a73-bacf-9f983152b486.jpg?1562898378"
    }
}
