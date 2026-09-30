package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val SeaSerpent = card("Sea Serpent") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    power = 5
    toughness = 5
    oracleText = "This creature can't attack unless defending player controls an Island.\nWhen you control no Islands, sacrifice this creature."

    staticAbility {
        ability = CantAttackUnless(Conditions.DefendingPlayerControlsLandType("Island"))
    }

    stateTriggeredAbility {
        condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Island"), negate = true)
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you control no Islands, sacrifice this creature"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0b333b7-db4d-4439-b0de-60414cbf8d7b.jpg?1783948702"
        flavorText = "Legend has it that Serpents used to be bigger, but how could that be?"
    }
}
