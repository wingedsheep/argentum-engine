package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Giant Turtle
 * {1}{G}{G}
 * Creature — Turtle
 * 2/4
 * This creature can't attack if it attacked during your last turn.
 *
 * A [CantAttack] on itself, gated by [Conditions.SourceAttackedLastTurn]. That predicate reads the
 * *current* controller's own-last-turn attacker record, which is what the 2004-10-04 ruling asks
 * for: a Turtle you steal after it attacked on an opponent's turn is free to attack on yours.
 */
val GiantTurtle = card("Giant Turtle") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Turtle"
    power = 2
    toughness = 4
    oracleText = "This creature can't attack if it attacked during your last turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantAttack(GroupFilter.source()),
            condition = Conditions.SourceAttackedLastTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Jeff A. Menges"
        flavorText = "\"The turtle lives 'twixt plated decks/ Which practically conceal its sex./ " +
            "I think it clever of the turtle/ In such a fix to be so fertile.\"\n—Ogden Nash, \"The Turtle\""
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87e5fc19-3b10-476f-9a73-e8bf4b5fbec0.jpg?1783948047"
    }
}
