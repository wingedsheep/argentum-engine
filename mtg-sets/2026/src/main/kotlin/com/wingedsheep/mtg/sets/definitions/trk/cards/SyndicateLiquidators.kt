package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.model.Rarity

/**
 * Syndicate Liquidators
 * {4}{G}
 * Creature — Orion Pirate
 * 4/4
 *
 * Trample
 * Landfall — Whenever a land you control enters, put two +1/+1 counters on target creature you control.
 */
val SyndicateLiquidators = card("Syndicate Liquidators") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Orion Pirate"
    colorIdentity = "G"
    oracleText = "Trample\nLandfall — Whenever a land you control enters, put two +1/+1 counters on target creature you control."
    power = 4
    toughness = 4
    keywords(Keyword.TRAMPLE)
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Carlos C Diaz"
        flavorText = "\"The Orion Syndicate. An alliance of all the worst smugglers, pirates, and general malfeasants you'd cross two streets to avoid.\"\n—Constable Odo"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18bb2edd-8dee-4714-a707-6542b6ebbd53.jpg?1786005573"
    }
}
