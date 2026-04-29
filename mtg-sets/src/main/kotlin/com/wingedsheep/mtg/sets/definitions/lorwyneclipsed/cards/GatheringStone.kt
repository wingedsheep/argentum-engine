package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Gathering Stone
 * {4}
 * Artifact
 *
 * As this artifact enters, choose a creature type.
 * Spells you cast of the chosen type cost {1} less to cast.
 * When this artifact enters and at the beginning of your upkeep, look at the top
 * card of your library. If it's a card of the chosen type, you may reveal it and
 * put it into your hand. If you don't put the card into your hand, you may put
 * it into your graveyard.
 */
val GatheringStone = card("Gathering Stone") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a creature type.\n" +
        "Spells you cast of the chosen type cost {1} less to cast.\n" +
        "When this artifact enters and at the beginning of your upkeep, look at the top card of your library. " +
        "If it's a card of the chosen type, you may reveal it and put it into your hand. " +
        "If you don't put the card into your hand, you may put it into your graveyard."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ReduceSpellCostByFilter(
            filter = GameObjectFilter.Any.withChosenSubtype(),
            amount = 1
        )
    }

    val lookAtTopEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "looked"
            ),
            FilterCollectionEffect(
                from = "looked",
                filter = CollectionFilter.MatchesFilter(GameObjectFilter.Any.withChosenSubtype()),
                storeMatching = "matchingCard",
                storeNonMatching = "nonMatchingCard"
            ),
            // Matching card: may reveal and put in hand
            SelectFromCollectionEffect(
                from = "matchingCard",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "toHand",
                storeRemainder = "matchKept",
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Don't put into your hand"
            ),
            MoveCollectionEffect(
                from = "toHand",
                destination = CardDestination.ToZone(Zone.HAND),
                revealed = true,
                revealToSelf = false
            ),
            // Matching card you didn't put in hand: may put in graveyard
            SelectFromCollectionEffect(
                from = "matchKept",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "matchToGraveyard",
                selectedLabel = "Put into your graveyard",
                remainderLabel = "Leave on top of your library"
            ),
            MoveCollectionEffect(
                from = "matchToGraveyard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            ),
            // Non-matching card: may put in graveyard
            SelectFromCollectionEffect(
                from = "nonMatchingCard",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "nonMatchToGraveyard",
                selectedLabel = "Put into your graveyard",
                remainderLabel = "Leave on top of your library"
            ),
            MoveCollectionEffect(
                from = "nonMatchToGraveyard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = lookAtTopEffect
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = lookAtTopEffect
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "257"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81dfbfe1-143a-4637-b683-a34cfc51993d.jpg?1767732919"

        ruling("2025-11-17", "You don't have to reveal the card you look at with Gathering Stone's third ability even if it's a card of the chosen type. If you choose not to reveal it this way, you can still put it into your graveyard.")
        ruling("2025-11-17", "Gathering Stone's second ability applies only to generic mana in the total cost of spells you cast of the chosen type.")
    }
}
