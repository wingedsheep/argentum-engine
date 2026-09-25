package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akki Underminer
 * {3}{R}
 * Creature — Goblin Rogue Shaman
 * 1/1
 * Whenever this creature deals combat damage to a player, that player sacrifices a permanent of their choice.
 *
 * "That player" is the damaged player — [Player.TriggeringPlayer], bound by the combat-damage
 * trigger (the same spelling as `inv/cards/BlazingSpecter.kt`) — and the sacrificed permanent is
 * any permanent they control, chosen by them ([com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect]).
 */
val AkkiUnderminer = card("Akki Underminer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Rogue Shaman"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature deals combat damage to a player, that player sacrifices a permanent of their choice."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Sacrifice(GameObjectFilter.Permanent, 1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever this creature deals combat damage to a player, that player sacrifices a permanent of their choice."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Thomas M. Baxa"
        flavorText = "\"Deep inside the Sokenzan Mountains, a band of akki discovered a cache of ancient items of power. Their ensuing spree of destruction became known as 'The Three Days of Fun.'\"\n—*Observations of the Kami War*"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efdd1e22-d68d-4858-b253-80071d26d50e.jpg?1783944304"
    }
}
