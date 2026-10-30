package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Basri Ket
 * {1}{W}{W}
 * Legendary Planeswalker — Basri
 * Loyalty: 3
 *
 * +1: Put a +1/+1 counter on up to one target creature. It gains indestructible until end of turn.
 * −2: Whenever one or more nontoken creatures attack this turn, create that many 1/1 white Soldier
 * creature tokens that are tapped and attacking.
 * −6: You get an emblem with "At the beginning of combat on your turn, create a 1/1 white Soldier
 * creature token, then put a +1/+1 counter on each creature you control."
 *
 * The −2 is a turn-bounded, repeating delayed trigger on any player's attack declaration that
 * counts only nontoken attackers: the trigger freezes the attackers it counted, so ones that leave
 * combat before it resolves still count and attacking tokens neither trigger it nor add to it
 * (ruling). Each Soldier's defender is the controller's choice (ruling — CONTROLLER_CHOOSES).
 */
val BasriKet = card("Basri Ket") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Basri"
    startingLoyalty = 3
    oracleText = "+1: Put a +1/+1 counter on up to one target creature. It gains indestructible until end of turn.\n" +
        "−2: Whenever one or more nontoken creatures attack this turn, create that many 1/1 white Soldier " +
        "creature tokens that are tapped and attacking.\n" +
        "−6: You get an emblem with \"At the beginning of combat on your turn, create a 1/1 white Soldier " +
        "creature token, then put a +1/+1 counter on each creature you control.\""

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.anyPlayer.attacks(with = GameObjectFilter.Creature.nontoken()),
            effect = Effects.CreateToken(
                count = DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured),
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier"),
                tapped = true,
                attacking = true,
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES
            )
        )
    }

    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Soldier")
                ) then Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
                )
                description = "At the beginning of combat on your turn, create a 1/1 white Soldier " +
                    "creature token, then put a +1/+1 counter on each creature you control."
            },
            emblem = true,
            descriptionOverride = "At the beginning of combat on your turn, create a 1/1 white Soldier " +
                "creature token, then put a +1/+1 counter on each creature you control."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "7"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98c85699-2daf-4e87-a3be-465d02bd64bb.jpg?1783930745"
        ruling("2020-06-23", "The delayed triggered ability of Basri's second ability triggers if any nontoken creatures attack, and it counts how many nontoken creatures attacked. Attacking nontoken creatures that leave combat before the triggered ability resolves are counted. Token creatures that are attacking don't stop the ability from triggering and aren't counted.")
        ruling("2020-06-23", "You choose which player or planeswalker each Soldier token is attacking. They don't have to be attacking the same players or planeswalkers that the nontoken creatures are attacking, and they don't have to be attacking the same player or planeswalker as each other.")
        ruling("2020-06-23", "Although the Soldiers are attacking creatures, they were never declared as attacking creatures. This means that abilities that trigger whenever a creature attacks won't trigger when the Soldiers enter the battlefield attacking.")
        ruling("2020-06-23", "The Soldier token created by Basri's last ability can't attack during the turn you create it unless it gains haste somehow.")
    }
}
