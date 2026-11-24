package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Splatter Thug — Return to Ravnica #105 (canonical printing)
 * {2}{R} · Creature — Human Warrior · 2/2
 *
 * First strike
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 */
val SplatterThug = card("Splatter Thug") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "First strike\nUnleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)"

    keywords(Keyword.FIRST_STRIKE)
    unleash()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c511805-3392-4033-8679-811711a0aaca.jpg?1783940353"
    }
}
