package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Oxidda Finisher
 * {5}{R}{R}
 * Creature — Ogre Rebel
 * 7/5
 * Affinity for Equipment (This spell costs {1} less to cast for each Equipment you control.)
 * Trample
 */
val OxiddaFinisher = card("Oxidda Finisher") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Rebel"
    power = 7
    toughness = 5
    oracleText = "Affinity for Equipment (This spell costs {1} less to cast for each Equipment you control.)\nTrample"

    keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.EQUIPMENT))
    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Vladimir Krisetskiy"
        flavorText = "Popular wisdom says that Phyrexians don't feel fear. But popular wisdom has never tried to hold a bridge against an angry ogre."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/7177431e-5cc4-4ecd-8848-f51903289072.jpg?1783918025"
    }
}
