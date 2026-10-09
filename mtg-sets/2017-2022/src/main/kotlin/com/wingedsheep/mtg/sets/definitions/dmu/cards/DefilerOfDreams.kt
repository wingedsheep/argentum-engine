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

val DefilerOfDreams = card("Defiler of Dreams") {
    manaCost = "{3}{U}{U}"
    typeLine = "Creature — Phyrexian Sphinx"
    power = 4
    toughness = 3
    oracleText = "Flying\nAs an additional cost to cast blue permanent spells, you may pay 2 life. Those spells cost {U} less to cast if you paid life this way. This effect reduces only the amount of blue mana you pay.\nWhenever you cast a blue permanent spell, draw a card."
    keywords(Keyword.FLYING)
    staticAbility {
        ability = ModifySpellCost(
            SpellCostTarget.YouCast(GameObjectFilter.Permanent.withColor(Color.BLUE)),
            CostModification.ReduceColored("{U}"), optionalLifePayment = 2,
        )
    }
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Permanent.withColor(Color.BLUE))
        effect = Effects.DrawCards(1)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/881cc4a3-252b-4155-a34f-63d4b4f44442.jpg?1783921353"
        ruling("2022-09-09", "You may only pay the additional cost once per permanent spell.")
    }
}
