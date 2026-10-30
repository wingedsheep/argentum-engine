package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Suffocating Fumes — Ikoria: Lair of Behemoths #100
 * {2}{B} · Instant
 *
 * Creatures your opponents control get -1/-1 until end of turn.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 *
 * The mass debuff is [Patterns.Group.modifyStatsForAll] over the opponents' creatures at resolution,
 * so creatures entering later are unaffected.
 */
val SuffocatingFumes = card("Suffocating Fumes") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Creatures your opponents control get -1/-1 until end of turn.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        effect = Patterns.Group.modifyStatsForAll(-1, -1, GroupFilter.AllCreaturesOpponentsControl)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"Yesterday it was flying tigers and crumbling bridges. Who knows what today will bring?\"\n" +
            "—Dol, Lavabrink quartermaster"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66b562e4-35df-4aee-848d-ceb4204bbe58.jpg"
    }
}
