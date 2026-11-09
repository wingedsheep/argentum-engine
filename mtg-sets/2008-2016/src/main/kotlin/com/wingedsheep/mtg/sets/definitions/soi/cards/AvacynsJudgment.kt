package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Avacyn's Judgment (Shadows over Innistrad #145)
 * {1}{R}
 * Sorcery
 *
 * Madness {X}{R}
 * Avacyn's Judgment deals 2 damage divided as you choose among any number of targets. If this
 * spell's madness cost was paid, it deals X damage divided as you choose among those permanents
 * and/or players instead.
 *
 * One amount, chosen by the cast: 2 from hand, X when cast for madness
 * ([Conditions.MadnessCostWasPaid], CR 702.35). The division is announced while casting (CR
 * 601.2d) and every target needs at least 1, so that amount also caps how many targets "any
 * number" can be — the same amount bounds the target count and is the total divided. A madness
 * cast announces X first (601.2b), then the targets, then the division.
 */
val AvacynsJudgment = card("Avacyn's Judgment") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Madness {X}{R} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)\n" +
        "Avacyn's Judgment deals 2 damage divided as you choose among any number of targets. If this " +
        "spell's madness cost was paid, it deals X damage divided as you choose among those permanents " +
        "and/or players instead."

    val damage = DynamicAmounts.conditional(
        Conditions.MadnessCostWasPaid,
        ifTrue = DynamicAmounts.xValue(),
        ifFalse = DynamicAmounts.fixed(2),
    )

    spell {
        target = AnyTarget(
            count = 2, minCount = 0, optional = true, dynamicMaxCount = damage,
            descriptionOverride = "any number of targets",
        )
        effect = Effects.DividedDamage(minTargets = 0, maxTargets = 2, dynamicTotal = damage)
    }

    madness("{X}{R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c5f44ce-1464-4282-9afa-20e9ea44c613.jpg?1783937759"
        ruling("2016-04-08", "If Avacyn's Judgment has multiple targets, and some but not all of them are illegal targets when Avacyn's Judgment resolves, Avacyn's Judgment will still deal damage to the remaining legal targets according to the original damage division.")
        ruling("2016-04-08", "You announce how the damage will be divided as part of casting Avacyn's Judgment. Each chosen target must receive at least 1 damage.")
    }
}
