package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sab-Sunen, Luxa Embodied — Aetherdrift #221
 * {3}{G}{U} · Legendary Creature — God · 6/6
 *
 * Reach, trample, indestructible
 * Sab-Sunen can't attack or block unless it has an even number of counters on it. (Zero is even.)
 * At the beginning of your first main phase, put a +1/+1 counter on Sab-Sunen. Then if it has an odd
 * number of counters on it, draw two cards.
 *
 * The card is a parity clock: the upkeep-side trigger flips it every turn, so it attacks on even
 * turns and draws on odd ones.
 *
 * Two things the wording pins down and the model must honor:
 *
 * - **"counters on it" means counters of *every* kind**, not just +1/+1 — a stun, shield or oil
 *   counter shifts the parity. Hence [CounterTypeFilter.Any] rather than `PlusOnePlusOne`. The
 *   reminder text "(Zero is even.)" is not an exception but a consequence: a freshly-resolved
 *   Sab-Sunen has zero counters, which is even, so it can attack immediately.
 * - **"Then if …" is checked on resolution, not as an intervening-if.** The draw is a
 *   [ConditionalEffect] chained after the counter is added, so it reads the post-counter total (CR
 *   608.2) — that is what makes the ability draw on the turns it cannot attack.
 *
 * The combat restriction is a plain pair of statics whose condition routes through the standard
 * `ConditionEvaluator`; [DynamicAmounts.countersOnSelf] reads `EntityReference.Source`, which for a
 * static ability on Sab-Sunen is Sab-Sunen. Because the condition is re-read at each restriction
 * check, a counter gained or lost between declare-attackers and declare-blockers correctly changes
 * whether it may block.
 */
val SabSunenLuxaEmbodied = card("Sab-Sunen, Luxa Embodied") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — God"
    oracleText = "Reach, trample, indestructible\n" +
        "Sab-Sunen can't attack or block unless it has an even number of counters on it. " +
        "(Zero is even.)\n" +
        "At the beginning of your first main phase, put a +1/+1 counter on Sab-Sunen. Then if it " +
        "has an odd number of counters on it, draw two cards."
    power = 6
    toughness = 6

    keywords(Keyword.REACH, Keyword.TRAMPLE, Keyword.INDESTRUCTIBLE)

    val countersOnSabSunen = DynamicAmounts.countersOnSelf(CounterTypeFilter.Any)
    val hasEvenCounters = Conditions.AmountIsEven(countersOnSabSunen)

    staticAbility {
        ability = CantAttackUnless(hasEvenCounters)
    }
    staticAbility {
        ability = CantBlockUnless(hasEvenCounters)
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self).then(
            ConditionalEffect(
                condition = Conditions.AmountIsOdd(countersOnSabSunen),
                effect = Effects.DrawCards(2),
            )
        )
        description = "At the beginning of your first main phase, put a +1/+1 counter on " +
            "Sab-Sunen. Then if it has an odd number of counters on it, draw two cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "221"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ef555b1-666d-4386-8983-0e88f9b6cdec.jpg?1783907852"
    }
}
