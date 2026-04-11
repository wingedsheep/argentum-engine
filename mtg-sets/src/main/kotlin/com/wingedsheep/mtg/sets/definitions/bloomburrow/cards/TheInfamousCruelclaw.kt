package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithAdditionalCostEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect

/**
 * The Infamous Cruelclaw
 * {1}{B}{R}
 * Legendary Creature — Weasel Mercenary
 * 3/3
 *
 * Menace
 * Whenever The Infamous Cruelclaw deals combat damage to a player, exile cards
 * from the top of your library until you exile a nonland card. You may cast that
 * card by discarding a card rather than paying its mana cost.
 */
val TheInfamousCruelclaw = card("The Infamous Cruelclaw") {
    manaCost = "{1}{B}{R}"
    typeLine = "Legendary Creature — Weasel Mercenary"
    power = 3
    toughness = 3
    oracleText = "Menace\n" +
        "Whenever The Infamous Cruelclaw deals combat damage to a player, exile cards from the top of your library until you exile a nonland card. You may cast that card by discarding a card rather than paying its mana cost."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = CompositeEffect(
            listOf(
                // Exile from top until nonland
                GatherUntilMatchEffect(
                    filter = GameObjectFilter.Nonland,
                    storeMatch = "nonland",
                    storeRevealed = "allRevealed"
                ),
                RevealCollectionEffect(from = "allRevealed"),
                // Move all revealed cards to exile
                MoveCollectionEffect(
                    from = "allRevealed",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                // Grant permission to play the nonland from exile
                GrantMayPlayFromExileEffect("nonland"),
                // Waive mana cost
                GrantPlayWithoutPayingCostEffect("nonland"),
                // Require discarding a card as additional cost
                GrantPlayWithAdditionalCostEffect(
                    from = "nonland",
                    additionalCost = AdditionalCost.DiscardCards(1)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "219"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc6c9196-6d28-4cc2-9748-60e9632a502b.jpg?1721427092"
        ruling("2024-07-26", "You cast the exiled card while the ability is resolving and still on the stack. You can't wait to cast it later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2024-07-26", "Since you are using an alternative cost to cast the spell, you can't pay any other alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, you must pay those.")
        ruling("2024-07-26", "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X.")
        ruling("2024-07-26", "If you choose not to cast the exiled card, it remains in exile.")
    }
}
