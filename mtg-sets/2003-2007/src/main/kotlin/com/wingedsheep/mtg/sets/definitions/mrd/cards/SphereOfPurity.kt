package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Sphere of Purity — Mirrodin #26
 * {3}{W} · Enchantment · Common
 *
 * If an artifact would deal damage to you, prevent 1 of that damage.
 *
 * Modelling notes:
 * - A continuous [PreventDamage] replacement (CR 615) rather than a shield effect: it applies to
 *   every instance for as long as the Sphere is on the battlefield, and there is nothing to
 *   "use up". `amount = 1` is a *partial* prevention — a Triskelion ping is fully stopped, an
 *   Arcbound Ravager swing for 4 still deals 3.
 * - Recipient is [Recipient.You], i.e. the Sphere's controller only; it does nothing for
 *   your creatures or planeswalkers, and nothing for your teammates.
 * - The source filter is `GameObjectFilter.Artifact`, not artifact *creature* — the damage
 *   source only has to be an artifact, so an artifact creature attacking you is covered too.
 *   Same shape as Artifact Ward's clause, with the recipient moved from the enchanted creature
 *   to the controller and the amount capped at 1.
 */
val SphereOfPurity = card("Sphere of Purity") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "If an artifact would deal damage to you, prevent 1 of that damage."

    replacementEffect(
        PreventDamage(
            amount = DynamicAmounts.fixed(1),
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.You,
                source = GameObjectFilter.Artifact
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Thomas Gianni"
        flavorText = "Purity rejects artifice."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/392944d5-bff1-4125-86db-68d05682a430.jpg?1783944557"
    }
}
