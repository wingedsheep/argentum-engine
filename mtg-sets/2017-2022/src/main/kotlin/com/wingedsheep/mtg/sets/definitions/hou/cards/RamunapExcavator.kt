package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard

/**
 * Ramunap Excavator
 * {2}{G}
 * Creature — Snake Cleric
 * 2/3
 * You may play lands from your graveyard.
 */
val RamunapExcavator = card("Ramunap Excavator") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Cleric"
    power = 2
    toughness = 3
    oracleText = "You may play lands from your graveyard."

    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Mark Behm"
        flavorText = "\"This world was once so much more than the confines of Naktamun.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90a54d18-8403-441d-a115-ee462fabdabb.jpg?1783936015"

        ruling(
            "2017-07-14",
            "Ramunap Excavator doesn't change the times when you can play those land cards. You can still " +
                "play only one land per turn, and only during your main phase when you have priority and the stack is empty."
        )
        ruling(
            "2017-07-14",
            "Ramunap Excavator doesn't allow you to activate activated abilities (such as cycling) of land cards in your graveyard."
        )
    }
}
