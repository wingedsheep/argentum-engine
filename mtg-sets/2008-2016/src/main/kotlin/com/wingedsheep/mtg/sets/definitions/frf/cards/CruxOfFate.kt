package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Crux of Fate {3}{B}{B}
 * Sorcery
 *
 * Choose one —
 * • Destroy all Dragon creatures.
 * • Destroy all non-Dragon creatures.
 *
 * Two [Effects.DestroyAll] modes over the projected Dragon subtype, so a changeling counts as a
 * Dragon for both modes.
 */
val CruxOfFate = card("Crux of Fate") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Destroy all Dragon creatures.\n" +
        "• Destroy all non-Dragon creatures."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy all Dragon creatures") {
                effect = Effects.DestroyAll(GameObjectFilter.Creature.withSubtype(Subtype.DRAGON))
            }
            mode("Destroy all non-Dragon creatures") {
                effect = Effects.DestroyAll(GameObjectFilter.Creature.notSubtype(Subtype.DRAGON))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Michael Komarck"
        flavorText = "Ugin's whispered summons led Sarkhan Vol to the moment that would echo down the centuries and seal Tarkir's fate: the primal battle between Ugin and Nicol Bolas."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1d45374-a41b-4b3f-a7c8-3eb5ca767cf6.jpg?1783938698"
    }
}
