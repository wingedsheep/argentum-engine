package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Stormbound Geist
 * {1}{U}{U}
 * Creature — Spirit
 * 2/2
 * Flying
 * This creature can block only creatures with flying.
 * Undying
 */
val StormboundGeist = card("Stormbound Geist") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "This creature can block only creatures with flying.\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"

    keywords(Keyword.FLYING, Keyword.UNDYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/040eddb0-fca2-41eb-ab07-c48d49385973.jpg"
    }
}
