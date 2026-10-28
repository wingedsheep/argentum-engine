package com.wingedsheep.mtg.sets.definitions.apc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Whirlpool Warrior
 * {2}{U}
 * Creature — Merfolk Warrior
 * 2/2
 * When this creature enters, shuffle the cards from your hand into your library, then draw that many cards.
 * {R}, Sacrifice this creature: Each player shuffles the cards from their hand into their library,
 * then draws that many cards.
 *
 * Both halves are [Patterns.Hand.wheelEffect] (Winds of Change's body): the enters trigger scoped
 * to you, the sacrifice ability to every player.
 */
val WhirlpoolWarrior = card("Whirlpool Warrior") {
    manaCost = "{2}{U}"
    colorIdentity = "UR"
    typeLine = "Creature — Merfolk Warrior"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, shuffle the cards from your hand into your library, then draw that many cards.\n" +
        "{R}, Sacrifice this creature: Each player shuffles the cards from their hand into their library, then draws that many cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.wheelEffect(Player.You)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.SacrificeSelf)
        effect = Patterns.Hand.wheelEffect(Player.Each)
        description = "{R}, Sacrifice this creature: Each player shuffles the cards from their hand into their library, then draws that many cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01f891ca-4e6a-4710-b1cf-5dabb5e1ad93.jpg?1783945350"
    }
}
