package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cleaving Reaper
 * {3}{B}{B}
 * Creature — Angel Berserker
 * 5/3
 * Flying, trample
 * Pay 3 life: Return this card from your graveyard to your hand. Activate only if you had an
 * Angel or Berserker enter the battlefield under your control this turn.
 *
 * "Angel or Berserker" is read off the per-turn entry log, so a qualifying permanent that has since
 * left the battlefield still counts.
 */
val CleavingReaper = card("Cleaving Reaper") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Angel Berserker"
    power = 5
    toughness = 3
    oracleText = "Flying, trample\n" +
        "Pay 3 life: Return this card from your graveyard to your hand. Activate only if you had an " +
        "Angel or Berserker enter the battlefield under your control this turn."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.PayLife(3)
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Conditions.CompareAmounts(
                        DynamicAmounts.subtypeEnteredUnderControlThisTurn(Subtype.ANGEL),
                        ComparisonOperator.GTE,
                        1,
                    ),
                    Conditions.CompareAmounts(
                        DynamicAmounts.subtypeEnteredUnderControlThisTurn(Subtype.BERSERKER),
                        ComparisonOperator.GTE,
                        1,
                    ),
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "376"
        artist = "Victor Adame Minguez"
        flavorText = "\"I won't pollute my blade with your blood, demon. You'll die by your own axe!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b59dadef-74e5-47c7-a3e0-51075ddb82b1.jpg?1783928123"
    }
}
