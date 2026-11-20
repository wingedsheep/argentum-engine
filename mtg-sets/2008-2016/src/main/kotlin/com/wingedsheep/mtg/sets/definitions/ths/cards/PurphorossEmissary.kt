package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Purphoros's Emissary — a 3/3 menace body; bestowed, the Aura's statics give the host +3/+3 and
 * menace (Observant Alseid shape).
 */
val PurphorossEmissary = card("Purphoros's Emissary") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Ox"
    power = 3
    toughness = 3
    oracleText = "Bestow {6}{R} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Enchanted creature gets +3/+3 and has menace."

    keywordAbility(KeywordAbility.bestow("{6}{R}"))
    keywords(Keyword.MENACE)

    staticAbility { ability = ModifyStats(3, 3) }
    staticAbility { ability = GrantKeyword(Keyword.MENACE) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d4ddc30-844c-4860-b394-d3c807c31162.jpg?1783939756"
    }
}
