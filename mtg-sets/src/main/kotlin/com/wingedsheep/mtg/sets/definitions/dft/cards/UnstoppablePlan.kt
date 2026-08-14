package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unstoppable Plan — Aetherdrift #72
 * {2}{U} · Enchantment
 *
 * At the beginning of your end step, untap all nonland permanents you control.
 *
 * A group untap, not a targeted one: [Effects.ForEachInGroup] over
 * [GroupFilter.AllNonlandPermanents]`.youControl()` rebinds [EffectTarget.Self] to each iterated
 * permanent, so nothing is targeted and no shroud/protection check applies. The enchantment itself
 * is in the group (it's a nonland permanent you control) — untapping an untapped permanent is a
 * no-op, which is exactly what the rules say happens.
 */
val UnstoppablePlan = card("Unstoppable Plan") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, untap all nonland permanents you control."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.ForEachInGroup(
            GroupFilter.AllNonlandPermanents.youControl(),
            Effects.Untap(EffectTarget.Self)
        )
        description = "At the beginning of your end step, untap all nonland permanents you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Borja Pindado"
        flavorText = "Jace knew that if he just had time to explain his plan, he would find " +
            "support. Unfortunately, time was in short supply."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaeb5981-7e6a-4ffd-bb02-4757b2e92f08.jpg?1783907901"
    }
}
