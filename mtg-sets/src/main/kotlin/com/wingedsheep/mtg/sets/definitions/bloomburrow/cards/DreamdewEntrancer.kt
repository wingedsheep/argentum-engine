package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Dreamdew Entrancer
 * {2}{G}{U}
 * Creature — Frog Wizard
 * 3/4
 *
 * Reach
 * When this creature enters, tap up to one target creature and put three stun
 * counters on it. If you control that creature, draw two cards.
 */
val DreamdewEntrancer = card("Dreamdew Entrancer") {
    manaCost = "{2}{G}{U}"
    typeLine = "Creature — Frog Wizard"
    power = 3
    toughness = 4
    oracleText = "Reach\nWhen this creature enters, tap up to one target creature and put three stun counters on it. If you control that creature, draw two cards."

    keywords(Keyword.REACH)

    // ETB: tap up to one target creature, put 3 stun counters on it,
    // and if you control it, draw 2 cards
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("creature", Targets.UpToCreatures(1))
        effect = Effects.Tap(t)
            .then(Effects.AddCounters("STUN", 3, t))
            .then(ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.youControl(), targetIndex = 0),
                effect = Effects.DrawCards(2)
            ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Zoltan Boros"
        flavorText = "\"Your dreams are too precious to leave to chance.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26bd6b0d-8606-4a37-8be3-a852f1a8e99c.jpg?1721427040"

        ruling("2024-07-26", "If the target creature is an illegal target as Dreamdew Entrancer's last ability tries to resolve, it won't resolve and none of its effects will happen.")
        ruling("2024-07-26", "You may target a creature that is already tapped with Dreamdew Entrancer's last ability. If the target creature is already tapped as the ability resolves, you'll still put three stun counters on it.")
    }
}
