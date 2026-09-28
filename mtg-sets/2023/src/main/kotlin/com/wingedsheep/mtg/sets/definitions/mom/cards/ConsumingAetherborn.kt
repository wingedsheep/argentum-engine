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
 * Consuming Aetherborn
 * {3}{B}
 * Creature — Aetherborn Vampire
 * 2/2
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * Lifelink
 *
 * Backup is an enters trigger marked `isBackup` (CR 702.165): the counter always lands; the grant
 * only when the target isn't this creature, which already has lifelink.
 */
val ConsumingAetherborn = card("Consuming Aetherborn") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Aetherborn Vampire"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following ability until end of turn.)\n" +
        "Lifelink"
    power = 2
    toughness = 2

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.LIFELINK, creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Aldo Domínguez"
        flavorText = "Doji found the Phyrexian's essence foul, but every drop added an hour to their life."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7311ade8-eb75-40f8-b018-668762aa3b77.jpg?1783917015"
    }
}
