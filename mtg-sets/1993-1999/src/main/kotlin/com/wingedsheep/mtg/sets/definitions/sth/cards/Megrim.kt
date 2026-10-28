package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Megrim
 * {2}{B}
 * Enchantment
 * Whenever an opponent discards a card, this enchantment deals 2 damage to that player.
 */
val Megrim = card("Megrim") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent discards a card, this enchantment deals 2 damage to that player."

    triggeredAbility {
        trigger = Triggers.anOpponent.discards()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Donato Giancola"
        flavorText = "\"You can run from your pain,\" explained Gerrard to Crovax, \"but take it from experience: you will tire before it does.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4eacd05b-078a-468c-b137-a802346d348a.jpg?1783946561"
    }
}
