package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Village Watch // Village Reavers (Innistrad: Midnight Hunt)
 * {4}{R}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front (4/3): Haste. Daybound.
 * Back (5/4): Wolves and Werewolves you control have haste. Nightbound.
 */
private val VillageWatchFront = card("Village Watch") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 4
    toughness = 3
    oracleText = "Haste\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.HASTE)

    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"You have my word. You'll see no wolf attacks while we're around!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bf48d2b-eb68-4f47-a80a-4751a4fa20a7.jpg?1783925593"
    }
}

private val VillageReavers = card("Village Reavers") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 5
    toughness = 4
    oracleText = "Wolves and Werewolves you control have haste.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(GameObjectFilter.Creature.withAnySubtype("Wolf", "Werewolf").youControl()),
        )
    }

    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Néstor Ossandón Leal"
        flavorText = "With no guards to be seen, the cobbled streets of Gatstaf echoed with howls."
        imageUri = "https://cards.scryfall.io/normal/back/1/b/1bf48d2b-eb68-4f47-a80a-4751a4fa20a7.jpg?1783925593"
    }
}

val VillageWatch: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = VillageWatchFront,
    backFace = VillageReavers,
)
