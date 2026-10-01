package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fangs of Kalonia
 * {1}{G}
 * Sorcery
 *
 * Put a +1/+1 counter on target creature you control, then double the number of +1/+1 counters on
 * each creature that had a +1/+1 counter put on it this way.
 * Overload {4}{G}{G}
 *
 * Overloaded, "target creature you control" reads "each creature you control" (CR 702.96a): every
 * creature you control gets a counter first, then each of them has its +1/+1 counters doubled.
 */
val FangsOfKalonia = card("Fangs of Kalonia") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control, then double the number of +1/+1 counters on " +
        "each creature that had a +1/+1 counter put on it this way.\nOverload {4}{G}{G} (You may cast this spell " +
        "for its overload cost. If you do, change \"target\" in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{4}{G}{G}"))

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, creature)

        overloadEffect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        ) then Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Leanna Crossan"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ae8a77b-4001-4da2-b684-d0fab370a162.jpg?1783911261"
        ruling("2024-06-07", "To double the number of +1/+1 counters on a creature, put a number of +1/+1 counters on it equal to the number it already has. Other cards that interact with putting counters on it will interact with this effect accordingly.")
        ruling("2024-06-07", "If you are instructed to cast a spell with overload \"without paying its mana cost,\" you can't choose to pay its overload cost instead.")
        ruling("2024-06-07", "Because a spell with overload doesn't target when its overload cost is paid, it may affect permanents with hexproof or with protection from the appropriate color.")
        ruling("2024-06-07", "If you don't pay the overload cost of a spell with overload, that spell will have a single target. If you pay the overload cost, the spell won't have any targets.")
        ruling("2024-06-07", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying (such as an overload cost), add any cost increases, then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
    }
}
