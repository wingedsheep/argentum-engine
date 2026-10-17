package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Underworld Dreams
 * {B}{B}{B}
 * Enchantment
 *
 * Whenever an opponent draws a card, this enchantment deals 1 damage to that player.
 */
val UnderworldDreams = card("Underworld Dreams") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent draws a card, this enchantment deals 1 damage to that player."

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Julie Baroh"
        flavorText = "\"In the drowsy dark cave of the mind dreams build their nest with fragments dropped from day's caravan.\" —Rabindranath Tagore"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0e8f8d8-eac0-451c-a167-be84667a8e3d.jpg?1783948061"
    }
}
