package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inner Struggle (SOI #167)
 * {3}{R}
 * Instant
 * Target creature deals damage to itself equal to its power.
 *
 * Kiku, Night's Flower's shape: the target is both the damage source and the recipient, so its own
 * abilities (lifelink, deathtouch, infect) apply to the damage. Power is read on resolution.
 */
val InnerStruggle = card("Inner Struggle") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
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
        collectorNumber = "167"
        artist = "Mathias Kollros"
        flavorText = "\"Reflect upon your evils lest they reflect upon you.\"\n—Inquisitor Laerin"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b43be50a-8624-46f6-860a-39ffbfbdaa6a.jpg?1783937748"
    }
}
