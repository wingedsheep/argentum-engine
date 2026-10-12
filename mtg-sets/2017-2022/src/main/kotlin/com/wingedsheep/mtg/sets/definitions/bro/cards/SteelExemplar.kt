package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Steel Exemplar
 * {5}
 * Artifact Creature — Wizard
 * 4/4
 * Trample
 * This creature enters with two +1/+1 counters on it unless two or more colors of mana were spent
 * to cast it.
 *
 * "Unless two or more colors" is the self [EntersWithCounters] gated on fewer than two distinct
 * colors spent (Converge's [DynamicAmounts.colorsOfManaSpent] read against the entering creature's
 * own cast). A Steel Exemplar put onto the battlefield without being cast spent no mana, so it
 * gets the counters too.
 */
val SteelExemplar = card("Steel Exemplar") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Wizard"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "This creature enters with two +1/+1 counters on it unless two or more colors of mana were spent to cast it."

    keywords(Keyword.TRAMPLE)
    replacementEffect(
        EntersWithCounters(
            count = 2,
            selfOnly = true,
            condition = Conditions.CompareAmounts(DynamicAmounts.colorsOfManaSpent(), ComparisonOperator.LT, 2),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "246"
        artist = "Kekai Kotaki"
        flavorText = "Reports differed on whether the figure was an automaton or a mage, mostly because few who saw it survived."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/386c139a-13fc-49ca-923d-45faca4f9a2f.jpg?1783920013"
    }
}
