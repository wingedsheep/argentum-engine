package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Harvesttide Infiltrator // Harvesttide Assailant (Innistrad: Midnight Hunt)
 * {2}{R}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Harvesttide Infiltrator (3/2): Trample. Daybound.
 * Back  — Harvesttide Assailant (4/4): Trample. Nightbound.
 */
private val HarvesttideInfiltratorFront = card("Harvesttide Infiltrator") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 3
    toughness = 2
    oracleText = "Trample\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.TRAMPLE)

    daybound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Mathias Kollros"
        flavorText = "He was one masked stranger among many. Blending in was child's play."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35fdb976-291c-4824-9518-dd8c9f93fcde.jpg?1783925603"
    }
}

private val HarvesttideAssailant = card("Harvesttide Assailant") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.TRAMPLE)

    nightbound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Mathias Kollros"
        flavorText = "When Tovolar's howl sounded from the woods, it was answered from the heart of the festival."
        imageUri = "https://cards.scryfall.io/normal/back/3/5/35fdb976-291c-4824-9518-dd8c9f93fcde.jpg?1783925603"
    }
}

val HarvesttideInfiltrator: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HarvesttideInfiltratorFront,
    backFace = HarvesttideAssailant,
)
