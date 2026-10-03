package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Monstrous Vortex
 * {3}{G}
 * Enchantment
 * Whenever you cast a creature spell with power 5 or greater, discover X, where X is that
 * spell's mana value.
 *
 * The power check reads the spell on the stack (its printed power, plus anything applying to
 * it there); X is the triggering spell's mana value, snapshotted from the cast event.
 */
val MonstrousVortex = card("Monstrous Vortex") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a creature spell with power 5 or greater, discover X, where X is " +
        "that spell's mana value. (Exile cards from the top of your library until you exile a nonland " +
        "card with that mana value or less. Cast it without paying its mana cost or put it into your " +
        "hand. Put the rest on the bottom in a random order.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.powerAtLeast(5))
        effect = Effects.Discover(DynamicAmounts.triggeringSpellManaValue())
        description = "Whenever you cast a creature spell with power 5 or greater, discover X, " +
            "where X is that spell's mana value."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/162fcdec-bf3c-43ba-9ada-212f7dec5fbd.jpg?1783911259"
        ruling("2024-06-07", "A spell's mana value is determined only by its mana cost. Ignore any alternative costs, additional costs, cost increases, or cost reductions.")
        ruling("2024-06-07", "When you discover, you must exile cards. The only optional part of the ability is whether you cast the exiled card or put it into your hand.")
    }
}
