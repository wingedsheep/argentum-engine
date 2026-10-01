package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vindictive Flamestoker
 * {R}
 * Creature — Phyrexian Wizard
 * 1/2
 *
 * Whenever you cast a noncreature spell, put an oil counter on this creature.
 * {6}{R}, Sacrifice this creature: Discard your hand, then draw four cards. This ability costs {1}
 * less to activate for each oil counter on this creature.
 *
 * The reduction reads the oil counters on the source while the total cost is determined, before
 * the sacrifice is paid; it only eats generic mana, so the {R} always remains.
 */
val VindictiveFlamestoker = card("Vindictive Flamestoker") {
    manaCost = "{R}"
    typeLine = "Creature — Phyrexian Wizard"
    power = 1
    toughness = 2
    oracleText = "Whenever you cast a noncreature spell, put an oil counter on this creature.\n" +
        "{6}{R}, Sacrifice this creature: Discard your hand, then draw four cards. This ability costs {1} less to activate for each oil counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}{R}"), Costs.SacrificeSelf)
        effect = Patterns.Hand.discardHand() then Effects.DrawCards(4)
        genericCostReduction = DynamicAmounts.countersOnSelf(CounterType.OIL)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Xavier Ribeiro"
        flavorText = "Her faith burns hot enough to sear away the flesh of the heretic."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f67e7a16-ff30-4fb9-9fcb-6561eec50caf.jpg?1783918021"
    }
}
