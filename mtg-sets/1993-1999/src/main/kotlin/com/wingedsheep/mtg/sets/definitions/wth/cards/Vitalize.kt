package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vitalize
 * {G}
 * Instant
 * Untap all creatures you control.
 */
val Vitalize = card("Vitalize") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Untap all creatures you control."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Untap(EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Pete Venters"
        flavorText = "\"After waking to the magic of Llanowar, no dreams can compare.\"\n—Mirri of the *Weatherlight*"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6ee4997-4b1a-4e03-88ac-63b451bb7b38.jpg?1783946715"
    }
}
