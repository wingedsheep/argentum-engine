package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storm the Seedcore
 * {2}{G}{G}
 * Sorcery
 * Distribute four +1/+1 counters among up to four target creatures you control. Creatures you
 * control gain vigilance and trample until end of turn.
 *
 * "Up to four" is a `count = 4, minCount = 0` target range; the controller chooses the division
 * through a `DistributeDecision`, each target getting at least one counter. With zero targets chosen
 * the spell still resolves and grants vigilance and trample; if every chosen target is illegal it
 * fizzles entirely (per the rulings).
 */
val StormTheSeedcore = card("Storm the Seedcore") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Distribute four +1/+1 counters among up to four target creatures you control. " +
        "Creatures you control gain vigilance and trample until end of turn."

    spell {
        targets(TargetFilter.Creature.youControl(), count = 4, minCount = 0)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 4) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity) then
                    Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Jason Rainville"
        flavorText = "In that moment, the fate of countless worlds rested with one dryad and a handful of the bravest Mirrodin had to offer."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf989a4d-3209-4071-b6e9-d2b99372ec10.jpg?1783916962"
        ruling("2023-04-14", "You choose how many targets Storm the Seedcore has and how the counters will be distributed as you cast the spell. Each target must receive at least one counter.")
        ruling("2023-04-14", "If some of the creatures are illegal targets as Storm the Seedcore tries to resolve, the original distribution of counters still applies and the counters that would have been put on the illegal targets are lost. They won't be put instead on a legal target.")
        ruling("2023-04-14", "If all of Storm the Seedcore's targets are illegal at the time the spell tries to resolve, it won't resolve and none of its effects will happen. Creatures you control won't gain vigilance and trample.")
    }
}
