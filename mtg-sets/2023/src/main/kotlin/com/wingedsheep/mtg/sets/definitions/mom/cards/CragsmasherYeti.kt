package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cragsmasher Yeti
 * {4}{R}{R}
 * Creature — Yeti
 * 4/2
 * Mountaincycling {2}
 * Backup 2
 * Trample
 */
val CragsmasherYeti = card("Cragsmasher Yeti") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Yeti"
    oracleText = "Mountaincycling {2} ({2}, Discard this card: Search your library for a Mountain card, reveal it, " +
        "put it into your hand, then shuffle.)\n" +
        "Backup 2 (When this creature enters, put two +1/+1 counters on target creature. If that's another " +
        "creature, it gains the following ability until end of turn.)\nTrample"
    power = 4
    toughness = 2

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.typecycling("Mountain", "{2}"))

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.TRAMPLE, creature),
            )
        description = "Backup 2 (When this creature enters, put two +1/+1 counters on target creature. If that's " +
            "another creature, it gains the following ability until end of turn.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "333"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee7a4ccb-c0c9-491f-9475-e78895fa9d03.jpg?1783916901"
    }
}
