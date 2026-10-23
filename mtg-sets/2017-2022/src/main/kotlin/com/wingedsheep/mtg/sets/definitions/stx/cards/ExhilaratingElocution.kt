package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Exhilarating Elocution — Strixhaven: School of Mages #185 (canonical printing)
 * {2}{W}{B} · Sorcery
 *
 * Put two +1/+1 counters on target creature you control. Other creatures you control get +1/+1
 * until end of turn.
 *
 * Counters on the target, then the Phalanx Tactics group pump over every creature you control
 * except the target (`otherThanTarget()`), found as the spell resolves.
 */
val ExhilaratingElocution = card("Exhilarating Elocution") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Put two +1/+1 counters on target creature you control. Other creatures you " +
        "control get +1/+1 until end of turn."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature) then
            Patterns.Group.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl.otherThanTarget())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Randy Vargas"
        flavorText = "His words poured forth on waves of golden light, scouring all fear from his audience's hearts and minds."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8468417d-7ef5-43e0-9190-e88f3eed9e82.jpg?1783927315"
    }
}
