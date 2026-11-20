package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Spearpoint Oread — a 2/2 first strike body; bestowed, the Aura's statics give the host +2/+2 and
 * first strike (Observant Alseid shape).
 */
val SpearpointOread = card("Spearpoint Oread") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Nymph"
    power = 2
    toughness = 2
    oracleText = "Bestow {5}{R} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "First strike\n" +
        "Enchanted creature gets +2/+2 and has first strike."

    keywordAbility(KeywordAbility.bestow("{5}{R}"))
    keywords(Keyword.FIRST_STRIKE)

    staticAbility { ability = ModifyStats(2, 2) }
    staticAbility { ability = GrantKeyword(Keyword.FIRST_STRIKE) }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22d9d241-74b8-4b40-b4b7-b8584012d482.jpg?1783939753"
    }
}
