package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Squire
 * {1}{W}
 * Creature — Bird Soldier
 * 1/1
 * Flying
 * Exalted
 */
val AvenSquire = card("Aven Squire") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 1
    toughness = 1
    oracleText = "Flying\nExalted (Whenever a creature you control attacks alone, that creature gets +1/+1 " +
        "until end of turn.)"

    keywords(Keyword.FLYING, Keyword.EXALTED)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "David Palumbo"
        flavorText = "When the meek charge into battle, courage becomes infectious."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60301dbd-40d1-4af8-8e2b-797febfa859f.jpg?1783942493"
    }
}
