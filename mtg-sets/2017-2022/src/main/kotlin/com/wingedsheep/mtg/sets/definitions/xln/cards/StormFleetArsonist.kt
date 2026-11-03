package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Storm Fleet Arsonist
 * {4}{R}
 * Creature — Orc Pirate
 * 4/4
 * Raid — When this creature enters, if you attacked this turn, target opponent sacrifices a permanent of their choice.
 *
 * Raid is the intervening-if [Conditions.YouAttackedThisTurn] on the enters trigger (see Storm Fleet Spy).
 */
val StormFleetArsonist = card("Storm Fleet Arsonist") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Pirate"
    power = 4
    toughness = 4
    oracleText = "Raid — When this creature enters, if you attacked this turn, target opponent sacrifices a permanent of their choice."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(GameObjectFilter.Permanent, 1, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Jason A. Engle"
        flavorText = "\"Go ahead, build another. I'll burn that down as well.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d8ecb6d-f794-468b-8399-60a50aaa13ac.jpg?1783935737"
    }
}
