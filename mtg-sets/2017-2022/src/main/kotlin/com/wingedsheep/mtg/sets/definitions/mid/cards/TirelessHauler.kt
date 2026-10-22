package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Tireless Hauler // Dire-Strain Brawler (Innistrad: Midnight Hunt)
 * {4}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front (4/5): Vigilance. Daybound.
 * Back (6/6): Vigilance. Nightbound.
 */
private val TirelessHaulerFront = card("Tireless Hauler") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 4
    toughness = 5
    oracleText = "Vigilance\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.VIGILANCE)

    daybound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Forrest Imel"
        flavorText = "The new hire proved more than enough to replace the three farmhands who disappeared the week before."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e96f9a6-c215-42b1-aa02-8e6143fe5bd7.jpg?1783925578"
    }
}

private val DireStrainBrawler = card("Dire-Strain Brawler") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 6
    toughness = 6
    oracleText = "Vigilance\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.VIGILANCE)

    nightbound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Forrest Imel"
        flavorText = "A few townsfolk had their suspicions, but none dared accuse him openly."
        imageUri = "https://cards.scryfall.io/normal/back/3/e/3e96f9a6-c215-42b1-aa02-8e6143fe5bd7.jpg?1783925578"
    }
}

val TirelessHauler: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = TirelessHaulerFront,
    backFace = DireStrainBrawler,
)
