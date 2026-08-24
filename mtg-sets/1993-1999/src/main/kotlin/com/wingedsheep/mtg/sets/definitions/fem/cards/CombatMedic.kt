package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Combat Medic
 * {2}{W}
 * Creature — Human Cleric Soldier
 * 0/2
 * {1}{W}: Prevent the next 1 damage that would be dealt to any target this turn.
 */
val CombatMedic = card("Combat Medic") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric Soldier"
    oracleText = "{1}{W}: Prevent the next 1 damage that would be dealt to any target this turn."
    power = 0
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val t = target("any target", AnyTarget())
        effect = Effects.PreventNextDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1a"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"Although Icatia's Combat Medics borrowed much of their knowledge from other societies, their skills were their own.\"\n—*Sarpadian Empires, vol. VI*"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cfd96cb-03d6-4845-8595-50bf17b35726.jpg?1783947922"
    }
}
