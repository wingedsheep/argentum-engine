package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Rakdos Drake — Dragon's Maze #28 (canonical printing)
 * {2}{B} · Creature — Drake · 1/2
 *
 * Flying
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 */
val RakdosDrake = card("Rakdos Drake") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Drake"
    power = 1
    toughness = 2
    oracleText = "Flying\nUnleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)"

    keywords(Keyword.FLYING)
    unleash()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Karl Kopinski"
        flavorText = "Rakdos rioters paint their faces to resemble the drakes' bloodied muzzles."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9c1bfd7-b8b2-4db7-9ea7-a2d643a83589.jpg"
    }
}
