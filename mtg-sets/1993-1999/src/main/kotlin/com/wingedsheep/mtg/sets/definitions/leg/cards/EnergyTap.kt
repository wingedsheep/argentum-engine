package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Energy Tap — Legends #54
 * {U} · Sorcery
 *
 * Tap target untapped creature you control. If you do, add an amount of {C} equal to that
 * creature's mana value.
 *
 * The target must still be an untapped creature you control on resolution, or the spell fizzles;
 * a legal target is therefore always untapped, so the tap always happens and the "if you do"
 * gate is always met. The mana is read off the (still-on-battlefield) target after tapping.
 */
val EnergyTap = card("Energy Tap") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap target untapped creature you control. If you do, add an amount of {C} equal to " +
        "that creature's mana value."

    spell {
        val creature = target(TargetFilter(GameObjectFilter.Creature.untapped().youControl()))
        effect = Effects.Tap(creature) then
            Effects.AddColorlessMana(DynamicAmounts.manaValueOf(creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37e69940-bdc8-48ff-a296-540343910adf.jpg?1783948076"
    }
}
