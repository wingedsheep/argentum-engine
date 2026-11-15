package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sidisi's Faithful
 * {U}
 * Creature — Snake Wizard
 * 0/4
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, return target creature to its owner's hand.
 *
 * The targeted bounce rides the exploit reflexive, so the creature is chosen after the sacrifice
 * resolves — and the payoff still happens when the Faithful exploits itself.
 */
val SidisisFaithful = card("Sidisi's Faithful") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Wizard"
    power = 0
    toughness = 4
    oracleText = "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, return target creature to its owner's hand."

    exploit {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Lius Lasahido"
        flavorText = "\"I tire of your prattle, and your face.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d02daad9-701f-40d4-ad0b-44a3cbd32c3f.jpg"
    }
}
