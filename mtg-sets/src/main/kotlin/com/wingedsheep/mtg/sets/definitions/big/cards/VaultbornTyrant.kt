package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Vaultborn Tyrant
 * {5}{G}{G}
 * Creature — Dinosaur
 * 6/6
 *
 * Trample
 * Whenever this creature or another creature you control with power 4 or
 * greater enters, you gain 3 life and draw a card.
 * When this creature dies, if it's not a token, create a token that's a copy
 * of it, except it's an artifact in addition to its other types.
 */
val VaultbornTyrant = card("Vaultborn Tyrant") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "Whenever this creature or another creature you control with power 4 or greater enters, you gain 3 life and draw a card.\n" +
        "When this creature dies, if it's not a token, create a token that's a copy of it, except it's an artifact in addition to its other types."

    keywords(Keyword.TRAMPLE)

    // "This or another creature you control with power 4+ enters" — ANY binding so self counts.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().powerAtLeast(4),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.Composite(listOf(
            Effects.GainLife(3),
            Effects.DrawCards(1)
        ))
    }

    // When this dies, if it's not a token, create an artifact copy of it.
    triggeredAbility {
        trigger = Triggers.Dies
        triggerCondition = Conditions.SourceMatches(
            GameObjectFilter(cardPredicates = listOf(CardPredicate.IsNontoken))
        )
        effect = CreateTokenCopyOfSourceEffect(
            addCardTypes = setOf("ARTIFACT")
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "20"
        artist = "Loïc Canavaggia"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62b3f560-262b-4bc3-9aef-535fd7082c28.jpg?1770090406"
    }
}
