package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Master's Rebuke
 * {1}{G}
 * Instant
 * Target creature you control deals damage equal to its power to target creature or planeswalker you don't control.
 */
val MastersRebuke = card("Master's Rebuke") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control deals damage equal to its power to target creature or planeswalker you don't control."
    spell {
        val source = target(TargetFilter.Creature.youControl())
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.DealDamage(DynamicAmounts.powerOf(source), victim, damageSource = source)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Francisco Miyara"
        flavorText = "\"You have always been a promising student, but your arrogance far outstrips your prowess.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d42ca7c-5b36-45a9-b235-4f90e66f4377.jpg?1783923843"
    }
}
