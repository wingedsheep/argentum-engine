package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gloomfang Mauler
 * {5}{B}{B}
 * Creature — Nightmare
 * 5/5
 * Swampcycling {2}
 * Backup 2 (When this creature enters, put two +1/+1 counters on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * Menace
 *
 * Swampcycling is printed above backup, so only menace is "the following ability" the backup target gains.
 */
val GloomfangMauler = card("Gloomfang Mauler") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare"
    oracleText = "Swampcycling {2} ({2}, Discard this card: Search your library for a Swamp card, " +
        "reveal it, put it into your hand, then shuffle.)\n" +
        "Backup 2 (When this creature enters, put two +1/+1 counters on target creature. If that's " +
        "another creature, it gains the following ability until end of turn.)\n" +
        "Menace"
    power = 5
    toughness = 5

    keywordAbility(KeywordAbility.typecycling("Swamp", ManaCost.parse("{2}")))

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.MENACE, creature),
            )
        description = "Backup 2 (When this creature enters, put two +1/+1 counters on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    keywords(Keyword.MENACE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Denis Zhbankov"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/025a5338-133f-486d-9f73-0896226685c0.jpg?1783917008"
    }
}
