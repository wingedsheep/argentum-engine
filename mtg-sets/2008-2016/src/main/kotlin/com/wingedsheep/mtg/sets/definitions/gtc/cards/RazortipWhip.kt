package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Razortip Whip
 * {2}
 * Artifact
 * {1}, {T}: This artifact deals 1 damage to target opponent or planeswalker.
 */
val RazortipWhip = card("Razortip Whip") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}: This artifact deals 1 damage to target opponent or planeswalker."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val victim = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "235"
        artist = "James Paick"
        flavorText = "\"It no longer surprises me what shows up in our raid of a Rakdos revue.\"\n—Janik Bara, Wojek captain"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24619d3f-1051-4d8e-9c8d-70a12621b282.jpg"
    }
}
