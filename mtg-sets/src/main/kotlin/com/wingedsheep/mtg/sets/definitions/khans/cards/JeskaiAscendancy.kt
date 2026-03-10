package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jeskai Ascendancy
 * {U}{R}{W}
 * Enchantment
 * Whenever you cast a noncreature spell, creatures you control get +1/+1 until end of turn.
 * Untap those creatures.
 * Whenever you cast a noncreature spell, you may draw a card. If you do, discard a card.
 */
val JeskaiAscendancy = card("Jeskai Ascendancy") {
    manaCost = "{U}{R}{W}"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a noncreature spell, creatures you control get +1/+1 until end of turn. Untap those creatures.\nWhenever you cast a noncreature spell, you may draw a card. If you do, discard a card."

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = CompositeEffect(
            listOf(
                ForEachInGroupEffect(
                    GroupFilter.AllCreaturesYouControl,
                    ModifyStatsEffect(1, 1, EffectTarget.Self)
                ),
                ForEachInGroupEffect(
                    GroupFilter.AllCreaturesYouControl,
                    TapUntapEffect(EffectTarget.Self, tap = false)
                )
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = MayEffect(EffectPatterns.loot())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca9c2522-5606-4bbd-863d-f0ab0a612b4e.jpg?1562793510"
    }
}
