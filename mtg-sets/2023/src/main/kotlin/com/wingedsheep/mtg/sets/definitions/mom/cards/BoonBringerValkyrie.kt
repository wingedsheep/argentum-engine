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
 * Boon-Bringer Valkyrie
 * {3}{W}{W}
 * Creature — Angel Warrior
 * 4/4
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following abilities until end of turn.)
 * Flying, first strike, lifelink
 *
 * Backup is an enters trigger marked `isBackup` (CR 702.165): the counter always lands; the grant
 * only when the target isn't this creature, which already has the abilities printed.
 */
val BoonBringerValkyrie = card("Boon-Bringer Valkyrie") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Warrior"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following abilities until end of turn.)\n" +
        "Flying, first strike, lifelink"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.LIFELINK)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.FLYING, creature) then
                    Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature) then
                    Effects.GrantKeyword(Keyword.LIFELINK, creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following abilities until end of turn.)"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Heonhwa"
        flavorText = "Her blades were twin beacons, calling the worthy to glory and the wretched to despair."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84e1cc7d-e645-4b0e-b117-63f93528ed12.jpg?1783917067"
    }
}
