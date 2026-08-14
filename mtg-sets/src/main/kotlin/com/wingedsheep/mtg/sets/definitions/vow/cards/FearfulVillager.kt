package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Fearful Villager // Fearsome Werewolf (Innistrad: Crimson Vow)
 * {2}{R}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Fearful Villager (2/3): Menace; Daybound.
 * Back  — Fearsome Werewolf (4/3): Menace; Nightbound.
 *
 * The plainest daybound werewolf: both faces are a vanilla body plus [Keyword.MENACE], with the
 * day/night transform behavior riding entirely on [daybound]/[nightbound] (CR 702.145). The back is a
 * transformed face with no mana cost, so its color comes from a color indicator (CR 204):
 * `colorIndicator = "R"`.
 */

private val FearfulVillagerFront = card("Fearful Villager") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 3
    oracleText = "Menace\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.MENACE)
    daybound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5eb3a08e-1d31-4ab9-854f-a86b060696ec.jpg?1783924841"
    }
}

private val FearsomeWerewolf = card("Fearsome Werewolf") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 3
    oracleText = "Menace\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.MENACE)
    nightbound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/back/5/e/5eb3a08e-1d31-4ab9-854f-a86b060696ec.jpg?1783924841"
    }
}

val FearfulVillager: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = FearfulVillagerFront,
    backFace = FearsomeWerewolf,
)
