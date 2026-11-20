package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Haazda Snare Squad
 * {2}{W}
 * Creature — Human Soldier
 * 1/4
 * Whenever this creature attacks, you may pay {W}. If you do, tap target creature an opponent controls.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 *
 * The target is chosen as the trigger goes on the stack; the optional {W} is offered on resolution
 * (the Quiet Contemplation / Frenzied Goblin `MayPay` shape).
 */
val HaazdaSnareSquad = card("Haazda Snare Squad") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 4
    oracleText = "Whenever this creature attacks, you may pay {W}. If you do, tap target creature an opponent controls."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{W}"),
            then = Effects.Tap(creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "David Palumbo"
        flavorText = "They've been probation officers, army grunts, bounty hunters—anything to make their district safer."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85d3c012-f356-424d-a960-60e95f395134.jpg"
    }
}
