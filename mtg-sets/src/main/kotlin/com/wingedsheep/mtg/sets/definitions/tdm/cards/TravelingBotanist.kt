package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Traveling Botanist
 * {1}{G}
 * Creature — Dog Scout
 * 2/3
 *
 * Whenever this creature becomes tapped, look at the top card of your library. If it's a land
 * card, you may reveal it and put it into your hand. If you don't put the card into your hand,
 * you may put it into your graveyard.
 *
 * Modeled as the standard look-at-top peek pipeline (Gather → Filter → Select → Move):
 *  1. Look at (peek, controller-only) the top card → "looked".
 *  2. Partition out the land — only a land card may go to hand → "landCard".
 *  3. The player may choose the land to reveal + put into hand (ChooseUpTo 1) → "toHand".
 *  4. Compute "notInHand" = looked − toHand: the top card if it was NOT put into hand. Per the
 *     oracle, "if you don't put the card into your hand" refers to the looked-at top card, land
 *     or not — so a non-land top card is eligible for the graveyard too, not just a declined land.
 *  5. The player may put that card into the graveyard (a second ChooseUpTo 1).
 *  6. A card left unmoved (declined at both steps) stays on top of the library.
 */
val TravelingBotanist = card("Traveling Botanist") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dog Scout"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature becomes tapped, look at the top card of your library. " +
        "If it's a land card, you may reveal it and put it into your hand. If you don't put the " +
        "card into your hand, you may put it into your graveyard."

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = Effects.Pipeline {
            // Look at the top card of your library (private peek).
            val looked = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                name = "looked"
            )
            // Only a land card may be put into hand.
            val landCard = filter(
                looked,
                CollectionFilter.MatchesFilter(GameObjectFilter.Land),
                name = "landCard"
            )
            // You may reveal the land and put it into your hand.
            val toHand = chooseUpTo(
                1, from = landCard,
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave on top of your library",
                name = "toHand"
            )
            move(
                toHand,
                destination = CardDestination.ToZone(Zone.HAND),
                revealed = true,
                revealToSelf = false
            )
            // "the card" you didn't put into your hand = the looked-at top card minus whatever
            // went to hand. This is any top card (land or not), not just a declined land.
            val notInHand = filter(
                looked,
                CollectionFilter.ExcludeOtherCollection("toHand"),
                name = "notInHand"
            )
            // If you didn't put the card into your hand, you may put it into your graveyard.
            val toGraveyard = chooseUpTo(
                1, from = notInHand,
                selectedLabel = "Put into your graveyard",
                remainderLabel = "Leave on top of your library",
                name = "toGraveyard"
            )
            move(
                toGraveyard,
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
            // A card declined at both steps stays on top of the library (never gathered out).
        }
        description = "look at the top card of your library. If it's a land card, you may reveal it " +
            "and put it into your hand. If you don't put the card into your hand, you may put it " +
            "into your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Daneen Wilkerson"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/890b11b4-777a-4f1e-8c4d-21c5ebbfb0a2.jpg?1743204626"
    }
}
