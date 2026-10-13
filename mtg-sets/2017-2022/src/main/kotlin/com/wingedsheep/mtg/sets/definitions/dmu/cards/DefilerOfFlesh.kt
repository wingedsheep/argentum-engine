package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val DefilerOfFlesh = card("Defiler of Flesh") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Phyrexian Horror"
    power = 4
    toughness = 4
    oracleText = "Menace\nAs an additional cost to cast black permanent spells, you may pay 2 life. Those spells cost {B} less to cast if you paid life this way. This effect reduces only the amount of black mana you pay.\nWhenever you cast a black permanent spell, target creature you control gets +1/+1 and gains menace until end of turn."
    keywords(Keyword.MENACE)
    staticAbility {
        ability = ModifySpellCost(
            SpellCostTarget.YouCast(GameObjectFilter.Permanent.withColor(Color.BLACK)),
            CostModification.ReduceColored("{B}"), optionalLifePayment = 2,
        )
    }
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Permanent.withColor(Color.BLACK))
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(1, 1, creature) then Effects.GrantKeyword(Keyword.MENACE, creature)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61a4dd11-6166-4a54-95c0-728c623d0fbc.jpg?1783921334"
        ruling("2022-09-09", "You may only pay the additional cost once per permanent spell.")
    }
}
