package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Shoreline Looter
 * {1}{U}
 * Creature — Rat Rogue
 * 1/1
 * Shoreline Looter can't be blocked.
 * Threshold — Whenever Shoreline Looter deals combat damage to a player, draw a card.
 * Then discard a card unless there are seven or more cards in your graveyard.
 */
val ShorelineLooter = card("Shoreline Looter") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Rat Rogue"
    power = 1
    toughness = 1
    oracleText = "Shoreline Looter can't be blocked.\nThreshold — Whenever Shoreline Looter deals combat damage to a player, draw a card. Then discard a card unless there are seven or more cards in your graveyard."

    flags(AbilityFlag.CANT_BE_BLOCKED)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.DrawCards(1)
            .then(ConditionalEffect(
                condition = Conditions.Not(Conditions.CardsInGraveyardAtLeast(7)),
                effect = EffectPatterns.discardCards(1)
            ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "PINDURSKI"
        flavorText = "A water skimmer's ability to sense danger in the slightest ripple makes them a prized companion for Long River trawlers."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5bf8cf0-419a-4dc9-9342-aad55c1af05a.jpg?1721426251"
    }
}
