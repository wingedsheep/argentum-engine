package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wei Scout
 * {1}{B}
 * Creature — Human Soldier Scout
 * 1/1
 * Horsemanship (This creature can't be blocked except by creatures with horsemanship.)
 */
val WeiScout = card("Wei Scout") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier Scout"
    power = 1
    toughness = 1
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Jiang Zhuqing"
        flavorText = "\"He will win who, prepared himself, waits to take the enemy unprepared.\"\n—Sun Tzu, *Art of War* (trans. Giles)"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a11d58fb-fb70-4e8f-8f64-232ad2c1f59b.jpg"
    }
}
