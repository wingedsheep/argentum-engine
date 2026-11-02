package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ragefire — Ravnica Allegiance #270
 * {1}{R} · Sorcery
 *
 * A plain burn sorcery.
 */
val Ragefire = card("Ragefire") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Ragefire deals 3 damage to target creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "270"
        inBooster = false
        artist = "Randy Vargas"
        flavorText = "\"Your precious laws can't save you now!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb13420d-d295-4000-9c38-fa5d10e06ece.jpg"
    }
}
