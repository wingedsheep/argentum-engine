package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sundering Stroke
 * {6}{R}
 * Sorcery
 * Sundering Stroke deals 7 damage divided as you choose among one, two, or three targets. If at
 * least seven red mana was spent to cast this spell, instead Sundering Stroke deals 7 damage to each
 * of those permanents and/or players.
 *
 * The division is announced as the spell is cast (CR 601.2d) even when seven red will be spent —
 * the divided step sits in the gate's `otherwise` branch, scoped to the target requirement with
 * `among`, so it is still collected at cast time (Magma Opus / Kuldotha Flamefiend shape). The gate
 * reads the mana actually spent (as Searing Barrage's adamant does): with seven red the division is
 * ignored and every still-legal target takes 7; a copy spent nothing, so it deals the damage as
 * divided.
 */
val SunderingStroke = card("Sundering Stroke") {
    manaCost = "{6}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Sundering Stroke deals 7 damage divided as you choose among one, two, or three targets. " +
        "If at least seven red mana was spent to cast this spell, instead Sundering Stroke deals 7 damage " +
        "to each of those permanents and/or players."

    spell {
        val damaged = targets(AnyTarget(count = 3, minCount = 1))
        effect = Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.RED),
                ComparisonOperator.GTE,
                7,
            ),
            then = Effects.ForEachTarget(Effects.DealDamage(7, EffectTarget.ContextTarget(0))),
            otherwise = Effects.DividedDamage(total = 7, minTargets = 1, maxTargets = 3, among = damaged),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Stanton Feng"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24b7a774-ca49-4291-8a19-cb5e475b10d5.jpg?1783932615"
        ruling(
            "2019-10-04",
            "You must divide the damage as you announce Sundering Stroke, even if you intend to pay seven " +
                "red mana to cast it. If you copy Sundering Stroke, no mana was spent to cast the copy at all, " +
                "so the copy will deal the damage as divided."
        )
        ruling("2019-10-04", "Each chosen target must receive at least 1 damage.")
        ruling(
            "2019-10-04",
            "If some of the targets become illegal for Sundering Stroke, the original division of damage " +
                "still applies, but the damage that would have been dealt to illegal targets isn't dealt at all."
        )
    }
}
