package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oath of Ajani
 * {G}{W}
 * Legendary Enchantment
 * When Oath of Ajani enters, put a +1/+1 counter on each creature you control.
 * Planeswalker spells you cast cost {1} less to cast.
 */
val OathOfAjani = card("Oath of Ajani") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Enchantment"
    oracleText = "When Oath of Ajani enters, put a +1/+1 counter on each creature you control.\n" +
        "Planeswalker spells you cast cost {1} less to cast."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                count = 1,
                target = EffectTarget.IterationEntity
            )
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Planeswalker),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Wesley Burt"
        flavorText = "\"Until all have found their place, I will keep watch.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/968c7d5a-b037-4ff7-b6d2-df00adf5691b.jpg"
    }
}
