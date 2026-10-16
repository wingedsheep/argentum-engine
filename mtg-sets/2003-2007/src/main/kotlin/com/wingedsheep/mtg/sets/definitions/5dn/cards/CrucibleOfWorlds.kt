package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard

/**
 * Crucible of Worlds
 * {3}
 * Artifact
 * You may play lands from your graveyard.
 */
val CrucibleOfWorlds = card("Crucible of Worlds") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You may play lands from your graveyard."

    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Ron Spencer"
        flavorText = "Amidst the darkest ashes grow the strongest seeds."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/312a6058-de08-487d-95bd-b3c56807fdd6.jpg?1783944383"

        ruling(
            "2018-07-13",
            "Crucible of Worlds doesn't change the times when you can play those land cards. You can still " +
                "play only one land per turn, and only during your main phase when you have priority and the stack is empty."
        )
        ruling(
            "2018-07-13",
            "Crucible of Worlds doesn't allow you to activate abilities (such as cycling) of land cards in your graveyard."
        )
    }
}
