package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Jessica Jones, Private Eye — Marvel Super Heroes #139 (uncommon)
 * {2}{R} · Legendary Creature — Human Detective Hero · 2/3
 *
 * {T}, Put a stun counter on Jessica Jones: Exile the top X cards of your library, where X is
 * Jessica Jones's power. You may play those cards this turn.
 *
 * Implementation notes:
 * - The activation cost is a two-atom [Costs.Composite]: the tap symbol plus
 *   [Costs.PutCounterOnSelf] with [Counters.STUN] — the Bandit's Haul idiom for counters paid as
 *   part of a cost. The stun counter is the self-imposed brake (CR 701.24: a permanent with a stun
 *   counter that would untap removes one instead), so repeat activations cost successive untap
 *   steps rather than mana.
 * - The payoff is the shared impulse recipe [Patterns.Exile.impulse] with a *dynamic* count —
 *   [DynamicAmounts.sourcePower], the Hugs, Grisly Guardian shape for "exile that many". The count
 *   is read when the ability resolves, off Jessica's projected power, so any pump (or shrink)
 *   applied in response is counted; the stun counter itself does not change her power.
 * - "You may play those cards **this turn**" is [MayPlayExpiry.EndOfTurn] — the permission lapses
 *   at cleanup and normal timing/costs still apply, so a land among them still uses your land drop.
 *   The permission is keyed to the exiled collection, not to Jessica, so it survives her leaving.
 * - If Jessica's power is 0 or less, nothing is exiled and nothing is granted; the ability still
 *   resolves (and the cost is still paid).
 */
val JessicaJonesPrivateEye = card("Jessica Jones, Private Eye") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Detective Hero"
    power = 2
    toughness = 3
    oracleText = "{T}, Put a stun counter on Jessica Jones: Exile the top X cards of your library, " +
        "where X is Jessica Jones's power. You may play those cards this turn. (If a permanent " +
        "with a stun counter would become untapped, remove one from it instead.)"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.PutCounterOnSelf(Counters.STUN),
        )
        effect = Patterns.Exile.impulse(
            count = DynamicAmounts.sourcePower(),
            expiry = MayPlayExpiry.EndOfTurn,
            storeAs = "jessicaJonesExiled",
        )
        description = "{T}, Put a stun counter on Jessica Jones: Exile the top X cards of your " +
            "library, where X is Jessica Jones's power. You may play those cards this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Julia Vasilyeva"
        flavorText = "\"I would trade all my powers for a fresh pot of hot coffee right now.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b48278fd-1d95-4ad6-9a51-f1c5a2ab9f4b.jpg?1783902929"
    }
}
