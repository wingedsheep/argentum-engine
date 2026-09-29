package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Unseal the Necropolis
 * {2}{B}
 * Instant
 *
 * Each player mills three cards. Then you return up to two creature cards from your graveyard to
 * your hand.
 *
 * The return is not targeted — it's a resolution-time choice made after the mill, so creature
 * cards just milled are legal picks. "Up to two" is `chooseUpTo(2)`: choosing zero is legal.
 */
val UnsealTheNecropolis = card("Unseal the Necropolis") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Each player mills three cards. Then you return up to two creature cards from your " +
        "graveyard to your hand. (To mill three cards, a player puts the top three cards of their " +
        "library into their graveyard.)"

    spell {
        effect = Effects.Pipeline {
            mill(3, Player.Each)
            val creatures = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature)
            )
            val chosen = chooseUpTo(
                2,
                from = creatures,
                showAllCards = true,
                prompt = "Return up to two creature cards from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Isis"
        flavorText = "On Amonkhet, Phyrexia found a ready-made army of horrors waiting for a new master."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f9825ac-c186-4a10-b9e3-e73b10f75ed9.jpg?1783916998"
    }
}
