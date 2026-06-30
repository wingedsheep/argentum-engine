package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Spin Out
 * {1}{B}{B}
 * Instant
 *
 * Destroy target creature or Vehicle.
 */
val SpinOut = card("Spin Out") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature or Vehicle."

    spell {
        val t = target(
            "creature or Vehicle",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle)),
        )
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Adrián Rodríguez Pérez"
        flavorText = "\"Good sportsmanship wins the crowd. Bad sportsmanship wins the race.\"\n—Winter"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be722ac5-e8c4-4180-aed0-7c28895afc0d.jpg?1782687877"
    }
}
