package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Silver Knight
 * {W}{W}
 * Creature — Human Knight
 * 2/2
 * First strike, protection from red
 */
val SilverKnight = card("Silver Knight") {
    manaCost = "{W}{W}"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "First strike, protection from red"

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.ProtectionFromColor(Color.RED))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Eric Peterson"
        flavorText = "Otaria's last defense against the wave of chaos threatening to engulf it."
        imageUri = "https://cards.scryfall.io/large/front/9/3/93f559da-08ad-402d-8c6b-3050bce5867b.jpg?1562532178"
    }
}
