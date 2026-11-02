package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Justice Strike
 * {R}{W}
 * Instant
 * Target creature deals damage to itself equal to its power.
 *
 * The target is both the damage source and the recipient, so its own abilities (lifelink,
 * deathtouch, infect) apply to the damage.
 */
val JusticeStrike = card("Justice Strike") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Target creature deals damage to itself equal to its power."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(
            DynamicAmounts.powerOf(creature),
            creature,
            damageSource = creature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Clint Cearley"
        flavorText = "\"Those who show no mercy to the weak deserve no mercy from the strong.\"\n—Firemane Kavrova"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27619ce9-59df-4f0e-a5db-2d32a530e547.jpg?1783934132"
    }
}
