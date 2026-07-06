package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rumbling Rockslide — {3}{R}
 * Sorcery
 * Rumbling Rockslide deals damage to target creature equal to the number of lands you control.
 */
val RumblingRockslide = card("Rumbling Rockslide") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Rumbling Rockslide deals damage to target creature equal to the number of lands you control."

    spell {
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.DealDamage(
            amount = DynamicAmount.Count(
                player = Player.You,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Land,
            ),
            target = t,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Johann Bodin"
        flavorText = "Sometimes an expedition reaches a natural conclusion. Sometimes it all just comes crashing down."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f06b53f-ec82-4d7f-bee3-6ca04583f023.jpg?1782694478"
    }
}
