package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Ronin Houndmaster
 * {2}{R}
 * Creature — Human Samurai
 * 2/2
 * Haste
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 *
 * Haste is an engine-live keyword, so it stays a plain `keywords(…)` declaration.
 */
val RoninHoundmaster = card("Ronin Houndmaster") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Samurai"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)"

    keywords(Keyword.HASTE)
    keywordAbility(KeywordAbility.bushido(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Edward P. Beard, Jr."
        flavorText = "Some samurai fell so far out of grace that only dogs would keep them company."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/614ead7b-1975-4a99-bdc2-f8afc6cf92d7.jpg?1783944297"
    }
}
