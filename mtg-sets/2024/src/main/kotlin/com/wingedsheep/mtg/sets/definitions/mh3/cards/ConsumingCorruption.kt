package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Consuming Corruption
 * {B}{B}
 * Instant
 * Consuming Corruption deals X damage to target creature or planeswalker and you gain X life,
 * where X is the number of Swamps you control.
 */
val ConsumingCorruption = card("Consuming Corruption") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Consuming Corruption deals X damage to target creature or planeswalker and you gain X life, " +
        "where X is the number of Swamps you control."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        val swamps = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype(Subtype.SWAMP)).count()
        effect = Effects.DealDamage(swamps, t) then Effects.GainLife(swamps)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Andrew Theophilopoulos"
        flavorText = "While crossing the mire, Lort the Sneaky found the perfect shortcut to the end of his life."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9068b3a4-9130-42d8-a26b-52010b7daa8b.jpg?1783911283"
    }
}
