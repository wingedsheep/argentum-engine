package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wu Light Cavalry
 * {1}{U}
 * Creature — Human Soldier
 * 1 / 2
 *
 * Horsemanship (This creature can't be blocked except by creatures with horsemanship.)
 */
val WuLightCavalry = card("Wu Light Cavalry") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Huang Qishi"
        flavorText = "A cunning strategist, Cao Ren tricked Zhou Yu by letting him enter Nanjun. Zhou Yu thought he was capturing the city until Cao Ren's arrows rained down upon him."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60e55b25-fde2-4b57-b5db-65b75262ff3d.jpg"
    }
}
