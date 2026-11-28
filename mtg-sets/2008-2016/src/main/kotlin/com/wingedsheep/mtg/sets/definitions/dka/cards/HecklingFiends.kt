package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Heckling Fiends — Dark Ascension #92 (canonical printing)
 * {2}{R} · Creature — Devil · 2/2
 *
 * {2}{R}: Target creature attacks this turn if able.
 */
val HecklingFiends = card("Heckling Fiends") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 2
    toughness = 2
    oracleText = "{2}{R}: Target creature attacks this turn if able."

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.MarkMustAttackThisTurn(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Clint Cearley"
        flavorText = "Their language may be a cacophonous agglomeration of chittering snorts and mad barking, " +
            "but the message they send is all too clear."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9fd8895-9282-44d3-969f-b0529eb3bc07.jpg?1783940816"
        ruling("2011-01-22", "The creature's controller still decides which player or planeswalker the creature attacks.")
        ruling(
            "2011-01-22",
            "If, during a player's declare attackers step, a creature is tapped, is affected by a spell or ability " +
                "that says it can't attack, or hasn't been under that player's control continuously since the turn " +
                "began (and doesn't have haste), then it doesn't attack. If there's a cost associated with having a " +
                "creature attack, the player isn't forced to pay that cost, so it doesn't have to attack in that case either."
        )
    }
}
