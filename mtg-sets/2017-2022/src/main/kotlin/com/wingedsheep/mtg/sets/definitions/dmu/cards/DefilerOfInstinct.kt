package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

val DefilerOfInstinct = card("Defiler of Instinct") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Phyrexian Kavu"
    power = 4
    toughness = 4
    oracleText = "First strike\nAs an additional cost to cast red permanent spells, you may pay 2 life. Those spells cost {R} less to cast if you paid life this way. This effect reduces only the amount of red mana you pay.\nWhenever you cast a red permanent spell, this creature deals 1 damage to any target."
    keywords(Keyword.FIRST_STRIKE)
    staticAbility {
        ability = ModifySpellCost(
            SpellCostTarget.YouCast(GameObjectFilter.Permanent.withColor(Color.RED)),
            CostModification.ReduceColored("{R}"), optionalLifePayment = 2,
        )
    }
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Permanent.withColor(Color.RED))
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(1, victim)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e76e0cc-caff-4b15-b550-39e2324c8c95.jpg?1783921320"
        ruling("2022-09-09", "You may only pay the additional cost once per permanent spell.")
    }
}
