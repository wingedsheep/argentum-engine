package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Thrill-Kill Assassin — Return to Ravnica #81 (canonical printing)
 * {1}{B} · Creature — Human Assassin · 1/2
 *
 * Deathtouch
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 */
val ThrillKillAssassin = card("Thrill-Kill Assassin") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    power = 1
    toughness = 2
    oracleText = "Deathtouch\nUnleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)"

    keywords(Keyword.DEATHTOUCH)
    unleash()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Tyler Jacobson"
        flavorText = "As the bounty on Massacre Girl rose, so did the number of imitators."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9f32204-eda7-4184-92e5-9da8b15b2359.jpg?1783940359"
    }
}
