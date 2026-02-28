package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gempalm Strider
 * {1}{G}
 * Creature — Elf
 * 2/2
 * Cycling {2}{G}{G}
 * When you cycle Gempalm Strider, Elf creatures get +2/+2 until end of turn.
 */
val GempalmStrider = card("Gempalm Strider") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf"
    oracleText = "Cycling {2}{G}{G}\nWhen you cycle Gempalm Strider, Elf creatures get +2/+2 until end of turn."
    power = 2
    toughness = 2

    keywordAbility(KeywordAbility.cycling("{2}{G}{G}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        effect = Effects.ModifyStatsForAll(2, 2, GroupFilter.allCreaturesWithSubtype("Elf"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f93d89f5-3e77-4dc0-935b-e6f6a3e968d2.jpg?1562945248"
    }
}
