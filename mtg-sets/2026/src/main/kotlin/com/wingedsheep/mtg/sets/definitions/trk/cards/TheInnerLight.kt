package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Inner Light — Star Trek #241
 * {3}{G}{W} · Enchantment — Saga
 *
 * I — Return target green or white creature card from your graveyard to the battlefield.
 * II — Choose target creature. Whenever it deals combat damage to a player this turn, you draw
 *      that many cards.
 * III — You gain life equal to the greatest power among creatures you control. Create a Treasure
 *       token.
 *
 * - Chapter II is the Saga's own delayed triggered ability watching the chosen creature
 *   (`watchedTarget`, the Fire Giant's Fury shape), not an ability granted to it — so the Saga's
 *   controller draws even when the target is an opponent's creature, and it lasts the turn.
 */
val TheInnerLight = card("The Inner Light") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Return target green or white creature card from your graveyard to the battlefield.\n" +
        "II — Choose target creature. Whenever it deals combat damage to a player this turn, you draw that many cards.\n" +
        "III — You gain life equal to the greatest power among creatures you control. Create a Treasure token."

    sagaChapter(1) {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard.withAnyColor(Color.GREEN, Color.WHITE))
        effect = Effects.Move(creatureCard, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    sagaChapter(2) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.CreateDelayedTrigger(
            effect = Effects.DrawCards(DynamicAmounts.triggerDamageAmount()),
            trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
            watchedTarget = creature,
            expiry = DelayedTriggerExpiry.EndOfTurn
        )
    }

    sagaChapter(3) {
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        ) then Effects.CreateTreasure(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Marko Manev"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af92a773-b237-4cd8-b50d-4f7c3b9290ed.jpg?1792753772"
    }
}
