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
 * Fearless Skald
 * {4}{R}
 * Creature — Dwarf Berserker
 * 3/2
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * Double strike
 *
 * Backup is an enters trigger marked `isBackup` (CR 702.165): the counter always lands; flying is
 * granted only when the target isn't this creature, which already has it printed.
 */
val FearlessSkald = card("Fearless Skald") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Berserker"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following ability until end of turn.)\n" +
        "Double strike"
    power = 3
    toughness = 2

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Slawomir Maniak"
        flavorText = "\"Hold nothing back, my friends. Tonight we feast in Starnheim!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e317d1d-3f20-4f0d-8b1e-31df351e8f83.jpg?1783916992"
    }
}
