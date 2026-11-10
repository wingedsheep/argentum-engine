package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Might Beyond Reason — Shadows over Innistrad #217
 * {3}{G} · Instant
 *
 * Put two +1/+1 counters on target creature.
 * Delirium — Put three +1/+1 counters on that creature instead if there are four or more card
 * types among cards in your graveyard.
 *
 * Delirium is checked as the spell resolves, while the spell is still on the stack, so it never
 * counts its own instant type. The upgrade replaces the base effect — three counters, not five.
 */
val MightBeyondReason = card("Might Beyond Reason") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put two +1/+1 counters on target creature.\nDelirium — Put three +1/+1 counters on " +
        "that creature instead if there are four or more card types among cards in your graveyard."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.Delirium(),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, t),
            otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, t),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "217"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a274689-82f1-488a-baf8-cd8ef3477b72.jpg?1783937725"
        ruling(
            "2016-04-08",
            "Some delirium abilities that appear on instants and sorceries use the word \"instead.\" These " +
                "spells have an upgraded effect when they resolve if there are four or more card types among " +
                "cards in your graveyard. They check that number only while they're resolving and don't count " +
                "themselves, since they aren't in your graveyard yet. You only get the upgraded effect, not both effects."
        )
    }
}
