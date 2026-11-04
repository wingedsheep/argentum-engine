package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deadeye Rig-Hauler
 * {3}{U}
 * Creature — Human Pirate
 * 3/2
 * Raid — When this creature enters, if you attacked this turn, you may return target creature to its
 * owner's hand.
 *
 * Raid is the intervening-if [Conditions.YouAttackedThisTurn] on the enters trigger, the same shape
 * as Storm Fleet Pyromancer; "you may" is the trigger's `optional` flag.
 */
val DeadeyeRigHauler = card("Deadeye Rig-Hauler") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 3
    toughness = 2
    oracleText = "Raid — When this creature enters, if you attacked this turn, you may return target creature to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        optional = true
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Victor Adame Minguez"
        flavorText = "\"Up you go, you soggy rat.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db310d8f-0db7-44c9-9f2d-9a0773b565b0.jpg?1783935325"
    }
}
