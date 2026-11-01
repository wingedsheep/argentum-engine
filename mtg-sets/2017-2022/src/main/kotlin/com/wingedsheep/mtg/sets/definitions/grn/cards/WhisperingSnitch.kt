package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Whispering Snitch
 * {1}{B}
 * Creature — Vampire Rogue
 * 1/3
 * Whenever you surveil for the first time each turn, this creature deals 1 damage to each opponent
 * and you gain 1 life.
 */
val WhisperingSnitch = card("Whispering Snitch") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "Whenever you surveil for the first time each turn, this creature deals 1 damage to " +
        "each opponent and you gain 1 life."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.surveils(firstTimeEachTurn = true)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Jason Rainville"
        flavorText = "\"Sure, I hear things. I hear plenty. But how am I to talk with this parched throat?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa4ca97e-b402-4a73-9c70-79ec4565a39c.jpg?1783934168"
    }
}
