package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Tandem Takedown
 * {1}{G}{G}
 * Instant
 * Up to two target creatures you control each get +1/+0 until end of turn. They each deal damage
 * equal to their power to another target creature, planeswalker, or battle.
 *
 * The victim may be a creature you control, so the dealers can't be sliced out of the chosen
 * targets by a "you control" filter (the Terrific Team-Up shape). Instead each dealer slot has its
 * own per-slot handle. The victim is declared first because the dealer requirement is
 * variable-width (0–2 targets) and target alignment is positional; the dealer requirement is a
 * [TargetOther] so neither dealer can also be the victim ("another target"). An unchosen or
 * illegal dealer slot resolves to nothing, so the other dealer is still pumped and still deals
 * damage (ruling); an illegal victim means nothing is dealt damage.
 */
val TandemTakedown = card("Tandem Takedown") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Up to two target creatures you control each get +1/+0 until end of turn. They each " +
        "deal damage equal to their power to another target creature, planeswalker, or battle."

    spell {
        val victim = target(TargetFilter.CreaturePlaneswalkerOrBattle)
        val (first, second) = targets(
            TargetOther(TargetObject(filter = TargetFilter.CreatureYouControl, count = 2, optional = true))
        )
        effect = Effects.ModifyStats(1, 0, first) then
            Effects.ModifyStats(1, 0, second) then
            Effects.DealDamage(DynamicAmounts.powerOf(first), victim, damageSource = first) then
            Effects.DealDamage(DynamicAmounts.powerOf(second), victim, damageSource = second)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Yigit Koroglu"
        flavorText = "\"I take no pleasure in this, but Vadrok might.\"\n—Vivien Reid, to Lukka"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0eb51be0-6a2e-464a-86d1-aa36179c8c18.jpg?1783916961"
        ruling(
            "2023-04-14",
            "If one of the two target creatures you control is an illegal target as Tandem Takedown " +
                "resolves, the other will still get +1/+0 and then deal damage equal to its power."
        )
        ruling(
            "2023-04-14",
            "If the last target is an illegal target as Tandem Takedown resolves, or if both of the " +
                "first targets are illegal targets, nothing deals or is dealt damage."
        )
    }
}
