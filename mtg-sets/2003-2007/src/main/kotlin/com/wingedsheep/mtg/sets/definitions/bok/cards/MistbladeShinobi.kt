package com.wingedsheep.mtg.sets.definitions.bok.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mistblade Shinobi — Betrayers of Kamigawa #43 (canonical printing)
 * {2}{U} · Creature — Human Ninja · 1/1
 *
 * Ninjutsu {U}
 * Whenever this creature deals combat damage to a player, you may return target creature that
 * player controls to its owner's hand.
 *
 * "That player" is the damaged player — `controlledByTriggeringPlayer()` scopes the target, as on
 * Rustmouth Ogre; [Effects.May] around the bounce is the "you may".
 */
val MistbladeShinobi = card("Mistblade Shinobi") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Ninja"
    power = 1
    toughness = 1
    oracleText = "Ninjutsu {U} ({U}, Return an unblocked attacker you control to hand: Put this " +
        "card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, you may return target creature " +
        "that player controls to its owner's hand."

    ninjutsu("{U}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter(GameObjectFilter.Creature.controlledByTriggeringPlayer()))
        effect = Effects.May(Effects.ReturnToHand(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c06f58f-0419-4637-8060-4a93670a4c68.jpg?1783944206"
    }
}
