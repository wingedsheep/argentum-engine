package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crosstown Courier — Return to Ravnica #34
 * {1}{U} · Creature — Vedalken · 2/1 · Common
 *
 * Whenever this creature deals combat damage to a player, that player mills that many cards.
 *
 * "That player" is [Player.TriggeringPlayer] (the damaged player); "that many" is the damage
 * actually dealt, read with [DynamicAmounts.triggerDamageAmount] as on Living Hive.
 */
val CrosstownCourier = card("Crosstown Courier") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature deals combat damage to a player, that player mills that many cards."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.mill(
            DynamicAmounts.triggerDamageAmount(),
            EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
        description = "Whenever this creature deals combat damage to a player, that player mills that many cards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Chase Stone"
        flavorText = "Information travels quickly through Ravnica's network of messengers and thought agents."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c8875a3-9f56-4947-9655-aa5d95f06de0.jpg?1783940370"
    }
}
