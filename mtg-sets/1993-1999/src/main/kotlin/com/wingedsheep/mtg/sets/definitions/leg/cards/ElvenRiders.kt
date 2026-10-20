package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Elven Riders
 * {3}{G}{G}
 * Creature — Elf
 * 3/3
 * This creature can't be blocked except by Walls and/or creatures with flying.
 *
 * Reach does not let a creature block it (Scryfall ruling): the blocker must actually be a
 * Wall or have flying.
 */
val ElvenRiders = card("Elven Riders") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf"
    power = 3
    toughness = 3
    oracleText = "This creature can't be blocked except by Walls and/or creatures with flying."

    staticAbility {
        ability = CantBeBlockedExceptBy(
            GameObjectFilter.Creature.withSubtype(Subtype.WALL) or
                GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Melissa A. Benson"
        flavorText = "\"Sometimes it is better to be swift of foot than strong of swordarm.\"\n—Elven Proverb"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad1d349b-b5ab-4b2b-9b39-f8d8f6374aa5.jpg?1783948048"
    }
}
