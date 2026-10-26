package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Meren of Clan Nel Toth
 * {2}{B}{G}
 * Legendary Creature — Human Shaman
 * 3/4
 *
 * Whenever another creature you control dies, you get an experience counter.
 * At the beginning of your end step, choose target creature card in your graveyard. If that
 * card's mana value is less than or equal to the number of experience counters you have, return
 * it to the battlefield. Otherwise, put it into your hand.
 *
 * Canonical printing: Commander 2015, the card's earliest real printing. The mana-value check is
 * made as the ability resolves (no choice for the player — ruling 2015-11-04), comparing the
 * target card's mana value with the experience counters you have at that moment.
 */
val MerenOfClanNelToth = card("Meren of Clan Nel Toth") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Shaman"
    power = 3
    toughness = 4
    oracleText = "Whenever another creature you control dies, you get an experience counter.\n" +
        "At the beginning of your end step, choose target creature card in your graveyard. If that card's mana " +
        "value is less than or equal to the number of experience counters you have, return it to the " +
        "battlefield. Otherwise, put it into your hand."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.EXPERIENCE, 1, EffectTarget.Controller)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val t = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.targetManaValue(),
                ComparisonOperator.LTE,
                DynamicAmounts.playerCounterCount(CounterType.EXPERIENCE),
            ),
            then = Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
            otherwise = Effects.Move(t, Zone.HAND, fromZone = Zone.GRAVEYARD),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "49"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f43bf91d-bc08-417f-a549-64919c24052a.jpg?1783938107"
        ruling(
            "2015-11-04",
            "If Meren of Clan Nel Toth leaves the battlefield at the same time as other creatures you control die, " +
                "its first ability will trigger for each of those creatures."
        )
        ruling(
            "2015-11-04",
            "You can't choose to put the creature card into your hand if its mana value is less than or equal to " +
                "the number of experience counters you have as the ability resolves."
        )
        ruling("2015-11-04", "If a creature card in your graveyard has {X} in its mana cost, X is 0.")
    }
}
