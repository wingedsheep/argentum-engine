package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seven of Nine, Unassimilated
 * {1}{U}
 * Legendary Artifact Creature — Human Officer
 * 0/4
 *
 * At the beginning of your first main phase, draw a card. Then discard a card unless you control
 * five or more differently named lands.
 * {6}{U}: Seven of Nine's base power and toughness become 7/9. Activate only if you control seven
 * or more differently named lands.
 *
 * "Your first main phase" is the precombat main phase (Klothys, God of Destiny). The discard gate
 * is read at resolution, after the draw. The {6}{U} ability has no duration, so the 7/9 base
 * lasts indefinitely (layer 7b, [Duration.Permanent]).
 */
val SevenOfNineUnassimilated = card("Seven of Nine, Unassimilated") {
    manaCost = "{1}{U}"
    typeLine = "Legendary Artifact Creature — Human Officer"
    power = 0
    toughness = 4
    oracleText = "At the beginning of your first main phase, draw a card. Then discard a card unless you " +
        "control five or more differently named lands.\n" +
        "{6}{U}: Seven of Nine's base power and toughness become 7/9. Activate only if you control seven " +
        "or more differently named lands."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.DrawCards(1) then Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.differentlyNamedLandsYouControl(),
                ComparisonOperator.LT,
                5,
            ),
            Effects.Discard(1),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{6}{U}")
        effect = Effects.SetBasePowerAndToughness(7, 9, EffectTarget.Self, Duration.Permanent)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.differentlyNamedLandsYouControl(),
                    ComparisonOperator.GTE,
                    7,
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "80"
        artist = "Toraji"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddc517bd-27e1-4688-8941-a41c312bd782.jpg?1792508210"
    }
}
