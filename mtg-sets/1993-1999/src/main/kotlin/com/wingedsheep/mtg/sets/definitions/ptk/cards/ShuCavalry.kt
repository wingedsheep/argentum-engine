package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Shu Cavalry
 * {2}{W}
 * Creature — Human Soldier
 * 2/2
 * Horsemanship
 */
val ShuCavalry = card("Shu Cavalry") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywordAbility(KeywordAbility.Simple(Keyword.HORSEMANSHIP))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Li Xiaohua"
        flavorText = "In establishing the Shu kingdom, Liu Bei's forces fought against Ma Chao at Chengdu. Eventually, Ma Chao surrendered and became one of Liu Bei's Tiger Generals."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0433bee7-406d-4dd3-8d1b-dfd6cb64d038.jpg"
    }
}
