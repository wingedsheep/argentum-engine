package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Wanderer
 * {3}{W}
 * Legendary Planeswalker
 * Loyalty 5
 *
 * Prevent all noncombat damage that would be dealt to you and other permanents you control.
 * −2: Exile target creature with power 4 or greater.
 *
 * The prevention is two continuous [PreventDamage] replacements restricted to
 * `DamageType.NonCombat` (Blessed Sanctuary's shape): one for the controller ([Recipient.You]) and
 * one for permanents you control other than The Wanderer itself (`notSourceItself()`), so The
 * Wanderer can still be damaged. Recipient filters are re-evaluated per damage instance, so
 * permanents that come under your control later are covered.
 */
val TheWanderer = card("The Wanderer") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker"
    startingLoyalty = 5
    oracleText = "Prevent all noncombat damage that would be dealt to you and other permanents you control.\n" +
        "−2: Exile target creature with power 4 or greater."

    // "Prevent all noncombat damage that would be dealt to you ..."
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.You,
                damageType = DamageType.NonCombat,
            ),
        ),
    )
    // "... and other permanents you control."
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(GameObjectFilter.Permanent.youControl().notSourceItself()),
                damageType = DamageType.NonCombat,
            ),
        ),
    )

    // −2: Exile target creature with power 4 or greater.
    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature.powerAtLeast(4))
        effect = Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0f0e463-9eed-4348-af7c-fde5f0d8188c.jpg?1783933472"
        ruling(
            "2019-05-03",
            "If The Wanderer would be dealt lethal damage at the same time as you and/or other permanents you " +
                "control would be dealt noncombat damage, the damage that would be dealt to you and your other " +
                "permanents is prevented.",
        )
    }
}
