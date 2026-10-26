package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ScaleCounterPlacement
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * The Earth Crystal
 * {2}{G}{G}
 * Legendary Artifact
 * Green spells you cast cost {1} less to cast.
 * If one or more +1/+1 counters would be put on a creature you control, twice that many
 * +1/+1 counters are put on that creature instead.
 * {4}{G}{G}, {T}: Distribute two +1/+1 counters among one or two target creatures you control.
 *
 * The cost reduction is a [ModifySpellCost] static over green spells the controller casts
 * ([CostModification.ReduceGeneric]). The +1/+1 doubling is a [ScaleCounterPlacement]
 * replacement scoped to +1/+1 counters on creatures the controller controls (a
 * Doubling-Season-style "place twice that many" effect, regardless of who places them).
 */
val TheEarthCrystal = card("The Earth Crystal") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Artifact"
    oracleText = "Green spells you cast cost {1} less to cast.\n" +
        "If one or more +1/+1 counters would be put on a creature you control, twice that many " +
        "+1/+1 counters are put on that creature instead.\n" +
        "{4}{G}{G}, {T}: Distribute two +1/+1 counters among one or two target creatures you control."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.GREEN)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    replacementEffect(
        ScaleCounterPlacement(
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                recipient = Recipient.Object(GameObjectFilter.Creature.youControl())
            )
        )
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}{G}"), Costs.Tap)
        target = TargetObject(filter = TargetFilter.CreatureYouControl, count = 2, minCount = 1)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Pablo Mendoza"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d585e218-3dc8-4fbd-8ad2-795fbc9b2155.jpg?1782686462"
    }
}
