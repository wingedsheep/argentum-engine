package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Stonecoil Serpent
 * {X}
 * Artifact Creature — Snake
 * 0/0
 * Reach, trample, protection from multicolored
 * This creature enters with X +1/+1 counters on it.
 */
val StonecoilSerpent = card("Stonecoil Serpent") {
    manaCost = "{X}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Snake"
    oracleText = "Reach, trample, protection from multicolored\n" +
        "This creature enters with X +1/+1 counters on it."
    power = 0
    toughness = 0

    keywords(Keyword.REACH, Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Multicolored))

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Mark Poole"
        flavorText = "Cast out of Garenbrig for his crimes, Ennor turned to fae magic to fashion the perfect weapon for his revenge."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b34bf7fd-9fe3-43e2-8cfe-7ce7cff08afe.jpg"
    }
}
