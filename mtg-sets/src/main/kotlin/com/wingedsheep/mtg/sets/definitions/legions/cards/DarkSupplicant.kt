package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Dark Supplicant
 * {B}
 * Creature — Human Cleric
 * 1/1
 * {T}, Sacrifice three Clerics: Search your graveyard, hand, and/or library for a card
 * named Scion of Darkness and put it onto the battlefield. If you search your library
 * this way, shuffle.
 */
val DarkSupplicant = card("Dark Supplicant") {
    manaCost = "{B}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "{T}, Sacrifice three Clerics: Search your graveyard, hand, and/or library for a card named Scion of Darkness and put it onto the battlefield. If you search your library this way, shuffle."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeMultiple(3, GameObjectFilter.Creature.withSubtype("Cleric"))
        )
        effect = Effects.SearchMultipleZones(
            zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
            filter = GameObjectFilter.Any.named("Scion of Darkness"),
            count = 1,
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Mark Zug"
        flavorText = "Cabal clerics serve two masters: their patriarch and their god."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb685932-5df5-4f26-9633-b1daa8925359.jpg?1562942279"
        ruling("2004-10-04", "You can choose which and how many of the three zones (graveyard, hand, and library) you want to search.")
        ruling("2004-10-04", "You only shuffle your library if you choose to search it.")
    }
}
