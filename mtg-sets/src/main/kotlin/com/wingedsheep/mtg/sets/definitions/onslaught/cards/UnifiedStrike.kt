package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

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
    oracleText = "Exile target attacking creature if its power is less than or equal to the number of Soldiers on the battlefield."

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
        imageUri = "https://cards.scryfall.io/large/front/2/9/29906eca-0823-4cd6-890f-e5b93cc50a11.jpg?1562904834"
    }
}
