package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Basri's Lieutenant
 * {3}{W}
 * Creature — Human Knight
 * 3/4
 *
 * Vigilance, protection from multicolored
 * When this creature enters, put a +1/+1 counter on target creature you control.
 * Whenever this creature or another creature you control dies, if it had a +1/+1 counter on it,
 * create a 2/2 white Knight creature token with vigilance.
 *
 * "This creature or another creature you control" is `Triggers.a(creature you control)`, which
 * includes the source itself. The "if it had a +1/+1 counter on it" clause is an intervening-if
 * over last-known information (Grakmaw, Skyclave Ravager's shape): the dying creature's counters
 * are read off the dies event (the creature as it last existed on the battlefield), and the
 * trigger fires once regardless of how many counters it had.
 */
val BasrisLieutenant = card("Basri's Lieutenant") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 4
    oracleText = "Vigilance, protection from multicolored\n" +
        "When this creature enters, put a +1/+1 counter on target creature you control.\n" +
        "Whenever this creature or another creature you control dies, if it had a +1/+1 counter on it, " +
        "create a 2/2 white Knight creature token with vigilance."

    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Multicolored))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "When this creature enters, put a +1/+1 counter on target creature you control."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.lastKnownPlusOneCounters(),
            ComparisonOperator.GTE,
            1
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/0/7/076f934b-a244-45f1-bcb3-7c5e882e9911.jpg?1783930592"
        )
        description = "Whenever this creature or another creature you control dies, if it had a +1/+1 counter " +
            "on it, create a 2/2 white Knight creature token with vigilance."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74b1eae0-1bf8-4922-a9e3-45c01ece9005.jpg?1783930745"
        ruling("2020-06-23", "Basri's Lieutenant can be the target of its own enters-the-battlefield triggered ability.")
        ruling("2020-06-23", "The second triggered ability of Basri's Lieutenant triggers just once if the dying creature has +1/+1 counters on it, no matter how many of those counters it has.")
        ruling("2020-06-23", "If Basri's Lieutenant dies at the same time as other creatures, its last ability will trigger for any of the creatures that had a +1/+1 counter on it, including itself.")
        ruling("2020-06-23", "If a creature with +1/+1 counters on it receives a greater or equal number of -1/-1 counters and this causes it to be destroyed by lethal damage or put into its owner's graveyard for having 0 or less toughness, the last ability triggers. This is because the ability checks the creature as it last existed on the battlefield, and it still had those counters on it at that point.")
    }
}
