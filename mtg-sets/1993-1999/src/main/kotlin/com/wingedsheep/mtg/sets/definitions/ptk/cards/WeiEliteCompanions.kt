package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wei Elite Companions
 * {4}{B}
 * Creature — Human Soldier
 */
val WeiEliteCompanions = card("Wei Elite Companions") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Li Youliang"
        flavorText = "Cao Cao was more concerned with capabilities than lineage. He excelled at recruiting and retaining men of talent to serve him."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/843cbf18-60ac-4d97-b156-874735db61c6.jpg"
    }
}
