package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Temporal Trespass — Fate Reforged #55 (canonical printing)
 * {8}{U}{U}{U} · Sorcery
 *
 * Delve
 * Take an extra turn after this one. Exile Temporal Trespass.
 *
 * Temporal Mastery's body ([Effects.TakeExtraTurn] + `selfExile()`) behind Treasure Cruise's delve.
 */
val TemporalTrespass = card("Temporal Trespass") {
    manaCost = "{8}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\n" +
        "Take an extra turn after this one. Exile Temporal Trespass."

    keywords(Keyword.DELVE)

    spell {
        selfExile()
        effect = Effects.TakeExtraTurn()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "55"
        artist = "Clint Cearley"
        flavorText = "Few truly live in the moment."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a08b7a1e-b404-41ea-875f-b3468501cb4b.jpg?1783938701"
        ruling("2021-03-19", "Delve doesn't change a spell's mana cost or mana value. For example, Treasure Cruise's mana value is 8 even if you exiled three cards to cast it.")
        ruling("2021-03-19", "You can exile cards to pay only for generic mana, and you can't exile more cards than the generic mana requirement of a spell with delve. For example, you can't exile more than seven cards from your graveyard to cast Treasure Cruise unless an effect has increased its cost.")
    }
}
