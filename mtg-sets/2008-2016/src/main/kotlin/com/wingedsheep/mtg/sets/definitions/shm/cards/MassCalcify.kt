package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mass Calcify
 * {5}{W}{W}
 * Sorcery
 *
 * Destroy all nonwhite creatures.
 *
 * - [Effects.DestroyAll] lowers to the gather-then-move-collection pipeline, so indestructible
 *   and regeneration are honoured per permanent rather than short-circuiting the whole sweep.
 * - `notColor(WHITE)` reads the creature's *current* colours off projected state, so a
 *   multicoloured creature that is partly white survives, and a creature turned white by a
 *   continuous effect survives too.
 */
val MassCalcify = card("Mass Calcify") {
    manaCost = "{5}{W}{W}"
    typeLine = "Sorcery"
    oracleText = "Destroy all nonwhite creatures."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.notColor(Color.WHITE))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Brandon Kitkouski"
        flavorText = "The dead serve as their own tombstones."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d24be94-9922-43bb-83c8-98090adc3f32.jpg?1783942768"
    }
}
