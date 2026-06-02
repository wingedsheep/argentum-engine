package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Dawnstrike Vanguard
 * {5}{W}
 * Creature — Human Knight
 * 4/5
 *
 * Lifelink
 * At the beginning of your end step, if you control two or more tapped creatures,
 * put a +1/+1 counter on each creature you control other than this creature.
 */
val DawnstrikeVanguard = card("Dawnstrike Vanguard") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 5
    oracleText = "Lifelink\n" +
        "At the beginning of your end step, if you control two or more tapped creatures, " +
        "put a +1/+1 counter on each creature you control other than this creature."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature.tapped()),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(2)
        )
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.OtherCreaturesYouControl,
            effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Arif Wijaya"
        flavorText = "\"Arrive as the dawn, and banish night through your brilliance!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a041722-9483-469f-9c17-7f0253b0db50.jpg?1752946591"

        ruling(
            "2025-07-25",
            "Dawnstrike Vanguard's last ability will check as your end step starts to see if you " +
                "control two or more tapped creatures. If you don't, the ability won't trigger at all. " +
                "You won't be able to tap anything during your end step in time to have the ability " +
                "trigger. If you don't control two or more tapped creatures when the ability resolves, " +
                "the ability won't do anything."
        )
    }
}
