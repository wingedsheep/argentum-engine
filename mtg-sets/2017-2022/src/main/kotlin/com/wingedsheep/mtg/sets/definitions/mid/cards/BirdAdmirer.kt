package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Bird Admirer // Wing Shredder (Innistrad: Midnight Hunt)
 * {2}{G}
 * Creature — Human Archer Werewolf // Creature — Werewolf
 *
 * Front — Bird Admirer (1/4): Reach. Daybound.
 * Back  — Wing Shredder (3/5): Reach. Nightbound.
 */

private val BirdAdmirerFront = card("Bird Admirer") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Archer Werewolf"
    power = 1
    toughness = 4
    oracleText = "Reach\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.REACH)
    daybound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71ccc444-54c8-4f7c-a425-82bc3eea1eb0.jpg?1783925590"
    }
}

private val WingShredder = card("Wing Shredder") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 5
    oracleText = "Reach\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.REACH)
    nightbound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/back/7/1/71ccc444-54c8-4f7c-a425-82bc3eea1eb0.jpg?1783925590"
    }
}

val BirdAdmirer: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BirdAdmirerFront,
    backFace = WingShredder,
)
