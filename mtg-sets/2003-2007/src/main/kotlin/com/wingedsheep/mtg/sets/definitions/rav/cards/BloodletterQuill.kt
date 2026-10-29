package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodletter Quill — Ravnica: City of Guilds #254
 * {3} · Artifact · Rare
 *
 * {2}, {T}, Put a blood counter on this artifact: Draw a card, then you lose 1 life for each blood
 * counter on this artifact.
 * {U}{B}: Remove a blood counter from this artifact.
 *
 * A repeatable draw whose price is its own history: every card costs one more life than the last,
 * and the Dimir half of the card buys that history back down a counter at a time.
 *
 * **The counter is an activation cost, not part of the effect.** [Costs.PutCounterOnSelf] is the
 * accruing cost atom (Mazemind Tome's shape) — it is paid on activation, before the ability is even
 * on the stack, and it takes nothing away, so it never runs out. That ordering is what the
 * card's own ruling turns on: the counter is already there when the ability resolves, so the very
 * first activation costs a life, and an opponent (or you) may respond to the activation by removing
 * that counter with the second ability before the life is counted.
 *
 * **The life loss is read at resolution, not at activation.** [DynamicAmounts.countersOnSelf] over
 * [CounterType.BLOOD] resolves against the Quill's counters at the moment the draw resolves — which is
 * exactly what makes the response window in that ruling meaningful.
 *
 * **The second ability removes the counter as its *effect*, not as a cost.** Its printed cost is
 * only {U}{B}; "Remove a blood counter from this artifact" is what it does on resolution. Wiring
 * the removal as a cost would make it unactivatable with no counters on the Quill — harmless in
 * practice, but wrong, and it would also let the removal happen before the mana was even paid.
 *
 * The blood counter ([CounterType.BLOOD]) is a passive storage counter with no inherent rule; the
 * card's own two abilities are the only things that write and read it. It is unrelated to the
 * Innistrad Blood *token*.
 */
val BloodletterQuill = card("Bloodletter Quill") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}, Put a blood counter on this artifact: Draw a card, then you lose 1 " +
        "life for each blood counter on this artifact.\n" +
        "{U}{B}: Remove a blood counter from this artifact."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.PutCounterOnSelf(CounterType.BLOOD),
        )
        effect = Effects.DrawCards(1) then Effects.LoseLife(
            DynamicAmounts.countersOnSelf(CounterType.BLOOD),
            EffectTarget.Controller,
        )
        description = "{2}, {T}, Put a blood counter on this artifact: Draw a card, then you lose " +
            "1 life for each blood counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Mana("{U}{B}")
        effect = Effects.RemoveCounters(CounterType.BLOOD, 1, EffectTarget.Self)
        description = "{U}{B}: Remove a blood counter from this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "254"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8fbdf87-2424-4fc2-904e-b1e5fef2a335.jpg?1783943602"
        ruling(
            "2005-10-01",
            "You may activate the second ability in response to activating the first one. If you " +
                "do, you'll remove the blood counter that was just added before you lose the life."
        )
    }
}
