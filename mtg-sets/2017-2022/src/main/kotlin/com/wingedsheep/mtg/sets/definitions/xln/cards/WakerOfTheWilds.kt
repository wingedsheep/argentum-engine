package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Waker of the Wilds
 * {2}{G}{G}
 * Creature — Merfolk Shaman
 * 3/3
 * {X}{G}{G}: Put X +1/+1 counters on target land you control. That land becomes a 0/0 Elemental
 * creature with haste. It's still a land.
 *
 * Roaring Earth's channel shape without the discard: [DynamicAmounts.xValue] counters, then a
 * permanent [Effects.BecomeCreature] (base 0/0, Elemental, haste) — the ruling says the animation
 * never wears off, even if Waker leaves or changes control.
 */
val WakerOfTheWilds = card("Waker of the Wilds") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Shaman"
    power = 3
    toughness = 3
    oracleText = "{X}{G}{G}: Put X +1/+1 counters on target land you control. That land becomes a 0/0 " +
        "Elemental creature with haste. It's still a land."

    activatedAbility {
        cost = Costs.Mana("{X}{G}{G}")
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.xValue(),
            target = land,
        ) then Effects.BecomeCreature(
            target = land,
            power = 0,
            toughness = 0,
            keywords = setOf(Keyword.HASTE),
            creatureTypes = setOf("Elemental"),
            duration = Duration.Permanent,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Shreya Shetty"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d46c4db-e12e-4c79-a17f-2166ed78e9bc.jpg?1783935715"

        ruling("2017-09-29", "The land-animation effect lasts indefinitely. It doesn't wear off during the cleanup step or when you lose control of Waker of the Wilds.")
        ruling("2017-09-29", "If the ability targets a land that's already a creature, that land creature's base power and toughness will become 0/0, overwriting its previous base power and toughness. Other effects that modify its power and/or toughness (including any +1/+1 counters that were on it) will continue to apply.")
    }
}
