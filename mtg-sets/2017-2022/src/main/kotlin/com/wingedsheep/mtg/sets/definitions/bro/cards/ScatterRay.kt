package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scatter Ray
 * {1}{U}
 * Instant
 * Counter target artifact or creature spell unless its controller pays {4}.
 *
 * Miscalculation's tax counter ([Effects.CounterUnlessPays]) narrowed to artifact or creature
 * spells on the stack, the same target Voracious Greatshark uses.
 */
val ScatterRay = card("Scatter Ray") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target artifact or creature spell unless its controller pays {4}."

    spell {
        target(TargetFilter(GameObjectFilter.CreatureOrArtifact, zone = Zone.STACK))
        effect = Effects.CounterUnlessPays("{4}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Awanqi (Angela Wang)"
        flavorText = "Mishra's army demanded the secrets of the Third Path. Hurkyl's followers graciously showed them."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c2852c9-d566-4824-836a-2f4c7a5148a1.jpg?1783920109"
    }
}
