package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Acolyte's Reward
 * {1}{W}
 * Instant
 * Prevent the next X damage that would be dealt to target creature this turn, where X is your
 * devotion to white. If damage is prevented this way, Acolyte's Reward deals that much damage to
 * any target.
 *
 * An amount shield on the first target, sized by devotion as the spell resolves. Its reaction is part
 * of the prevention (no stack, before state-based actions): each damage event the shield prevents
 * damage from has Acolyte's Reward deal that much damage to the second target, chosen as the spell
 * was cast. A second target that has left the battlefield since takes nothing; the damage is still
 * prevented.
 */
val AcolytesReward = card("Acolyte's Reward") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent the next X damage that would be dealt to target creature this turn, where X is " +
        "your devotion to white. If damage is prevented this way, Acolyte's Reward deals that much damage " +
        "to any target. (Each {W} in the mana costs of permanents you control counts toward your devotion to white.)"

    spell {
        val creature = target(TargetFilter.Creature)
        val victim = target(Targets.Any)
        effect = Effects.PreventDamage(
            target = creature,
            amount = DynamicAmounts.devotionTo(Color.WHITE),
            onPrevented = Effects.DealDamage(DynamicAmounts.preventedDamage(), victim),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd8980af-c3fa-4027-917a-4a3d441c0a75.jpg?1783939586"
    }
}
