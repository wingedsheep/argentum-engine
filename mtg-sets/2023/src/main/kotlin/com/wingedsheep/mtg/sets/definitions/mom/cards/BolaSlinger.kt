package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bola Slinger
 * {3}{W}
 * Creature — Cat Soldier
 * 2/2
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * Whenever this creature attacks, tap target artifact or creature an opponent controls.
 *
 * Backup (CR 702.165) composes as an enters trigger marked `isBackup`: the counter always lands, and
 * only another creature gains the attack trigger printed below backup until end of turn.
 */
val BolaSlinger = card("Bola Slinger") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following ability until end of turn.)\n" +
        "Whenever this creature attacks, tap target artifact or creature an opponent controls."
    power = 2
    toughness = 2

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantTriggeredAbility(
                    ability = grantedTriggeredAbility {
                        trigger = Triggers.self.attacks()
                        val tapped = target(TargetFilter(GameObjectFilter.CreatureOrArtifact.opponentControls()))
                        effect = Effects.Tap(tapped)
                    },
                    target = creature,
                ),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val tapped = target(TargetFilter(GameObjectFilter.CreatureOrArtifact.opponentControls()))
        effect = Effects.Tap(tapped)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Hendry Iwanaga"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/896043a7-c7a2-4542-8739-7b4f09c6e1f1.jpg?1783917069"
    }
}
