package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Ouroboroid
 * {2}{G}{G}
 * Creature — Plant Wurm
 * 1/3
 * At the beginning of combat on your turn, put X +1/+1 counters on each creature you control, where X is this creature's power.
 */
val Ouroboroid = card("Ouroboroid") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Wurm"
    power = 1
    toughness = 3
    oracleText = "At the beginning of combat on your turn, put X +1/+1 counters on each creature you control, where X is this creature's power."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        // Snapshot source power before iteration so counter gains on Ouroboroid don't
        // increase X for the remaining creatures mid-loop.
        effect = Effects.Composite(
            Effects.StoreNumber("ouroboroid_power", DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power)),
            Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = Effects.AddDynamicCounters(
                    counterType = "+1/+1",
                    amount = DynamicAmount.VariableReference("ouroboroid_power"),
                    target = EffectTarget.Self
                )
            )
        )
        description = "At the beginning of combat on your turn, put X +1/+1 counters on each creature you control, where X is this creature's power."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "201"
        artist = "Samuel Perin"
        flavorText = "\"I've found a strange vine in the ice. Thawing for further analysis.\"\n—Meltstrider log, final entry"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/209c591a-4ab2-4e89-9523-a7b766cf4e51.jpg?1752947376"
    }
}
