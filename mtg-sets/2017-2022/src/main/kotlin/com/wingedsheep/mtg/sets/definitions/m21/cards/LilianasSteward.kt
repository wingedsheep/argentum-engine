package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Liliana's Steward
 * {B}
 * Creature — Zombie
 * 1/2
 * {T}, Sacrifice this creature: Target opponent discards a card. Activate only as a sorcery.
 */
val LilianasSteward = card("Liliana's Steward") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 2
    oracleText = "{T}, Sacrifice this creature: Target opponent discards a card. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(1, opponent)
        description = "{T}, Sacrifice this creature: Target opponent discards a card. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Jason A. Engle"
        flavorText = "Servants at Vess Manor are chosen for their strong work ethic and respectful demeanor. " +
            "Being alive is not required."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1945fc78-8aa4-46fb-9571-eaa1c4729e3d.jpg?1783930705"
    }
}
