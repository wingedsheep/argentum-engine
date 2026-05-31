package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dragon Sniper
 * {G}
 * Creature — Human Archer
 * 1/1
 *
 * Vigilance, reach, deathtouch
 */
val DragonSniper = card("Dragon Sniper") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Archer"
    power = 1
    toughness = 1
    oracleText = "Vigilance, reach, deathtouch"

    keywords(Keyword.VIGILANCE, Keyword.REACH, Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "David Auden Nash"
        flavorText = "During Dromoka's reign, Qatros Karst City was a haven for the Abzan rebellion. " +
            "Its current residents have not lost their proficiency against their former foes."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/074b1e00-45bb-4436-8f5e-058512b2d08a.jpg?1743204520"
    }
}
