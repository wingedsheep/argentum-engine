package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

val PhantasmalForces = card("Phantasmal Forces") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Illusion"
    oracleText = "Flying\nAt the beginning of your upkeep, sacrifice this creature unless you pay {U}."
    colorIdentity = "U"
    power = 4
    toughness = 1
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(cost = Costs.pay.Mana("{U}"), suffer = SacrificeSelfEffect)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0631c7c8-9aa5-4333-8e20-20247fc47033.jpg?1783948703"
        flavorText = "These beings embody the essence of true heroes long dead. Summoned from the dreamrealms, they rise to meet their enemies."
    }
}
