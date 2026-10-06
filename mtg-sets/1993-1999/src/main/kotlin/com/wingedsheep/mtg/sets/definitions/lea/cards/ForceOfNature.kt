package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Force of Nature
 * {2}{G}{G}{G}{G}
 * Creature — Elemental
 * 8/8
 * Trample
 * At the beginning of your upkeep, this creature deals 8 damage to you unless you pay {G}{G}{G}{G}.
 */
val ForceOfNature = card("Force of Nature") {
    manaCost = "{2}{G}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 8
    toughness = 8
    oracleText = "Trample\n" +
        "At the beginning of your upkeep, this creature deals 8 damage to you unless you pay {G}{G}{G}{G}."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana(ManaCost.parse("{G}{G}{G}{G}")),
            suffer = Effects.DealDamage(8, EffectTarget.Controller),
        )
        description = "At the beginning of your upkeep, this creature deals 8 damage to you unless you pay {G}{G}{G}{G}."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21551cb6-3a53-42dd-9bbd-4bc56304d6d3.jpg?1783948677"
    }
}
