package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enduring Bondwarden
 * {W}
 * Creature — Human Scout
 * 0/1
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * When this creature dies, put its counters on target creature you control.
 *
 * Backup is an enters trigger marked `isBackup` (CR 702.165); the granted copy of the dies trigger
 * is the same last-known-counters move the Bondwarden prints.
 */
val EnduringBondwarden = card("Enduring Bondwarden") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following ability until end of turn.)\n" +
        "When this creature dies, put its counters on target creature you control."
    power = 0
    toughness = 1

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantTriggeredAbility(
                    ability = grantedTriggeredAbility {
                        trigger = Triggers.self.dies()
                        val recipient = target(TargetFilter.CreatureYouControl)
                        effect = Effects.MoveAllLastKnownCounters(recipient)
                        description = "When this creature dies, put its counters on target creature you control."
                    },
                    target = creature,
                ),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val recipient = target(TargetFilter.CreatureYouControl)
        effect = Effects.MoveAllLastKnownCounters(recipient)
        description = "When this creature dies, put its counters on target creature you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Kim Sokol"
        flavorText = "\"I will be with you. Always.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb0680eb-ceca-44d9-9654-78ea2ccfce17.jpg?1783917065"
    }
}
