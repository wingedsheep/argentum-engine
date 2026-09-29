package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Doomskar Warrior
 * {2}{G}{G}
 * Creature — Human Warrior
 * 4/3
 * Backup 1
 * Trample
 * Whenever this creature deals combat damage to a player or battle, look at that many cards from
 * the top of your library. You may reveal a creature or land card from among them and put it into
 * your hand. Put the rest on the bottom of your library in a random order.
 *
 * "That many" is the combat damage dealt (`triggerDamageAmount`), feeding the Vivien Reid dig
 * pipeline. Backup grants both trample and the dig trigger to another creature until end of turn.
 */
private fun doomskarDig(): Effect = Patterns.Library.lookAtTopRevealMatchingToHand(
    count = DynamicAmounts.triggerDamageAmount(),
    filter = GameObjectFilter.CreatureOrLand,
    prompt = "You may reveal a creature or land card to put into your hand",
)

val DoomskarWarrior = card("Doomskar Warrior") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following abilities until end of turn.)\n" +
        "Trample\n" +
        "Whenever this creature deals combat damage to a player or battle, look at that many cards " +
        "from the top of your library. You may reveal a creature or land card from among them and put " +
        "it into your hand. Put the rest on the bottom of your library in a random order."
    power = 4
    toughness = 3

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
                    Effects.GrantTriggeredAbility(
                        ability = grantedTriggeredAbility {
                            trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
                            effect = doomskarDig()
                        },
                        target = creature,
                    ),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following abilities until end of turn.)"
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
        effect = doomskarDig()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa1db4e3-e98c-49af-8682-3b7aa20bc31e.jpg?1783916970"
    }
}
