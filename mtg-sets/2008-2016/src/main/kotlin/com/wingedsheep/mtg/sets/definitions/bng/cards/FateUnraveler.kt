package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fate Unraveler
 * {3}{B}
 * Enchantment Creature — Hag
 * 3/4
 * Whenever an opponent draws a card, this creature deals 1 damage to that player.
 *
 * Underworld Dreams on a body.
 */
val FateUnraveler = card("Fate Unraveler") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Hag"
    power = 3
    toughness = 4
    oracleText = "Whenever an opponent draws a card, this creature deals 1 damage to that player."

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "David Palumbo"
        flavorText = "\"Never forget that the life you've built can be undone by a single loose thread.\"\n—Perisophia the philosopher"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f15cd233-e27b-4cfd-aa4e-18040d1de22f.jpg?1783939557"
    }
}
