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
 * Saiba Cryptomancer
 * {1}{U}
 * Creature — Moonfolk Ninja
 * 0/1
 * Flash
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * Hexproof
 *
 * Flash sits above backup, so only hexproof is granted — and only when the target isn't this creature.
 */
val SaibaCryptomancer = card("Saiba Cryptomancer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Moonfolk Ninja"
    oracleText = "Flash\n" +
        "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following ability until end of turn.)\n" +
        "Hexproof"
    power = 0
    toughness = 1

    keywords(Keyword.FLASH, Keyword.HEXPROOF)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.HEXPROOF, creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Aaron J. Riley"
        flavorText = "\"Good luck getting through a class-seven proxy ward, creep.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/9/3934d535-740d-471a-bbf1-c3b26b1cd596.jpg?1783917025"
    }
}
