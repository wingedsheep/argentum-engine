package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Inquisitive Puppet
 * {1}
 * Artifact Creature — Construct
 * 0/2
 *
 * When this creature enters, scry 1.
 * Exile this creature: Create a 1/1 white Human creature token.
 */
val InquisitivePuppet = card("Inquisitive Puppet") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    oracleText = "When this creature enters, scry 1.\n" +
        "Exile this creature: Create a 1/1 white Human creature token."
    power = 0
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(1)
    }

    activatedAbility {
        cost = Costs.ExileSelf
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Joseph Meehan"
        flavorText = "\"The strings were gone, but he still felt the pull of invisible forces tugging at his mind.\"\n—*Beyond the Great Henge*"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/807a570a-b5eb-420d-9fa5-6e5364025510.jpg?1783932585"
    }
}
