package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spirit Shackle
 * {B}{B}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature becomes tapped, put a -0/-2 counter on it.
 *
 * "It" is the creature that became tapped — the attachment trigger binds it as the triggering
 * entity — so the counter still lands if the Aura leaves while the trigger is on the stack.
 */
val SpiritShackle = card("Spirit Shackle") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhenever enchanted creature becomes tapped, put a -0/-2 counter on it."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.becomesTapped()
        effect = Effects.AddCounters(CounterType.MINUS_ZERO_MINUS_TWO, 1, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a30bb266-5bd1-4998-ae94-56f0f3354167.jpg?1783948063"
    }
}
