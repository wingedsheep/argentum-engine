package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Westgate Regent
 * {3}{B}{B}
 * Creature — Vampire
 * 4/4
 * Flying
 * Ward—Discard a card.
 * Whenever this creature deals combat damage to a player, put that many +1/+1 counters on it.
 */
val WestgateRegent = card("Westgate Regent") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Flying\n" +
        "Ward—Discard a card. (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player discards a card.)\n" +
        "Whenever this creature deals combat damage to a player, put that many +1/+1 counters on it."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Ward(WardCost.Discard()))

    // Whenever this creature deals combat damage to a player, put that many +1/+1 counters on it.
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.triggerDamageAmount(),
            EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Mike Jordana"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e757cc7d-b9cc-4ef1-80c9-a22755a6d271.jpg?1783926487"
    }
}
