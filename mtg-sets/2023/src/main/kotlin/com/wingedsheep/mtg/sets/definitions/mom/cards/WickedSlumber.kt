package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wicked Slumber
 * {3}{U}
 * Instant
 *
 * Convoke
 * Tap up to two target creatures. Put a stun counter on either of them. Then put a stun
 * counter on either of them.
 *
 * The two "either of them" placements are a resolution-time split of two stun counters among
 * the (still-legal) targets, both allowed on the same creature — `DistributeCountersAmongTargets`
 * with `minPerTarget = 0`. A spell's distribute is chosen at resolution through a
 * `DistributeDecision`, and a lone surviving target takes both counters (per the rulings).
 * The tap runs per target via `ForEachTarget`, so an illegal target is skipped.
 */
val WickedSlumber = card("Wicked Slumber") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Tap up to two target creatures. Put a stun counter on either of them. Then put a stun " +
        "counter on either of them. (If a permanent with a stun counter would become untapped, " +
        "remove one from it instead.)"

    keywords(Keyword.CONVOKE)

    spell {
        targets(TargetFilter.Creature, count = 2, minCount = 0)
        effect = Effects.ForEachTarget(Effects.Tap(EffectTarget.ContextTarget(0))) then
            Effects.DistributeCountersAmongTargets(2, CounterType.STUN, minPerTarget = 0)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Anato Finnstark"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/140cd8bd-b74d-4cb1-b78f-8de3cbd878ff.jpg?1783917023"
        ruling(
            "2023-04-14",
            "The two stun counters can end up on the same creature, or they can each end up on a " +
                "different one of the target creatures."
        )
        ruling(
            "2023-04-14",
            "If you choose two target creatures, but one of them is an illegal target as Wicked " +
                "Slumber resolves, the illegal target will be unaffected. You'll put both stun " +
                "counters on the remaining legal target. If both targets become illegal, Wicked " +
                "Slumber won't resolve at all."
        )
        ruling("2023-04-14", "Wicked Slumber can target creatures that are already tapped.")
    }
}
