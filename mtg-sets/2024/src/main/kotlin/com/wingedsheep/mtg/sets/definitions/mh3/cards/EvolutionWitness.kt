package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Evolution Witness {2}{G}
 * Creature — Elf Shaman Mutant
 * 2/1
 * {1}{G}: Adapt 2.
 * Whenever one or more +1/+1 counters are put on this creature, return target permanent card from
 * your graveyard to your hand.
 *
 * Adapt is the zero-counter gate over AddCounters, checked on resolution (CR 701.46a). The trigger is
 * the per-recipient `getsCounters` template bound to this creature, with no once-per-turn rider.
 */
val EvolutionWitness = card("Evolution Witness") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Shaman Mutant"
    oracleText = "{1}{G}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)\n" +
        "Whenever one or more +1/+1 counters are put on this creature, return target permanent card from your " +
        "graveyard to your hand."
    power = 2
    toughness = 1

    // {1}{G}: Adapt 2.
    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        val card = target(TargetFilter.PermanentInYourGraveyard)
        effect = Effects.ReturnToHand(card)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Nereida"
        flavorText = "She remembers every breakthrough, from the gene's mutation to the new life's propagation."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d89283e-9783-4006-9294-4ae0473d2ce6.jpg?1783911262"
        ruling(
            "2024-06-07",
            "You can always activate an ability that will cause a creature to adapt. As that ability resolves, " +
                "if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 " +
                "counters on it.",
        )
        ruling(
            "2024-06-07",
            "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.",
        )
    }
}
