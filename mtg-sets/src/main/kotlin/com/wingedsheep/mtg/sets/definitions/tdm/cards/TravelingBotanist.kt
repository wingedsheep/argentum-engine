package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
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
 *  2. Partition into land / non-land. Only a land card is actionable.
 *  3. The player may choose the land to reveal + put into hand (ChooseUpTo 1).
 *  4. Any land NOT put into hand may then be put into the graveyard (a second ChooseUpTo 1).
 *  5. Cards left unmoved (a declined land, or a non-land top card) stay on top of the library.
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
        effect = Effects.Composite(
            listOf(
                // Look at the top card of your library (private peek).
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "looked"
                ),
                // Only a land card is actionable.
                FilterCollectionEffect(
                    from = "looked",
                    filter = CollectionFilter.MatchesFilter(GameObjectFilter.Land),
                    storeMatching = "landCard",
                    storeNonMatching = "nonLand"
                ),
                // You may reveal the land and put it into your hand.
                SelectFromCollectionEffect(
                    from = "landCard",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "toHand",
                    storeRemainder = "landNotTaken",
                    selectedLabel = "Reveal and put into your hand",
                    remainderLabel = "Leave on top of your library"
                ),
                MoveCollectionEffect(
                    from = "toHand",
                    destination = CardDestination.ToZone(Zone.HAND),
                    revealed = true,
                    revealToSelf = false
                ),
                // If you don't put the land into your hand, you may put it into your graveyard.
                SelectFromCollectionEffect(
                    from = "landNotTaken",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "toGraveyard",
                    selectedLabel = "Put into your graveyard",
                    remainderLabel = "Leave on top of your library"
                ),
                MoveCollectionEffect(
                    from = "toGraveyard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                )
                // "nonLand" and any declined land stay on top of the library (never gathered out).
            )
        )
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
