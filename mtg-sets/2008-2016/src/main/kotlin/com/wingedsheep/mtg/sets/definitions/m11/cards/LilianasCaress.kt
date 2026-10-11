package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liliana's Caress
 * {1}{B}
 * Enchantment
 *
 * Whenever an opponent discards a card, that player loses 2 life.
 */
val LilianasCaress = card("Liliana's Caress") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent discards a card, that player loses 2 life."

    triggeredAbility {
        trigger = Triggers.anOpponent.discards()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent discards a card, that player loses 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Steve Argyle"
        flavorText = "\"This might hurt less if you don't fight so hard. But I doubt it.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d995382-57a6-40a4-9050-31f57cb8dae3.jpg?1783941814"
    }
}
