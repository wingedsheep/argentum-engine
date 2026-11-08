package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hinterland Drake
 * {2}{U}
 * Creature — Drake
 * 2/3
 * Flying
 * This creature can't block artifact creatures.
 *
 * Expressed as its complement — the creatures it *can* block are exactly the nonartifact ones,
 * which is what `CanOnlyBlockCreaturesWith` takes. The check reads projected types, so an attacker
 * that becomes (or stops being) an artifact mid-game is judged by what it is when blocks are declared.
 */
val HinterlandDrake = card("Hinterland Drake") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    oracleText = "Flying\nThis creature can't block artifact creatures."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.nonartifact())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Lake Hurwitz"
        flavorText = "\"As a kid, I dreamed of flying on those wild drakes. They inspired me to build wings of my own.\"\n—Nadja Cloudtoucher, aerowright"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/418448d0-3e8d-4581-b598-696165775d23.jpg?1783936774"
    }
}
