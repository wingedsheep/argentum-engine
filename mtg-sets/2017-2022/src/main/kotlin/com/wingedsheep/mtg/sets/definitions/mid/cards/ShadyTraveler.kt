package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Shady Traveler // Stalking Predator (Innistrad: Midnight Hunt)
 * {2}{B}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Shady Traveler (2/3): Menace. Daybound.
 * Back  — Stalking Predator (4/4): Menace. Nightbound.
 */

private val ShadyTravelerFront = card("Shady Traveler") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.MENACE)
    daybound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Lie Setiawan"
        flavorText = "The stranger spoke to no one, and the townspeople gladly returned the favor."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55f0666a-5c3e-492b-b4ea-42fa7f24661b.jpg?1783925615"
    }
}

private val StalkingPredator = card("Stalking Predator") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.MENACE)
    nightbound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Lie Setiawan"
        flavorText = "Tomorrow a new village, and new victims."
        imageUri = "https://cards.scryfall.io/normal/back/5/5/55f0666a-5c3e-492b-b4ea-42fa7f24661b.jpg?1783925615"
    }
}

val ShadyTraveler: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ShadyTravelerFront,
    backFace = StalkingPredator,
)
