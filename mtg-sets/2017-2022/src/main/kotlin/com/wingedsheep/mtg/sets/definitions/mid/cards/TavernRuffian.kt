package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Tavern Ruffian // Tavern Smasher (Innistrad: Midnight Hunt)
 * {3}{R}
 * Creature — Human Warrior Werewolf // Creature — Werewolf
 *
 * Front — Tavern Ruffian (2/5): Daybound.
 * Back  — Tavern Smasher (6/5): Nightbound.
 */

private val TavernRuffianFront = card("Tavern Ruffian") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior Werewolf"
    power = 2
    toughness = 5
    oracleText = "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    daybound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Zoltan Boros"
        flavorText = "She dispatched every challenger with a sickening snap of bone."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d7b2d05-ce5c-4b73-8fa6-d9b69619d58c.jpg?1783925594"
    }
}

private val TavernSmasher = card("Tavern Smasher") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 6
    toughness = 5
    oracleText = "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    nightbound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Zoltan Boros"
        flavorText = "With every arm in sight broken, she turned her attention to the furniture."
        imageUri = "https://cards.scryfall.io/normal/back/1/d/1d7b2d05-ce5c-4b73-8fa6-d9b69619d58c.jpg?1783925594"
    }
}

val TavernRuffian: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = TavernRuffianFront,
    backFace = TavernSmasher,
)
