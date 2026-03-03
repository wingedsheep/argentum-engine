package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Blinding Spray
 * {4}{U}
 * Instant
 * Creatures your opponents control get -4/-0 until end of turn.
 * Draw a card.
 */
val BlindingSpray = card("Blinding Spray") {
    manaCost = "{4}{U}"
    typeLine = "Instant"
    oracleText = "Creatures your opponents control get -4/-0 until end of turn.\nDraw a card."

    spell {
        effect = Effects.ModifyStatsForAll(
            power = -4,
            toughness = 0,
            filter = GroupFilter.AllCreaturesOpponentsControl
        ).then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Wayne Reynolds"
        flavorText = "\"The stronger our enemies seem, the more vulnerable they are.\"\n—Sultai secret"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b588355-c349-458d-aeb7-0e2780caa3f9.jpg?1562790980"
    }
}
