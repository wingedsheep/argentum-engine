package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Abzan Guide
 * {3}{W}{B}{G}
 * Creature — Human Warrior
 * 4/4
 * Lifelink
 * Morph {2}{W}{B}{G}
 */
val AbzanGuide = card("Abzan Guide") {
    manaCost = "{3}{W}{B}{G}"
    typeLine = "Creature — Human Warrior"
    power = 4
    toughness = 4
    oracleText = "Lifelink\nMorph {2}{W}{B}{G}"

    keywords(Keyword.LIFELINK)
    morph = "{2}{W}{B}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Steve Prescott"
        flavorText = "\"These roads are desolate and changeable. Follow me, or die in the wastes.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b7f7158-4a31-4a1c-bf3b-574c0b09276a.jpg?1562783275"
    }
}
