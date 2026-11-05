package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sixth Sense
 * {G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "Whenever this creature deals combat damage to a player, you may draw a card."
 *
 * The trigger is granted to the creature, so "you" is the creature's controller, not necessarily
 * the Aura's.
 */
val SixthSense = card("Sixth Sense") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"Whenever this creature deals combat damage to a player, you may draw a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantTriggeredAbility(
            TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.May(Effects.DrawCards(1))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Zoltan Boros"
        flavorText = "Instinct yields the truest insight."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e564db6b-0b8b-4c67-9135-5da76d2225fe.jpg"
    }
}
