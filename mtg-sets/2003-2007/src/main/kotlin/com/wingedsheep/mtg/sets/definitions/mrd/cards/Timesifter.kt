package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** Pipeline slot holding the single player who won the contest, or nothing if it ended undecided. */
private const val WINNER = "timesifterWinner"

/**
 * Timesifter — Mirrodin #262 (canonical printing)
 * {5} · Artifact · Rare
 *
 * At the beginning of each upkeep, each player exiles the top card of their library. The player
 * who exiled the card with the greatest mana value takes an extra turn after this one. If two or
 * more players' cards are tied for greatest, the tied players repeat this process until the tie is
 * broken.
 *
 * Modelling notes:
 * - The whole first sentence plus the tie rule is one primitive,
 *   [Effects.ExileTopCardContest] — the rounds are fully deterministic (no player ever chooses
 *   anything), so it resolves in a single pass with no decision or continuation.
 * - **The primitive is open**: it publishes the winning player into [WINNER] and stops, and the
 *   payoff is composed here off `EffectTarget.PipelineTarget`. That keeps "take an extra turn" out
 *   of a library primitive, and is what lets the next card that ranks exiled top cards reuse it.
 * - **The contest can end with nobody winning**, so the extra turn is gated on [WINNER] actually
 *   holding a player: a player with an empty library exiles nothing and therefore can't have
 *   exiled the greatest mana value, and if no contender can exile at all the tie stands unbroken.
 *   Without the gate an unresolved `PipelineTarget` falls back to the ability's controller, which
 *   would hand Timesifter's own controller a free turn every time the table decked out.
 * - `Triggers.EachUpkeep` fires on **every** player's upkeep, not just its controller's — the card
 *   is symmetrical, and in a two-player game that is two contests per turn cycle.
 * - Cards exiled by the contest stay in exile face up; nothing here returns them.
 */
val Timesifter = card("Timesifter") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of each upkeep, each player exiles the top card of their " +
        "library. The player who exiled the card with the greatest mana value takes an extra " +
        "turn after this one. If two or more players' cards are tied for greatest, the tied " +
        "players repeat this process until the tie is broken."

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = Effects.Composite(
            Effects.ExileTopCardContest(storeWinnerAs = WINNER),
            ConditionalEffect(
                condition = Conditions.CompareAmounts(
                    DynamicAmount.DistinctEntitiesInCollections(listOf(WINNER)),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(1)
                ),
                effect = Effects.TakeExtraTurn(target = EffectTarget.PipelineTarget(WINNER))
            )
        )
        description = "At the beginning of each upkeep, each player exiles the top card of their " +
            "library. The player who exiled the card with the greatest mana value takes an extra " +
            "turn after this one. If two or more players' cards are tied for greatest, the tied " +
            "players repeat this process until the tie is broken."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "262"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/561cab0e-8874-4534-bf79-0c1488a9f0a5.jpg"
    }
}
