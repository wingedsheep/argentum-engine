package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani, Adversary of Tyrants
 * {2}{W}{W}
 * Legendary Planeswalker — Ajani
 * Loyalty: 4
 *
 * +1: Put a +1/+1 counter on each of up to two target creatures.
 * −2: Return target creature card with mana value 2 or less from your graveyard to the battlefield.
 * −7: You get an emblem with "At the beginning of your end step, create three 1/1 white Cat
 * creature tokens with lifelink."
 */
val AjaniAdversaryOfTyrants = card("Ajani, Adversary of Tyrants") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 4
    oracleText = "+1: Put a +1/+1 counter on each of up to two target creatures.\n" +
        "−2: Return target creature card with mana value 2 or less from your graveyard to the battlefield.\n" +
        "−7: You get an emblem with \"At the beginning of your end step, create three 1/1 white Cat " +
        "creature tokens with lifelink.\""

    loyaltyAbility(+1) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
    }

    loyaltyAbility(-2) {
        val card = target(
            TargetFilter(GameObjectFilter.Creature.manaValueAtMost(2).ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(card)
    }

    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.END)
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Cat"),
                    keywords = setOf(Keyword.LIFELINK),
                    count = 3,
                    imageUri = "https://cards.scryfall.io/normal/front/b/3/b31f1580-5bba-4cef-b0c8-f2837a597b7d.jpg?1783934470"
                )
                description = "At the beginning of your end step, create three 1/1 white Cat creature " +
                    "tokens with lifelink."
            },
            emblem = true,
            descriptionOverride = "At the beginning of your end step, create three 1/1 white Cat " +
                "creature tokens with lifelink."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c565076-5db2-47ea-8ee0-4a4fd7bb353d.jpg?1783934611"
        ruling("2018-07-13", "If a card in your graveyard has {X} in its mana cost, X is considered to be 0.")
    }
}
