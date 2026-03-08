package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetOpponentOrPlaneswalker

/**
 * Jeskai Charm
 * {U}{R}{W}
 * Instant
 * Choose one —
 * • Put target creature on top of its owner's library.
 * • Jeskai Charm deals 4 damage to target opponent or planeswalker.
 * • Creatures you control get +1/+1 and gain lifelink until end of turn.
 */
val JeskaiCharm = card("Jeskai Charm") {
    manaCost = "{U}{R}{W}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Put target creature on top of its owner's library.\n• Jeskai Charm deals 4 damage to target opponent or planeswalker.\n• Creatures you control get +1/+1 and gain lifelink until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Put target creature on top of its owner's library") {
                val creature = target("target creature", TargetCreature())
                effect = Effects.PutOnTopOfLibrary(creature)
            }
            mode("Jeskai Charm deals 4 damage to target opponent or planeswalker") {
                val t = target("target opponent or planeswalker", TargetOpponentOrPlaneswalker())
                effect = Effects.DealDamage(4, t)
            }
            mode("Creatures you control get +1/+1 and gain lifelink until end of turn") {
                effect = EffectPatterns.modifyStatsForAll(
                    power = 1,
                    toughness = 1,
                    filter = GroupFilter.AllCreaturesYouControl
                ).then(
                    EffectPatterns.grantKeywordToAll(
                        keyword = Keyword.LIFELINK,
                        filter = GroupFilter.AllCreaturesYouControl
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca268705-ef04-4bf1-8a5d-866bb3e5bb61.jpg?1562793488"
    }
}
