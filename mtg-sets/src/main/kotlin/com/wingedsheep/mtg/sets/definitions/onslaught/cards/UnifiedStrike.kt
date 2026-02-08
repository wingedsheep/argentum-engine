package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Unified Strike
 * {W}
 * Instant
 * Exile target attacking creature if its power is less than or equal to
 * the number of Soldiers on the battlefield.
 */
val UnifiedStrike = card("Unified Strike") {
    manaCost = "{W}"
    typeLine = "Instant"

    spell {
        target = Targets.AttackingCreature
        effect = ConditionalEffect(
            condition = Conditions.TargetPowerAtMost(
                DynamicAmounts.creaturesWithSubtype(Subtype("Soldier"))
            ),
            effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.EXILE)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Dave Dorman"
        flavorText = "\"Together we are more than the sum of our swords.\"\n—Aven Brigadier"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/457a6c06-e318-4fa3-bc89-53c6f1f1f435.jpg?1562910136"
    }
}
