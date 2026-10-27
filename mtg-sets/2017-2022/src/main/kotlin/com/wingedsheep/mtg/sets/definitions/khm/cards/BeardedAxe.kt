package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bearded Axe
 * {2}{R}
 * Artifact — Equipment
 * Equipped creature gets +1/+1 for each Dwarf, Equipment, and/or Vehicle you control.
 * Equip {2}
 *
 * The count is one permanent-count over a subtype union ("and/or" — a Dwarf Vehicle counts once),
 * re-evaluated continuously. The Axe is itself an Equipment, so it always counts itself.
 */
private val dwarvesEquipmentAndVehiclesYouControl =
    DynamicAmounts.battlefield(
        Player.You,
        GameObjectFilter.Any.withAnySubtype("Dwarf", "Equipment", "Vehicle"),
    ).count()

val BeardedAxe = card("Bearded Axe") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each Dwarf, Equipment, and/or Vehicle you control.\n" +
        "Equip {2}"

    staticAbility {
        ability = GrantDynamicStats(
            filter = Filters.EquippedCreature,
            powerBonus = dwarvesEquipmentAndVehiclesYouControl,
            toughnessBonus = dwarvesEquipmentAndVehiclesYouControl,
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "388"
        artist = "Ovidio Cartagena"
        flavorText = "Every dwarven artisan spends the first hundred years of their life forging a single weapon and infusing it with runic magic."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2381dbab-e968-4bbc-b3a7-a1e55b066039.jpg?1783928116"
        ruling("2021-02-05", "The bonus granted by Bearded Axe is constantly updated as the number of Dwarves, Equipment, and Vehicles you control changes.")
        ruling("2021-02-05", "Bearded Axe will count itself, so it will give at least +1/+1 in most cases.")
    }
}
