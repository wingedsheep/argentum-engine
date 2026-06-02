package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Mechan Assembler
 * {4}{U}
 * Artifact Creature — Robot Artificer
 * Whenever another artifact you control enters, create a 2/2 colorless Robot artifact creature token. This ability triggers only once each turn.
 * 4/4
 */
val MechanAssembler = card("Mechan Assembler") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot Artificer"
    power = 4
    toughness = 4
    oracleText = "Whenever another artifact you control enters, create a 2/2 colorless Robot artifact creature token. This ability triggers only once each turn."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Artifact
                    .youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        oncePerTurn = true
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(), // colorless
            creatureTypes = setOf("Robot"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
        )
        description = "Whenever another artifact you control enters, create a 2/2 colorless Robot artifact creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Mirko Failoni"
        flavorText = "Every iteration brings it closer to understanding its own creation."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fd46726-095e-4eb2-a804-eeeb988eee1d.jpg?1752946801"
    }
}
