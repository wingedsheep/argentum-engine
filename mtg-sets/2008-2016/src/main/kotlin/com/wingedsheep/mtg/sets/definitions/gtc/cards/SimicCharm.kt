package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Simic Charm
 * {G}{U}
 * Instant
 * Choose one —
 * • Target creature gets +3/+3 until end of turn.
 * • Permanents you control gain hexproof until end of turn.
 * • Return target creature to its owner's hand.
 */
val SimicCharm = card("Simic Charm") {
    manaCost = "{G}{U}"
    colorIdentity = "UG"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature gets +3/+3 until end of turn.\n" +
        "• Permanents you control gain hexproof until end of turn.\n" +
        "• Return target creature to its owner's hand."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature gets +3/+3 until end of turn") {
                val t = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(3, 3, t)
            }
            mode("Permanents you control gain hexproof until end of turn") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Permanent.youControl()),
                    Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity)
                )
            }
            mode("Return target creature to its owner's hand") {
                val t = target(TargetFilter.Creature)
                effect = Effects.ReturnToHand(t)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Zoltan Boros"
        flavorText = "\"We measure our progress in heartbeats.\"\n—Prime Speaker Zegana"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97c27bdd-77f5-4e93-8f54-93a204fc980a.jpg"
    }
}
