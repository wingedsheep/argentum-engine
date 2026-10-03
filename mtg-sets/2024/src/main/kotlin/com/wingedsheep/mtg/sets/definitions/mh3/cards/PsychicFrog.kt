package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Psychic Frog — Modern Horizons 3 #199
 * {U}{B} · Creature — Frog · Rare · 1/2
 *
 * Whenever this creature deals combat damage to a player or planeswalker, draw a card.
 * Discard a card: Put a +1/+1 counter on this creature.
 * Exile three cards from your graveyard: This creature gains flying until end of turn.
 */
val PsychicFrog = card("Psychic Frog") {
    manaCost = "{U}{B}"
    typeLine = "Creature — Frog"
    power = 1
    toughness = 2
    oracleText = "Whenever this creature deals combat damage to a player or planeswalker, draw a card.\n" +
        "Discard a card: Put a +1/+1 counter on this creature.\n" +
        "Exile three cards from your graveyard: This creature gains flying until end of turn."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker)
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Discard a card: Put a +1/+1 counter on this creature."
    }

    activatedAbility {
        cost = Costs.ExileFromGraveyard(3)
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
        description = "Exile three cards from your graveyard: This creature gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Pete Venters"
        flavorText = "Some frogs poison the body. Some frogs poison the mind."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68924203-c3d9-41ce-8ca8-c6dd491eb3ca.jpg?1783911246"
    }
}
