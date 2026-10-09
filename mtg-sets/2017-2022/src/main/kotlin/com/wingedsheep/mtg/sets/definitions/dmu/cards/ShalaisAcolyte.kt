package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.Conditions

val ShalaisAcolyte = card("Shalai's Acolyte") {
    manaCost = "{4}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 4
    oracleText = "Kicker {1}{G} (You may pay an additional {1}{G} as you cast this spell.)\nFlying\nIf this creature was kicked, it enters with two +1/+1 counters on it."

    keywordAbility(KeywordAbility.kicker("{1}{G}"))
    keywords(Keyword.FLYING)
    replacementEffect(EntersWithCounters(count = 2, selfOnly = true, condition = Conditions.WasKicked))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Zara Alfonso"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39996722-e7f2-41e4-aa1e-2b6778d7b535.jpg?1783921359"
    }
}
