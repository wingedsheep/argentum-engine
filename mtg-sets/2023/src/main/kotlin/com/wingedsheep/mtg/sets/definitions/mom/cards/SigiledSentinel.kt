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
 * Sigiled Sentinel
 * {2}{W}
 * Creature — Human Knight
 * 2/2
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * Vigilance
 *
 * Backup is an enters trigger marked `isBackup` (CR 702.165): the counter always lands; vigilance
 * is granted only when the target isn't this creature, which already has it printed.
 */
val SigiledSentinel = card("Sigiled Sentinel") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following ability until end of turn.)\n" +
        "Vigilance"
    power = 2
    toughness = 2

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.VIGILANCE, creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Volkan Baǵa"
        flavorText = "\"Alara has been broken before. I will not let it break again.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f65b178-51d6-4dcb-a1cc-4b4dcb2237cd.jpg?1783917048"
    }
}
