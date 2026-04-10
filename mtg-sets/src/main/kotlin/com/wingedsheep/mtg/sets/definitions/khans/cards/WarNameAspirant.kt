package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.YouAttackedThisTurn
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * War-Name Aspirant
 * {1}{R}
 * Creature — Human Warrior
 * 2/1
 * Raid — War-Name Aspirant enters the battlefield with a +1/+1 counter on it
 * if you attacked this turn.
 * War-Name Aspirant can't be blocked by creatures with power 1 or less.
 */
val WarNameAspirant = card("War-Name Aspirant") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "Raid — This creature enters with a +1/+1 counter on it if you attacked this turn.\nThis creature can't be blocked by creatures with power 1 or less."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = YouAttackedThisTurn
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "David Gaillet"
        flavorText = "\"No battle means more to a Mardu warrior than the one that earns her war name.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8d1714b-ff65-4c5c-ad21-b469f2c72286.jpg?1562795326"
    }
}
