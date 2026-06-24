package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Self-Destruct
 * {1}{R}
 * Instant
 *
 * Target creature you control deals X damage to any other target and X damage to itself, where X
 * is its power.
 *
 * The controlled creature (target index 0) is the damage source for both prongs, so X is read off
 * its power as last-known information while the damage is dealt. The "any other target" is target
 * index 1 (any target — creature, player, planeswalker, or battle).
 */
val SelfDestruct = card("Self-Destruct") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control deals X damage to any other target and X damage to itself, where X is its power."

    spell {
        val yourCreature = target("creature you control", Targets.CreatureYouControl)
        val other = target("any other target", TargetOther(baseRequirement = AnyTarget()))
        effect = Effects.DealDamage(
            DynamicAmounts.targetPower(0),
            other,
            damageSource = yourCreature
        ) then Effects.DealDamage(
            DynamicAmounts.targetPower(0),
            yourCreature,
            damageSource = yourCreature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Liiga Smilshkalne"
        flavorText = "\"Hah! If I'd just left you in the lurch, I'd look like a jerk for all of history!\"\n—Gilgamesh"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7661003c-bf83-46bb-bcc0-8fbf5819ffa8.jpg?1748706346"
    }
}
