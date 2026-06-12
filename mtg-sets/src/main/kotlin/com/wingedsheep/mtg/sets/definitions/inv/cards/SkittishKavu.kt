package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Skittish Kavu
 * {1}{R}
 * Creature — Kavu
 * 1/1
 * This creature gets +1/+1 as long as no opponent controls a white or blue creature.
 */
val SkittishKavu = card("Skittish Kavu") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kavu"
    power = 1
    toughness = 1
    oracleText = "This creature gets +1/+1 as long as no opponent controls a white or blue creature."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Conditions.Not(
                Exists(
                    Player.EachOpponent,
                    Zone.BATTLEFIELD,
                    GameObjectFilter.Creature.withAnyColor(Color.WHITE, Color.BLUE)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be806378-50a7-4416-9d99-1ea2c1f2b7cb.jpg?1562933310"
    }
}
