package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Venerable Warsinger — Strixhaven: School of Mages #246 (canonical printing)
 * {1}{R}{W}
 * Creature — Spirit Cleric
 * 3/3
 *
 * Vigilance, trample
 * Whenever this creature deals combat damage to a player, you may return target creature card
 * with mana value X or less from your graveyard to the battlefield, where X is the amount of
 * damage this creature dealt to that player.
 *
 * X is the triggering event's damage, read by the target filter itself
 * (`manaValueAtMostDynamic(triggerDamageAmount())`): trigger targeting carries the trigger's event
 * context, so the cap is known when the target is chosen and re-checked against the same amount on
 * resolution. "You may" is the resolution-time [optional] choice, like Sun Titan's.
 */
val VenerableWarsinger = card("Venerable Warsinger") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Spirit Cleric"
    power = 3
    toughness = 3
    oracleText = "Vigilance, trample\n" +
        "Whenever this creature deals combat damage to a player, you may return target creature card " +
        "with mana value X or less from your graveyard to the battlefield, where X is the amount of " +
        "damage this creature dealt to that player."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val creatureCard = target(
            TargetFilter(
                GameObjectFilter.Creature.ownedByYou().manaValueAtMostDynamic(DynamicAmounts.triggerDamageAmount()),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard)
        optional = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Jokubas Uogintas"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/006313f8-cc60-4a4e-8ec6-953cdf1c16e3.jpg?1783927285"
    }
}
