package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tattered Haunter (Eldritch Moon #77)
 * {1}{U}
 * Creature — Spirit
 * 2/1
 *
 * Flying
 * This creature can block only creatures with flying.
 *
 * Vaporkin shape: the restriction is printed on the creature itself, so
 * [CanOnlyBlockCreaturesWith]'s default `filter` (the source) is the right one. The engine reads it
 * through `CanOnlyBlockCreaturesWithRule` in the block-legality rules.
 */
val TatteredHaunter = card("Tattered Haunter") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "Flying\nThis creature can block only creatures with flying."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Nils Hamm"
        flavorText = "When the Nebelgast recedes, there are often stragglers."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd8ac8da-7497-4b07-af86-29bac1361c8e.jpg"
    }
}
