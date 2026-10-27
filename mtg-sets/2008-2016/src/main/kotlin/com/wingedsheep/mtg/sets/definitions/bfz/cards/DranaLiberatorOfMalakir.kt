package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Drana, Liberator of Malakir
 * {1}{B}{B}
 * Legendary Creature — Vampire Ally
 * 2/3
 * Flying, first strike
 * Whenever Drana deals combat damage to a player, put a +1/+1 counter on each attacking creature
 * you control.
 *
 * The group is read as the trigger resolves, so it covers whichever attackers survived the
 * first-strike damage step — and those without first strike hit harder in the regular step.
 */
val DranaLiberatorOfMalakir = card("Drana, Liberator of Malakir") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Vampire Ally"
    oracleText = "Flying, first strike\nWhenever Drana deals combat damage to a player, put a +1/+1 " +
        "counter on each attacking creature you control."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl().attacking()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "109"
        artist = "Mike Bierek"
        flavorText = "\"I will not live as a slave. If you would be free, then fight alongside me.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3f6ed18-eb6f-4d07-8660-cf7c074c87cd.jpg?1783938202"
        ruling("2015-08-25", "Drana's last ability triggers after combat damage has been dealt, so only attacking creatures that survive the combat damage step in which Drana deals combat damage will have +1/+1 counters put on them.")
        ruling("2015-08-25", "The +1/+1 counter won't change how much damage Drana or any other attacking creature with first strike or double strike deals during that combat damage step. However, the +1/+1 counters that are put on attacking creatures without first strike will affect the damage those creatures deal during the regular combat damage step.")
    }
}
