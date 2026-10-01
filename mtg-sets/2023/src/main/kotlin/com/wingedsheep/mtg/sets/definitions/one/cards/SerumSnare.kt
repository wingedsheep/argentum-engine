package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Serum Snare
 * {1}{U}
 * Instant
 *
 * Return target nonland permanent to its owner's hand. If that permanent had mana value 3 or less,
 * proliferate.
 *
 * "Had" is a past-tense read of the permanent as it last existed on the battlefield. A
 * `ContextTarget` read is live-only and a bounced token ceases to exist, so the mana value is frozen
 * with `storeNumber` *before* the bounce and the proliferate gate compares the stored number.
 */
val SerumSnare = card("Serum Snare") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent to its owner's hand. If that permanent had mana value 3 " +
        "or less, proliferate. (Choose any number of permanents and/or players, then give each another " +
        "counter of each kind already there.)"

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Pipeline {
            val manaValue = storeNumber(DynamicAmounts.manaValueOf(permanent))
            run(Effects.ReturnToHand(permanent))
            run(
                Effects.If(
                    condition = Conditions.CompareAmounts(manaValue.amount, ComparisonOperator.LTE, 3),
                    then = Effects.Proliferate()
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Lauren K. Cannon"
        flavorText = "\"I bring a message from the Autonomous Furn—argh!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d326bff1-5370-4817-8370-6da87a061058.jpg?1783918057"
    }
}
