package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Backfire
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature deals damage to you, this Aura deals that much damage to that
 * creature's controller.
 *
 * "You" is the Aura's controller, and "that creature's controller" is the damage source's
 * controller — last-known if the creature has left the battlefield before the trigger resolves
 * (CR 608.2h), so a token that traded in combat still names a player. The Aura is the default
 * damage source, which keeps dealing the damage as it last existed if it went to the graveyard
 * with the creature.
 */
val Backfire = card("Backfire") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature deals damage to you, this Aura deals that much damage to that creature's controller."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dealsDamage(Recipient.You)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.ControllerOfDamageSource
        )
        description = "Whenever enchanted creature deals damage to you, this Aura deals that much damage " +
            "to that creature's controller."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04bc57aa-d4d9-4bd9-ba09-984370c7e23b.jpg?1783948078"
    }
}
