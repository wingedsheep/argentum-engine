package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Cultbrand Cinder
 * {4}{B/R}
 * Creature — Elemental Shaman
 * 3 / 3
 *
 * When this creature enters, put a -1/-1 counter on target creature.
 *
 * - The target is mandatory and unrestricted: any creature on the battlefield, including one you
 *   control and including the Cinder itself if it is the only creature around.
 * - [Counters.MINUS_ONE_MINUS_ONE] is the named counter type, not a stat modification — the counter
 *   persists and interacts with wither/persist, which is the whole point in Shadowmoor.
 */
val CultbrandCinder = card("Cultbrand Cinder") {
    manaCost = "{4}{B/R}"
    typeLine = "Creature — Elemental Shaman"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, put a -1/-1 counter on target creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.AddCounters(Counters.MINUS_ONE_MINUS_ONE, 1, creature)
        description = "When this creature enters, put a -1/-1 counter on target creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Christopher Moeller"
        flavorText = "\"Your seared flesh will be the first step in your journey to dark enlightenment.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6b80a10-a9a9-445e-8040-6cc0155271d8.jpg?1783942728"
    }
}
