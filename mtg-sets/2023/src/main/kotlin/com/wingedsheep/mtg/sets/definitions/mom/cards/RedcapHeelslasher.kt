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
 * Redcap Heelslasher
 * {3}{R}
 * Creature — Goblin Rogue
 * 2/3
 * Backup 1
 * First strike
 */
val RedcapHeelslasher = card("Redcap Heelslasher") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Rogue"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another " +
        "creature, it gains the following ability until end of turn.)\nFirst strike"
    power = 2
    toughness = 3

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's " +
            "another creature, it gains the following ability until end of turn.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Alexey Kruglov"
        flavorText = "Biffle was delighted to have an excuse to use the nice cutlery he'd liberated from Edgewall."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c7ca435-aa84-4d35-80db-d1c74b878b12.jpg?1783916983"
    }
}
