package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Daring Demolition — Aether Revolt #55
 * {2}{B}{B} · Sorcery
 *
 * Destroy target creature or Vehicle.
 *
 * The target is [GameObjectFilter.CreatureOrVehicle], as on Hellish Sideswipe, so an uncrewed
 * Vehicle is a legal target.
 */
val DaringDemolition = card("Daring Demolition") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature or Vehicle."

    spell {
        val creatureOrVehicle = target(TargetFilter(GameObjectFilter.CreatureOrVehicle))
        effect = Effects.Destroy(creatureOrVehicle)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Svetlin Velinov"
        flavorText = "Living so close to death gives the aetherborn an unusual perspective on risk."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6378898-50b7-47c9-8c25-dc660606be9f.jpg?1783936765"
    }
}
