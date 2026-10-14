package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val TatoFarmer = card("Tato Farmer") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Zombie Mutant Peasant"
    power = 1
    toughness = 4
    oracleText = "Landfall — Whenever a land you control enters, you may get two rad counters.\n" +
        "{T}: Put target land card in a graveyard that was milled this turn onto the battlefield tapped under your control."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        optional = true
        effect = Effects.AddCounters(CounterType.RAD, 2, EffectTarget.Controller)
    }

    activatedAbility {
        cost = Costs.Tap
        val land = target(TargetFilter(GameObjectFilter.Land.milledThisTurn(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOntoBattlefieldFromGraveyard(land, underYourControl = true, tapped = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Xavier Ribeiro"
        flavorText = "\"Try one! They're disgusting!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/907bade4-7232-4808-af5c-2dbed4d5bf07.jpg?1783912395"
    }
}
