package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Spawn of Rix Maadi — Return to Ravnica #199 (canonical printing)
 * {3}{B}{R} · Creature — Horror · 5/3
 *
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 */
val SpawnOfRixMaadi = card("Spawn of Rix Maadi") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Horror"
    power = 5
    toughness = 3
    oracleText = "Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)"

    unleash()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "199"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6196e702-7f76-49db-8ee4-bd343359d498.jpg?1783940332"
    }
}
