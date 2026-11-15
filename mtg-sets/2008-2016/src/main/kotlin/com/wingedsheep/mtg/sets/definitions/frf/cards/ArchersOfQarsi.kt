package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Archers of Qarsi
 * {3}{G}
 * Creature — Snake Archer
 * 5/2
 * Defender
 * Reach (This creature can block creatures with flying.)
 */
val ArchersOfQarsi = card("Archers of Qarsi") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Archer"
    power = 5
    toughness = 2
    oracleText = "Defender\n" +
        "Reach (This creature can block creatures with flying.)"

    keywords(Keyword.DEFENDER, Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Alejandro Mirabal"
        flavorText = "\"We think of the naga as creeping cutthroats, slitherers in the swamps. But they are equally at home in the trees.\"\n—Goton, Qal Sisma shaman"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fcf0074-162a-4cc0-9726-8672a0261307.jpg"
    }
}
