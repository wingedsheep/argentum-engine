package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.dsl.Effects

/**
 * Valorous Charge
 * {1}{W}{W}
 * Sorcery
 * White creatures get +2/+0 until end of turn.
 */
val ValorousCharge = card("Valorous Charge") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withColor(Color.WHITE)),
            ModifyStatsEffect(2, 0, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Douglas Shuler"
        flavorText = "\"Stand in the way of truth at your peril.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67f61bdf-cbcd-4a63-8866-eb13ec9b351c.jpg"
    }
}
