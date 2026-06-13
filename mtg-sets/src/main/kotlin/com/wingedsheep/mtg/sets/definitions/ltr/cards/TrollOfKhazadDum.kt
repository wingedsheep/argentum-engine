package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Troll of Khazad-dûm
 * {5}{B}
 * Creature — Troll
 * 6/5
 *
 * This creature can't be blocked except by three or more creatures.
 * Swampcycling {1}
 *
 * The block restriction uses the new `CantBeBlockedByFewerThan(3)` static (a generalization of
 * menace's minimum-2). Swampcycling composes via `KeywordAbility.typecycling("Swamp", "{1}")`.
 */
val TrollOfKhazadDum = card("Troll of Khazad-dûm") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Troll"
    power = 6
    toughness = 5
    oracleText = "This creature can't be blocked except by three or more creatures.\n" +
        "Swampcycling {1} ({1}, Discard this card: Search your library for a Swamp card, reveal it, " +
        "put it into your hand, then shuffle.)"

    staticAbility {
        ability = CantBeBlockedByFewerThan(3)
    }

    keywordAbility(KeywordAbility.typecycling("Swamp", "{1}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Simon Dominic"
        flavorText = "\"A great cave-troll, I think. There is no hope of escape that way.\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6539e26-b63b-4725-9407-caaf451de084.jpg?1743419034"
    }
}
