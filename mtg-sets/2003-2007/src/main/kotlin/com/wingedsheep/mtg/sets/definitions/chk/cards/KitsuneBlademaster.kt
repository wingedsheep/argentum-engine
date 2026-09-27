package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Kitsune Blademaster
 * {2}{W}
 * Creature — Fox Samurai
 * 2/2
 * First strike
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 *
 * First strike is an engine-live keyword, so it stays a plain `keywords(…)` declaration.
 */
val KitsuneBlademaster = card("Kitsune Blademaster") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Samurai"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)"

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.bushido(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Keith Garletts"
        flavorText = "Those kitsune trained in the blade preferred to fight with a blade-catching jitte in the off hand, buying them just enough time to deliver the first deadly cut."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb9d108d-ee19-4b1d-9d4b-b4c4d9b8ad0d.jpg?1783944336"
    }
}
