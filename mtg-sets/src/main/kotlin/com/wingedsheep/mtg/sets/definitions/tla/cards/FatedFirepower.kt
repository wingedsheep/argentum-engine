package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fated Firepower
 * {X}{R}{R}{R}
 * Enchantment
 *
 * Flash
 * This enchantment enters with X fire counters on it.
 * If a source you control would deal damage to an opponent or a permanent an opponent
 * controls, it deals that much damage plus an amount of damage equal to the number of
 * fire counters on this enchantment instead.
 *
 * Reuses the existing [Counters.FIRE] named counter (introduced for War Balloon). The
 * "enters with X fire counters" clause mirrors Riptide Replicator's
 * [EntersWithDynamicCounters] with `count = DynamicAmount.XValue` for a noncreature
 * permanent. The outgoing-damage amplification is a [ModifyDamageAmount] whose
 * `dynamicModifier` reads this enchantment's own fire-counter count
 * ([DynamicAmounts.countersOnSelf]); the [EventPattern.DamageEvent] scopes it to a source
 * the controller owns ([SourceFilter.YouControl]) dealing damage to an opponent or a
 * permanent an opponent controls ([RecipientFilter.OpponentOrPermanentTheyControl]).
 */
val FatedFirepower = card("Fated Firepower") {
    manaCost = "{X}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "This enchantment enters with X fire counters on it.\n" +
        "If a source you control would deal damage to an opponent or a permanent an opponent " +
        "controls, it deals that much damage plus an amount of damage equal to the number of " +
        "fire counters on this enchantment instead."

    keywords(Keyword.FLASH)

    // This enchantment enters with X fire counters on it.
    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterTypeFilter.Named(Counters.FIRE),
            count = DynamicAmount.XValue
        )
    )

    // Outgoing-damage amplification: a source you control deals that much damage plus the
    // number of fire counters on this enchantment to an opponent or their permanents.
    replacementEffect(
        ModifyDamageAmount(
            dynamicModifier = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.FIRE)),
            appliesTo = EventPattern.DamageEvent(
                source = SourceFilter.YouControl,
                recipient = RecipientFilter.OpponentOrPermanentTheyControl
            )
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "132"
        artist = "Takayama Toshiaki"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51352127-1f86-42e9-b4ca-2fd58b14e86b.jpg?1768375955"
    }
}
