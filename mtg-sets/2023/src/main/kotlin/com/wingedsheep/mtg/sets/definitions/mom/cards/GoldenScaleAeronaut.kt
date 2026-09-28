package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Golden-Scale Aeronaut
 * {4}{W}
 * Creature — Dwarf Pilot
 * 2/3
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * Flying
 *
 * Backup is an enters trigger marked `isBackup` (CR 702.165): the counter always lands; flying is
 * granted only when the target isn't this creature, which already has it printed.
 */
val GoldenScaleAeronaut = card("Golden-Scale Aeronaut") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Pilot"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following ability until end of turn.)\n" +
        "Flying"
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.FLYING, creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Javier Charro"
        flavorText = "Johar had never heard of a pterodon. He just knew Saheeli's new invention let him " +
            "fly circles around the invading monsters."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b2f3c52-8d6a-411a-aa62-cbb08a144351.jpg?1783917064"
    }
}
