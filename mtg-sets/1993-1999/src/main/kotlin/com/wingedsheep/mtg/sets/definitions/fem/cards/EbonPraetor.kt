package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ebon Praetor
 * {4}{B}{B}
 * Creature — Avatar Praetor
 * 5/5
 * First strike, trample
 * At the beginning of your upkeep, put a -2/-2 counter on this creature.
 * Sacrifice a creature: Remove a -2/-2 counter from this creature. If the sacrificed creature was
 * a Thrull, put a +1/+0 counter on this creature. Activate only during your upkeep and only once
 * each turn.
 *
 * The Praetor shrinks by 2/2 every upkeep and the sacrifice only undoes one counter per turn, so
 * feeding it Thrulls — which also add a +1/+0 — is the intended way to keep it alive. Both the
 * upkeep trigger and the once-per-turn ability live in your upkeep, and their order is the player's:
 * activating in response to the trigger removes a counter that isn't there yet, so the usual line is
 * to let the trigger resolve first.
 */
val EbonPraetor = card("Ebon Praetor") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Avatar Praetor"
    oracleText = "First strike, trample\n" +
        "At the beginning of your upkeep, put a -2/-2 counter on this creature.\n" +
        "Sacrifice a creature: Remove a -2/-2 counter from this creature. If the sacrificed " +
        "creature was a Thrull, put a +1/+0 counter on this creature. Activate only during your " +
        "upkeep and only once each turn."
    power = 5
    toughness = 5

    keywords(Keyword.FIRST_STRIKE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.AddCounters(Counters.MINUS_TWO_MINUS_TWO, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, put a -2/-2 counter on this creature."
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP),
                ActivationRestriction.OncePerTurn
            )
        )
        effect = Effects.RemoveCounters(Counters.MINUS_TWO_MINUS_TWO, 1, EffectTarget.Self)
            .then(
                ConditionalEffect(
                    condition = Conditions.SacrificedHadSubtype(Subtype.THRULL.value),
                    effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ZERO, 1, EffectTarget.Self)
                )
            )
        description = "Sacrifice a creature: Remove a -2/-2 counter from this creature. If the sacrificed creature was a Thrull, put a +1/+0 counter on this creature. Activate only during your upkeep and only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Randy Asplund-Faith"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40451f7a-692a-422d-99d3-d93a4d9315e0.jpg?1783947903"
    }
}
