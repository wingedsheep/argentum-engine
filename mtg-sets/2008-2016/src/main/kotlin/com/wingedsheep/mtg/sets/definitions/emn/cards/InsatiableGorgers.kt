package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Insatiable Gorgers (Eldritch Moon #134)
 * {2}{R}{R}
 * Creature — Vampire Berserker
 * 5/3
 *
 * This creature attacks each combat if able.
 * Madness {3}{R}
 */
val InsatiableGorgers = card("Insatiable Gorgers") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Berserker"
    oracleText = "This creature attacks each combat if able.\n" +
        "Madness {3}{R} (If you discard this card, discard it into exile. When you do, cast it for its " +
        "madness cost or put it into your graveyard.)"
    power = 5
    toughness = 3

    staticAbility {
        ability = MustAttack()
    }

    madness("{3}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Nils Hamm"
        flavorText = "\"Drink deeply, for there is no tomorrow!\"\n—Anje Falkenrath"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb56491b-bad7-44da-8aa5-91ffd875e76a.jpg"
    }
}
