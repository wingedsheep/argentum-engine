package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Genesis Effect
 * {4}{R}
 * Instant
 *
 * Genesis Effect deals 6 damage to target creature. Destroy target land you control. Create a
 * tapped Planet land token named New Planet with "{T}: Add one mana of any color."
 */
val GenesisEffect = card("Genesis Effect") {
    manaCost = "{4}{R}"
    typeLine = "Instant"
    oracleText = "Genesis Effect deals 6 damage to target creature. Destroy target land you control. " +
        "Create a tapped Planet land token named New Planet with \"{T}: Add one mana of any color.\""

    spell {
        val creature = target(TargetFilter.Creature)
        val land = target(TargetFilter.LandYouControl)
        effect = Effects.DealDamage(6, creature) then
            Effects.Destroy(land) then
            Effects.CreateNewPlanet()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Anthony Devine"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0996bab-c2e4-4483-a76e-1355c81f1577.jpg?1793361330"
    }
}
