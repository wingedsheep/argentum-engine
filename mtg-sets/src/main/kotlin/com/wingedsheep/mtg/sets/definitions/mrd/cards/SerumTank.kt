package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serum Tank — Mirrodin #240
 * {3} · Artifact · Uncommon
 *
 * Whenever this artifact or another artifact enters, put a charge counter on this artifact.
 * {3}, {T}, Remove a charge counter from this artifact: Draw a card.
 *
 * The trigger is deliberately unscoped: the oracle text says "another artifact", not "another
 * artifact you control", so *every* artifact entering the battlefield charges the Tank — including
 * an opponent's. Modelled as [Triggers.entersBattlefield] over the plain (uncontrolled)
 * [GameObjectFilter.Artifact] with [TriggerBinding.ANY], the same read [LeoninElder] and
 * [Vermiculos] use. ANY rather than OTHER is what covers the "this artifact or" half: the Tank is
 * already on the battlefield when its own `ZoneChangeEvent` is emitted, so it sees itself enter and
 * arrives with one counter already on it.
 *
 * The draw is [Costs.Composite] of mana, [Costs.Tap] and [Costs.RemoveCounterFromSelf] — three
 * separate throttles, so the Tank converts at most one counter per turn cycle and only while it has
 * a counter to remove. Removing the counter is part of the *cost*, so an unpaid or illegal
 * activation never draws, and countering the ability on the stack does not refund the counter.
 */
val SerumTank = card("Serum Tank") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever this artifact or another artifact enters, put a charge counter on this artifact.\n" +
        "{3}, {T}, Remove a charge counter from this artifact: Draw a card."

    // Whenever this artifact or another artifact enters, put a charge counter on this artifact.
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Artifact,
            binding = TriggerBinding.ANY
        )
        effect = AddCountersEffect(Counters.CHARGE, 1, EffectTarget.Self)
    }

    // {3}, {T}, Remove a charge counter from this artifact: Draw a card.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(Counters.CHARGE, 1)
        )
        effect = Effects.DrawCards(1)
        description = "{3}, {T}, Remove a charge counter from this artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Corey D. Macourek"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/126ec055-1a7d-426b-a87f-c85c60aa7fc3.jpg?1783944505"
    }
}
