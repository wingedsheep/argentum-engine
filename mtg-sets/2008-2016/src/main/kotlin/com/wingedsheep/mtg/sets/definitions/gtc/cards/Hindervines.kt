package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hindervines
 * {2}{G}
 * Instant
 * Prevent all combat damage that would be dealt this turn by creatures with no +1/+1 counters on them.
 *
 * Modeling notes:
 *  - A source-side combat-damage shield (same shape as Harmless Assault): no recipient is named, so
 *    the damage is prevented wherever it would land.
 *  - The source filter is re-evaluated as each damage instance would be dealt, matching the ruling
 *    that only whether the creature has a +1/+1 counter at that moment matters.
 */
val Hindervines = card("Hindervines") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt this turn by creatures with no +1/+1 counters on them."

    spell {
        effect = Effects.PreventCombatDamageFrom(
            GameObjectFilter.Creature.withoutCounter(CounterType.PLUS_ONE_PLUS_ONE)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Svetlin Velinov"
        flavorText = "\"Woodlands and laws. Both grow faster than the Azorius's ability to manage them.\"\n—Suniel the Woodwise"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6b85a09-6d43-4798-8164-131d35b65836.jpg?1783940117"
        ruling(
            "2013-01-24",
            "Hindervines checks whether a creature has a +1/+1 counter on it at the moment it deals damage. " +
                "It doesn't matter whether a creature had a +1/+1 counter, or was even on the battlefield, when Hindervines resolved."
        )
    }
}
