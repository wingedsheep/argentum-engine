package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Phalanx Tactics
 * {1}{W}
 * Instant
 *
 * Target creature you control gets +2/+1 until end of turn. Each other creature you control gets
 * +1/+1 until end of turn.
 *
 * The target is pumped directly; every *other* creature you control (`otherThanTarget()`) is found
 * as the spell resolves and gets +1/+1.
 */
val PhalanxTactics = card("Phalanx Tactics") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +2/+1 until end of turn. " +
        "Each other creature you control gets +1/+1 until end of turn."

    spell {
        val leader = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(2, 1, leader) then
            Patterns.Group.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl.otherThanTarget())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Bayard Wu"
        flavorText = "Every soldier has a place to stand and a role to play."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f114a10-0b00-4f3a-bd73-d4c791fbf4d5.jpg?1783931592"
    }
}
