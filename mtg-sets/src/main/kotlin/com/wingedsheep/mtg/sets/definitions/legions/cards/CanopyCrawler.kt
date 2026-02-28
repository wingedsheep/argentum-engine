package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AmplifyEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Canopy Crawler
 * {3}{G}
 * Creature — Beast
 * 2/2
 * Amplify 1 (As this creature enters, put a +1/+1 counter on it for each
 * Beast card you reveal in your hand.)
 * {T}: Target creature gets +1/+1 until end of turn for each +1/+1 counter
 * on Canopy Crawler.
 */
val CanopyCrawler = card("Canopy Crawler") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 2
    oracleText = "Amplify 1 (As this creature enters, put a +1/+1 counter on it for each Beast card you reveal in your hand.)\n{T}: Target creature gets +1/+1 until end of turn for each +1/+1 counter on Canopy Crawler."

    keywords(Keyword.AMPLIFY)

    replacementEffect(AmplifyEffect(countersPerReveal = 1))

    activatedAbility {
        cost = Costs.Tap
        val creature = target("creature", Targets.Creature)
        effect = Effects.ModifyStats(
            DynamicAmount.CountersOnSelf(CounterTypeFilter.PlusOnePlusOne),
            DynamicAmount.CountersOnSelf(CounterTypeFilter.PlusOnePlusOne),
            creature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0ccdc9d7-71b5-4304-8d19-a63952e17a6b.jpg?1562897615"
        ruling("2004-10-04", "The number of +1/+1 counters for the ability is checked when the ability resolves.")
    }
}
