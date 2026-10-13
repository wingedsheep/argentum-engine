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

val DefilerOfFaith = card("Defiler of Faith") {
    manaCost = "{3}{W}{W}"
    typeLine = "Creature — Phyrexian Human"
    power = 5
    toughness = 5
    oracleText = "Vigilance\nAs an additional cost to cast white permanent spells, you may pay 2 life. Those spells cost {W} less to cast if you paid life this way. This effect reduces only the amount of white mana you pay.\nWhenever you cast a white permanent spell, create a 1/1 white Soldier creature token."
    keywords(Keyword.VIGILANCE)
    staticAbility {
        ability = ModifySpellCost(
            SpellCostTarget.YouCast(GameObjectFilter.Permanent.withColor(Color.WHITE)),
            CostModification.ReduceColored("{W}"), optionalLifePayment = 2,
        )
    }
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Permanent.withColor(Color.WHITE))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cfe404d-1a27-4627-9284-56188d92d5d5.jpg?1783921366"
        ruling("2022-09-09", "You may only pay the additional cost once per permanent spell.")
    }
}
