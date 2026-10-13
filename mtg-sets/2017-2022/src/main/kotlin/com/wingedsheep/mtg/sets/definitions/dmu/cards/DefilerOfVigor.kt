package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
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

val DefilerOfVigor = card("Defiler of Vigor") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Phyrexian Wurm"
    power = 6
    toughness = 6
    oracleText = "Trample\nAs an additional cost to cast green permanent spells, you may pay 2 life. Those spells cost {G} less to cast if you paid life this way. This effect reduces only the amount of green mana you pay.\nWhenever you cast a green permanent spell, put a +1/+1 counter on each creature you control."
    keywords(Keyword.TRAMPLE)
    staticAbility {
        ability = ModifySpellCost(
            SpellCostTarget.YouCast(GameObjectFilter.Permanent.withColor(Color.GREEN)),
            CostModification.ReduceColored("{G}"), optionalLifePayment = 2,
        )
    }
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Permanent.withColor(Color.GREEN))
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0896860c-d028-4193-89a5-97e05c12a1e7.jpg?1783921304"
        ruling("2022-09-09", "You may only pay the additional cost once per permanent spell.")
    }
}
