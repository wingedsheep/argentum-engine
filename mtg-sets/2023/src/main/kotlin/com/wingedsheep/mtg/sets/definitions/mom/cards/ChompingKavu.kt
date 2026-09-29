package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chomping Kavu
 * {3}{G}
 * Creature — Kavu
 * 3/3
 * Backup 1
 * This creature can't be blocked by creatures with power 2 or less.
 *
 * The backed-up creature gains the evasion as a granted [CantBeBlockedBy] static (read at the
 * point of use by combat's `CantBeBlockedByRule`), expiring at end of turn.
 */
val ChompingKavu = card("Chomping Kavu") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Kavu"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another " +
        "creature, it gains the following ability until end of turn.)\n" +
        "This creature can't be blocked by creatures with power 2 or less."
    power = 3
    toughness = 3

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantStaticAbility(CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2)), creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's " +
            "another creature, it gains the following ability until end of turn.)"
    }

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "John Tedrick"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31c249d9-37c0-451d-866b-e834c4c57214.jpg?1783916974"
        ruling("2023-04-14", "Once Chomping Kavu has been legally blocked by a creature, changing that creature's power to 2 or less won't undo that block.")
    }
}
