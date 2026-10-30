package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Inspire Awe
 * {3}{G}
 * Instant
 * Prevent all combat damage that would be dealt this turn except combat damage that would be dealt
 * by enchanted creatures and enchantment creatures. Scry 2.
 *
 * A filtered Fog (the Tanglesap / Moonmist shield): combat damage is prevented from every creature
 * that is neither enchanted nor an enchantment. The source filter is re-read against projected
 * state each time combat damage would be dealt, so — per the ruling — a creature that becomes
 * enchanted (or stops being an enchantment) later in the turn is judged as it is then. The scry
 * happens as the spell resolves.
 */
val InspireAwe = card("Inspire Awe") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt this turn except combat damage that " +
        "would be dealt by enchanted creatures and enchantment creatures. Scry 2."

    spell {
        effect = Effects.PreventCombatDamageFrom(
            GameObjectFilter.Creature
                .withStatePredicate(StatePredicate.Not(StatePredicate.IsEnchanted))
                .withCardPredicate(CardPredicate.Not(CardPredicate.IsEnchantment))
        ) then Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Ekaterina Burmak"
        flavorText = "Nylea appeared amid the howl of a hundred beasts."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b23c8b5a-2e18-4dd8-b236-5b1fd356f867.jpg?1783931537"
        ruling("2020-01-24", "You scry 2 while Inspire Awe is resolving, not after damage is prevented.")
        ruling(
            "2020-01-24",
            "Inspire Awe doesn't modify the characteristics of creatures, so check immediately before damage " +
                "would be dealt whether a creature is enchanted or also an enchantment in order to determine " +
                "if its combat damage should be prevented."
        )
    }
}
