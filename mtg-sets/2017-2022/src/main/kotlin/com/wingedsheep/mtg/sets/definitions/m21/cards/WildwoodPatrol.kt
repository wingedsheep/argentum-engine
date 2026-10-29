package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wildwood Patrol (Core Set 2021 #339)
 * {2}{G} · Creature — Centaur Scout · 4/2
 *
 * Trample
 */
val WildwoodPatrol = card("Wildwood Patrol") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Scout"
    power = 4
    toughness = 2
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)"

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "339"
        artist = "Dan Murayama Scott"
        flavorText = "If you're lucky, the centaurs will give a single warning to leave their territory before driving a spear into you. If you're unlucky, the spear is the warning."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cceaf927-261e-4d61-9fac-eb8abbe87593.jpg?1783930617"
    }
}
