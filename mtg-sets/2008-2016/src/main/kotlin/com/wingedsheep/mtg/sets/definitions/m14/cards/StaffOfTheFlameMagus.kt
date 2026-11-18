package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Staff of the Flame Magus
 * {3}
 * Artifact
 *
 * Whenever you cast a red spell or a Mountain you control enters, you gain 1 life.
 *
 * One triggered ability with two trigger events, so it is a single `Triggers.or` over the cast
 * atom and the enters atom. "Mountain" is the land *type* (any land with that subtype counts, per the
 * 2014-07-18 ruling), which is what [Filters.MountainCard] matches. Casting a copy of a card counts; a copy
 * put onto the stack without being cast does not, so the cast atom is `casts`, not `castsOrCopies`.
 */
val StaffOfTheFlameMagus = card("Staff of the Flame Magus") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you cast a red spell or a Mountain you control enters, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.you.casts(GameObjectFilter.Any.withColor(Color.RED)),
            Triggers.a(Filters.MountainCard.youControl()).enters(),
        )
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Daniel Ljunggren"
        flavorText = "A symbol of passion in indifferent times."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d6befbd-4fe3-4338-b8ea-13b8b70a7664.jpg?1783939893"
        ruling(
            "2014-07-18",
            "Each of these artifacts cares about any land with the appropriate basic land type. For example, Staff of the Sun Magus's ability triggers when any land with the subtype Plains enters the battlefield under your control, not just lands named Plains."
        )
        ruling(
            "2014-07-18",
            "Most nonbasic lands don't have basic land types, even if they produce colored mana. For example, Caves of Koilos is neither a Plains nor a Swamp. Having one enter the battlefield under your control won't cause Staff of the Sun Magus's ability to trigger."
        )
        ruling(
            "2014-07-18",
            "If you cast a copy of a card that's a certain color, the appropriate Staff's ability will trigger. However, if you copy a spell on the stack without casting that copy, it will not."
        )
    }
}
