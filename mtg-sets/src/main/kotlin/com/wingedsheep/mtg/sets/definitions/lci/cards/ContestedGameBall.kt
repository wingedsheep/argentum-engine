package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GainControlByActivePlayerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Contested Game Ball (The Lost Caverns of Ixalan)
 * {2}
 * Artifact
 *
 * Whenever you're dealt combat damage, the attacking player gains control of this artifact and
 * untaps it.
 * {2}, {T}: Draw a card and put a point counter on this artifact. Then if it has five or more
 * point counters on it, sacrifice it and create a Treasure token.
 *
 * Implementation:
 *  - The combat-damage trigger uses the defensive batch trigger
 *    [Triggers.OneOrMoreCreaturesDealCombatDamageToYou] (Witch-king of Angmar's idiom): it fires
 *    at most once per combat-damage batch no matter how many creatures connected (matching the
 *    ruling "triggers only once ... no matter how many creatures deal combat damage to you at the
 *    same time"). "You" re-binds to whoever currently controls the artifact each combat.
 *  - "The attacking player gains control" is modeled with [GainControlByActivePlayerEffect]
 *    (Risky Move's idiom, a permanent Layer.CONTROL change to the active player). Combat only
 *    happens during the active player's turn, and only the active player declares attackers, so
 *    the player who dealt you combat damage is necessarily the active player; when this trigger
 *    resolves the turn hasn't changed, so the active player is still that attacker. This is exact
 *    for the supported one-on-one game. (In Two-Headed Giant the printed card lets you choose which
 *    member of the attacking team gains control; multiplayer isn't supported yet — see
 *    backlog/multiplayer.md.) The artifact is then untapped so the new controller can use it.
 *  - The activated ability composes [Effects.DrawCards] (1) -> [Effects.AddCounters] (a passive
 *    [Counters.POINT] counter on Self) -> a resolution-time [ConditionalEffect] gated on
 *    [Conditions.SourceCounterCountAtLeast]`(point, 5)` that sacrifices the artifact
 *    ([Effects.SacrificeTarget]`(Self)`) and makes a Treasure ([Effects.CreateTreasure]). The
 *    threshold is checked only as the ability resolves (ruling: point counters added another way
 *    don't trigger the sacrifice), which `ConditionalEffect` does. Same "add a counter, then
 *    conditionally do more" shape as Treasure Map / Brass's Tunnel-Grinder.
 */
val ContestedGameBall = card("Contested Game Ball") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Whenever you're dealt combat damage, the attacking player gains control of this " +
        "artifact and untaps it.\n" +
        "{2}, {T}: Draw a card and put a point counter on this artifact. Then if it has five or " +
        "more point counters on it, sacrifice it and create a Treasure token."

    triggeredAbility {
        trigger = Triggers.OneOrMoreCreaturesDealCombatDamageToYou()
        effect = Effects.Composite(
            GainControlByActivePlayerEffect(EffectTarget.Self),
            Effects.Untap(EffectTarget.Self),
        )
        description = "Whenever you're dealt combat damage, the attacking player gains control of " +
            "this artifact and untaps it."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.AddCounters(Counters.POINT, 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtLeast(Counters.POINT, 5),
                effect = Effects.Composite(
                    Effects.SacrificeTarget(EffectTarget.Self),
                    Effects.CreateTreasure(1),
                ),
            ),
        )
        description = "{2}, {T}: Draw a card and put a point counter on this artifact. Then if it " +
            "has five or more point counters on it, sacrifice it and create a Treasure token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "251"
        artist = "Camille Alquier"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71cb7776-a6af-4efb-b536-6b9b4f3d3874.jpg?1782694410"
        ruling(
            "2023-11-10",
            "Contested Game Ball's triggered ability triggers only once whenever you're dealt " +
                "combat damage, no matter how many creatures deal combat damage to you at the same time.",
        )
        ruling(
            "2023-11-10",
            "If point counters are put on Contested Game Ball some way other than its last ability, " +
                "you won't sacrifice it or create a Treasure token. The check for five or more point " +
                "counters happens only as the activated ability resolves.",
        )
    }
}
