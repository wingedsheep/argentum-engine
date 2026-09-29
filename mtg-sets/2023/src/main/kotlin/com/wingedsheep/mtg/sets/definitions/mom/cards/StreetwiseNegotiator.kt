package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Streetwise Negotiator
 * {1}{G}
 * Creature — Cat Citizen
 * 0/2
 * Backup 1
 * This creature assigns combat damage equal to its toughness rather than its power.
 *
 * The printed clause is an unconditional self-scoped [AssignDamageEqualToToughness]. The backed-up
 * creature gains it as the [AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS] flag until end of turn
 * (the same grant Bill the Pony uses), which `CombatDamageUtils` reads unconditionally.
 */
val StreetwiseNegotiator = card("Streetwise Negotiator") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat Citizen"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another " +
        "creature, it gains the following ability until end of turn.)\n" +
        "This creature assigns combat damage equal to its toughness rather than its power."
    power = 0
    toughness = 2

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS, creature, Duration.EndOfTurn),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's " +
            "another creature, it gains the following ability until end of turn.)"
    }

    staticAbility {
        ability = AssignDamageEqualToToughness(
            filter = GroupFilter.source(),
            onlyWhenToughnessGreaterThanPower = false,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Brent Hollowell"
        flavorText = "Like the laws, the rules of engagement always seem to bend in the Brokers' favor."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e83f9565-4c04-449d-b337-0eff3fd0c295.jpg?1783916962"
    }
}
