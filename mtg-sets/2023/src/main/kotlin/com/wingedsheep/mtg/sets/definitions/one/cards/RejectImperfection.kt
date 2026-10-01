package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reject Imperfection — Phyrexia: All Will Be One #67
 * {1}{U}{U} · Instant
 *
 * Counter target spell. If that spell's mana value was 3 or less, proliferate.
 *
 * "Was" is the mana value the spell had on the stack, so the comparison is made before the counter
 * moves it — the same evaluate-first shape as Unravel. The rider is not "if countered this way", so a spell that can't be countered still
 * lets you proliferate.
 */
val RejectImperfection = card("Reject Imperfection") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. If that spell's mana value was 3 or less, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.manaValueOf(spell),
                ComparisonOperator.LTE,
                3
            ),
            then = Effects.CounterSpell() then Effects.Proliferate(),
            otherwise = Effects.CounterSpell()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "David Astruga"
        flavorText = "\"Your theory holds merit. You do not.\"\n—Sarnvax, Gitaxian sective"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7ce5296-f28f-4105-9c79-b9c63ea720e7.jpg?1783918058"
    }
}
