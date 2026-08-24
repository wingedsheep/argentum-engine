package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Alert Shu Infantry
 * {2}{W}
 * Creature — Human Soldier
 * 2 / 2
 *
 * Vigilance
 */
val AlertShuInfantry = card("Alert Shu Infantry") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance"

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Solomon Au Yeung"
        flavorText = "Kongming's predecessor helped Liu Bei win the battle of Fancheng by recognizing and defeating Cao Ren's use of the dreaded \"Eight Gold Locks\" formation."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c94a92a9-060e-42d3-a8d1-49425defc08a.jpg"
    }
}
