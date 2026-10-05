package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val SedgeTroll = card("Sedge Troll") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Troll"
    oracleText = "This creature gets +1/+1 as long as you control a Swamp.\n{B}: Regenerate this creature."
    colorIdentity = "BR"
    power = 2
    toughness = 2
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype(Subtype.SWAMP)),
        )
    }
    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b13bf496-f3c0-4c13-8282-e7abfab6a198.jpg?1783948681"
        flavorText = "The stench in the hovel was overpowering; something loathsome was cooking. Occasionally something surfaced in the thick paste, but my host would casually push it down before I could quite make out what it was."
    }
}
