package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goblin Diplomats
 * {1}{R}
 * Creature — Goblin
 * 2/1
 * {T}: Each creature attacks this turn if able.
 *
 * The requirement is a live group over every creature on the battlefield (all players'), so per
 * the rulings creatures that arrive after the ability resolves must attack too.
 */
val GoblinDiplomats = card("Goblin Diplomats") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 1
    oracleText = "{T}: Each creature attacks this turn if able."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.MustAttackGroup(GroupFilter.AllCreatures)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Izzy"
        flavorText = "When you need to convey that special kind of message."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4620c581-fef7-45e8-ba20-d00903c2f4c5.jpg?1783939913"
        ruling("2013-07-01", "The controller of each creature still decides which player or planeswalker that creature attacks.")
        ruling("2013-07-01", "Creatures that enter the battlefield (or noncreature permanents that become creatures) after the ability resolves are affected by the ability and must attack if able.")
        ruling("2013-07-01", "If, during a player's declare attackers step, a creature is tapped, is affected by a spell or ability that says it can't attack, or hasn't been under that player's control continuously since the turn began (and doesn't have haste), then it doesn't attack. If there's a cost associated with having a creature attack, the player isn't forced to pay that cost, so it doesn't have to attack in that case either.")
    }
}
