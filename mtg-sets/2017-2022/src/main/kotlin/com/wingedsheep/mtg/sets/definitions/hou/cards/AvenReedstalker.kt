package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val AvenReedstalker = card("Aven Reedstalker") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Warrior"
    oracleText = "Flash\nFlying"
    power = 2
    toughness = 3

    keywords(Keyword.FLASH, Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Scott Murphy"
        flavorText = "\"Our best chance against the Eternals will be tactics they've never seen and styles they've never trained in.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0c2d70a-d646-44eb-9f65-ddf6e329ab43.jpg?1783936054"
    }
}
