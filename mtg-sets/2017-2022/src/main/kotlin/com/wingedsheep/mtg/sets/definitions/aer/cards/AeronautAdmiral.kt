package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Aeronaut Admiral — Aether Revolt #2
 * {3}{W} · Creature — Human Pilot · 3/1
 *
 * Flying
 * Vehicles you control have flying.
 *
 * "Vehicles you control have flying" is a Layer 6 grant over every Vehicle you control, not just
 * crewed ones — an uncrewed Vehicle is a noncreature artifact, hence a bare subtype filter rather
 * than `GameObjectFilter.Creature` (same shape as Mu Yanling, Wind Rider).
 */
val AeronautAdmiral = card("Aeronaut Admiral") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Pilot"
    power = 3
    toughness = 1
    oracleText = "Flying\nVehicles you control have flying."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantKeyword(
            Keyword.FLYING,
            GroupFilter(GameObjectFilter.Any.withSubtype(Subtype.VEHICLE).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "E. M. Gist"
        flavorText = "The Consulate recruits its best pilots from the League of Aeronauts. " +
            "Many go on to command entire fleets."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff15c06c-160d-4960-92d5-6f8e7b33f051.jpg?1783936785"
    }
}
