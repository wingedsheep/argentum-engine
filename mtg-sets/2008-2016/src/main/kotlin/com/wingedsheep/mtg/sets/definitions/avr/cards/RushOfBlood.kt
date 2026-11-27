package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rush of Blood — Avacyn Restored #154 (canonical printing)
 * {2}{R} · Instant
 *
 * Target creature gets +X/+0 until end of turn, where X is its power.
 *
 * X is read once, on resolution, off the target's current (projected) power — the modify-stats
 * executor evaluates the amount when it creates the floating effect — so later pumps don't
 * re-double it. Same composition as Berserk's pump.
 */
val RushOfBlood = card("Rush of Blood") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +X/+0 until end of turn, where X is its power."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(DynamicAmounts.powerOf(creature), DynamicAmounts.fixed(0), creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Cynthia Sheppard"
        flavorText = "Sometimes it's not for sustenance or to feed the dependence. Sometimes it's just to feel again."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2884824-d138-47f2-913b-32cd475e9584.jpg?1783940683"
        ruling(
            "2012-05-01",
            "The value of X is determined when Rush of Blood resolves. The bonus won't change later in the " +
                "turn if the creature's power changes."
        )
    }
}
