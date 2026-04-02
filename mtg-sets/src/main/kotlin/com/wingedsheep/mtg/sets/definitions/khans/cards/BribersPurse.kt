package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Briber's Purse
 * {X}
 * Artifact
 * Briber's Purse enters the battlefield with X gem counters on it.
 * {1}, {T}, Remove a gem counter from Briber's Purse: Target creature can't attack or block this turn.
 *
 * Ruling (2014-09-20): Activating the ability targeting a creature that's already attacking or
 * blocking won't remove it from combat or affect that attack or block.
 * Ruling (2014-09-20): If Briber's Purse has no gem counters on it, it remains on the
 * battlefield, although you can't activate its last ability.
 */
val BribersPurse = card("Briber's Purse") {
    manaCost = "{X}"
    typeLine = "Artifact"
    oracleText = "Briber's Purse enters the battlefield with X gem counters on it.\n{1}, {T}, Remove a gem counter from Briber's Purse: Target creature can't attack or block this turn."

    replacementEffect(EntersWithDynamicCounters(
        counterType = CounterTypeFilter.Named(Counters.GEM),
        count = DynamicAmount.XValue
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(Counters.GEM)
        )
        val creature = target("creature", Targets.Creature)
        effect = Effects.CantAttackOrBlock(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Steve Argyle"
        flavorText = "\"Victory is certain. The price, negotiable.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f9951f1-ca51-44a2-8480-602df466f0ab.jpg?1562789249"
        ruling("2014-09-20", "Activating the ability targeting a creature that's already attacking or blocking won't remove it from combat or affect that attack or block.")
        ruling("2014-09-20", "If Briber's Purse has no gem counters on it, it remains on the battlefield, although you can't activate its last ability.")
    }
}
