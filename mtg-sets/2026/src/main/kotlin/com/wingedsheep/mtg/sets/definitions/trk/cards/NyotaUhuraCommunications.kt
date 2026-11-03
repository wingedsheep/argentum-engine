package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Nyota Uhura, Communications
 * {2}{U}
 * Legendary Creature — Human Officer
 * 2/4
 *
 * Hailing Frequencies — As Nyota Uhura enters, choose a number between 0 and 10.
 * Open a Channel — Whenever an opponent casts a spell with mana value equal to the chosen number,
 * you draw a card.
 *
 * "Hailing Frequencies" and "Open a Channel" are flavor words with no rules meaning.
 *
 * The Talion, the Kindly Lord shape: a true as-enters replacement ([EntersWithChoice] of
 * [ChoiceType.NUMBER], 0 and 10 inclusive) recorded under [ChoiceSlot.CHOSEN_NUMBER], read back by
 * [CardPredicate.ManaValueEqualsDynamic] against the spell on the stack — so an `{X}` spell's mana
 * value includes the X it was cast for, and cost reductions don't change it.
 */
val NyotaUhuraCommunications = card("Nyota Uhura, Communications") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Officer"
    power = 2
    toughness = 4
    oracleText = "Hailing Frequencies — As Nyota Uhura enters, choose a number between 0 and 10.\n" +
        "Open a Channel — Whenever an opponent casts a spell with mana value equal to the chosen " +
        "number, you draw a card."

    replacementEffect(
        EntersWithChoice(choiceType = ChoiceType.NUMBER, minValue = 0, maxValue = 10)
    )

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(
            GameObjectFilter.Any.copy(
                cardPredicates = listOf(
                    CardPredicate.ManaValueEqualsDynamic(
                        DynamicAmounts.castChoice(ChoiceSlot.CHOSEN_NUMBER)
                    )
                )
            )
        )
        effect = Effects.DrawCards(1)
        description = "Whenever an opponent casts a spell with mana value equal to the chosen " +
            "number, you draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Charles Chaisson"
        flavorText = "She's fluent in thirty-seven languages and infinitely more insightful than a " +
            "universal translator."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b04066ef-ad43-4b40-b4ae-5d94f154261f.jpg?1792508198"
    }
}
