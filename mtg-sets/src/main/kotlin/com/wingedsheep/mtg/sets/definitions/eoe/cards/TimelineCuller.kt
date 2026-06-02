package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.Costs

/**
 * Timeline Culler
 * {B}{B}
 * Creature — Drix Warlock
 * Haste
 * You may cast this card from your graveyard using its warp ability.
 * Warp—{B}, Pay 2 life. (You may cast this card from your hand or graveyard for its warp cost.
 *   If you do, exile this creature at the beginning of the next end step, then you may cast it
 *   from exile on a later turn.)
 * 2/2
 */
val TimelineCuller = card("Timeline Culler") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Drix Warlock"
    oracleText = "Haste\n" +
        "You may cast this card from your graveyard using its warp ability.\n" +
        "Warp—{B}, Pay 2 life. (You may cast this card from your hand or graveyard for its warp cost. " +
        "If you do, exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"
    power = 2
    toughness = 2

    keywords(Keyword.HASTE)

    // Warp—{B}, Pay 2 life. The `fromGraveyard = true` flag wires up the
    // "You may cast this card from your graveyard using its warp ability" clause.
    keywordAbility(
        KeywordAbility.Warp(
            cost = ManaCost.parse("{B}"),
            additionalCost = Costs.additional.PayLife(2),
            fromGraveyard = true,
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Alfonso Santano"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33410410-72f2-49c4-9e63-a72202cd075a.jpg?1752947042"
    }
}
