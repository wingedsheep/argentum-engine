package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.effects.CreateChosenTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.EntersWithColorChoice
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters

/**
 * Riptide Replicator
 * {X}{4}
 * Artifact
 * As Riptide Replicator enters the battlefield, choose a color and a creature type.
 * Riptide Replicator enters the battlefield with X charge counters on it.
 * {4}, {T}: Create an X/X creature token of the chosen color and type, where X is
 * the number of charge counters on Riptide Replicator.
 */
val RiptideReplicator = card("Riptide Replicator") {
    manaCost = "{X}{4}"
    typeLine = "Artifact"
    oracleText = "As Riptide Replicator enters the battlefield, choose a color and a creature type.\nRiptide Replicator enters the battlefield with X charge counters on it.\n{4}, {T}: Create an X/X creature token of the chosen color and type, where X is the number of charge counters on Riptide Replicator."

    replacementEffect(EntersWithColorChoice())
    replacementEffect(EntersWithCreatureTypeChoice())
    replacementEffect(EntersWithDynamicCounters(
        counterType = CounterTypeFilter.Named("charge"),
        count = DynamicAmount.XValue
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{4}"),
            Costs.Tap
        )
        effect = CreateChosenTokenEffect(
            dynamicPower = DynamicAmount.CountersOnSelf(CounterTypeFilter.Named("charge")),
            dynamicToughness = DynamicAmount.CountersOnSelf(CounterTypeFilter.Named("charge"))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "309"
        artist = "Michael Sutfin"
        flavorText = "It doesn't create just any kind of monster—it creates the best kind of monster."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41bb314f-237a-43fc-95c8-b26188dc4476.jpg?1562910457"
    }
}
