package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Flashfires
 * {3}{R}
 * Sorcery
 * Destroy all Plains.
 */
val Flashfires = card("Flashfires") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(
            GroupFilter(GameObjectFilter.Land.withSubtype(Subtype.PLAINS)),
            MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Dameon Willich"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee8a05a4-0ce3-4abe-bb60-08af53cf08e5.jpg?1559591304"
    }
}
