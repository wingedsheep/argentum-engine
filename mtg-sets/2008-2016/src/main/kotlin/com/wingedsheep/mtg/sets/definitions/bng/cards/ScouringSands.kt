package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Scouring Sands — Born of the Gods #110
 * {1}{R} · Sorcery
 *
 * Scouring Sands deals 1 damage to each creature your opponents control. Scry 1.
 */
val ScouringSands = card("Scouring Sands") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Scouring Sands deals 1 damage to each creature your opponents control. Scry 1. " +
        "(Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        effect = Patterns.Group.dealDamageToAll(1, GroupFilter.AllCreaturesOpponentsControl) then
            Patterns.Library.scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Jonas De Ro"
        flavorText = "When Purphoros cleans the ashes from his forge, entire cities vanish."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/273f25fc-9c9f-4b73-a28b-1461d8fcd443.jpg?1783939539"
    }
}
