package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Archpriest of Shadows
 * {3}{B}{B}
 * Creature — Human Warlock
 * 4/4
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following abilities until end of turn.)
 * Deathtouch
 * Whenever this creature deals combat damage to a player or battle, return target creature card
 * from your graveyard to the battlefield.
 */
val ArchpriestOfShadows = card("Archpriest of Shadows") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following abilities until end of turn.)\n" +
        "Deathtouch\n" +
        "Whenever this creature deals combat damage to a player or battle, return target creature " +
        "card from your graveyard to the battlefield."
    power = 4
    toughness = 4

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature) then
                    Effects.GrantTriggeredAbility(
                        ability = grantedTriggeredAbility {
                            trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
                            val reanimated = target(TargetFilter.CreatureInYourGraveyard)
                            effect = Effects.PutOntoBattlefield(reanimated)
                        },
                        target = creature,
                    ),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following abilities until end of turn.)"
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
        val reanimated = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefield(reanimated)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Fariba Khamseh"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5931746-a55c-4528-9e4a-ee15edab3489.jpg?1783917018"
    }
}
