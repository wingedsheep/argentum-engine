package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets

val PirateShip = card("Pirate Ship") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 4
    toughness = 3
    oracleText = "This creature can't attack unless defending player controls an Island.\n{T}: This creature deals 1 damage to any target.\nWhen you control no Islands, sacrifice this creature."

    staticAbility {
        ability = CantAttackUnless(Conditions.DefendingPlayerControlsLandType("Island"))
    }

    activatedAbility {
        cost = Costs.Tap
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(1, victim)
    }

    stateTriggeredAbility {
        condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Island"), negate = true)
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you control no Islands, sacrifice this creature"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Tom Wänerstrand"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0a7cb23-d229-43c5-addd-dcf423984b0c.jpg?1783948703"
    }
}
