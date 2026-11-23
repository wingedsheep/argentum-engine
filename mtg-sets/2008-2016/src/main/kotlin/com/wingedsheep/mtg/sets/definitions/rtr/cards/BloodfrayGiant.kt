package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodfray Giant — Return to Ravnica #89 (canonical printing)
 * {2}{R}{R} · Creature — Giant · 4/3
 *
 * Trample
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 */
val BloodfrayGiant = card("Bloodfray Giant") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 4
    toughness = 3
    oracleText = "Trample\nUnleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)"

    keywords(Keyword.TRAMPLE)
    unleash()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Steve Argyle"
        flavorText = "The star performer of the Juri Revue, his shows have fans and blood spilling into the street."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8c468cd-1255-4257-9a69-c6b40a27c427.jpg"
    }
}
