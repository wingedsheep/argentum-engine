package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thrashing Mossdog
 * {3}{G}
 * Creature — Plant Dog
 * 3/3
 * Reach
 * Scavenge {4}{G}{G}
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 *
 * Scavenge follows the Deadbridge Goliath shape: a graveyard-activated ability costing the mana
 * plus [Costs.ExileSelf], sorcery timing, and +1/+1 counters equal to the exiled card's power.
 */
val ThrashingMossdog = card("Thrashing Mossdog") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Dog"
    power = 3
    toughness = 3
    oracleText = "Reach (This creature can block creatures with flying.)\nScavenge {4}{G}{G} ({4}{G}{G}, " +
        "Exile this card from your graveyard: Put a number of +1/+1 counters equal to this card's power " +
        "on target creature. Scavenge only as a sorcery.)"

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}{G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {4}{G}{G}"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffd0d63a-d947-4ce4-8e34-5c1521955b18.jpg?1783940034"
    }
}
