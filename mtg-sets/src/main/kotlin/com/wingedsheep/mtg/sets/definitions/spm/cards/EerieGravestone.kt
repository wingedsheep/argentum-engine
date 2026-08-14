package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Eerie Gravestone
 * {2}
 * Artifact
 *
 * When this artifact enters, draw a card.
 * {1}{B}, Sacrifice this artifact: Mill four cards. You may put a creature card from among
 * them into your hand. (To mill four cards, put the top four cards of your library into your
 * graveyard.)
 *
 * Implementation:
 *  - ETB trigger = [Effects.DrawCards]`(1)`.
 *  - Activated ability cost = {1}{B} + [Costs.SacrificeSelf]; effect composes the mill+pick
 *    pipeline (same shape as Cache Grab): gather top 4 → move to graveyard, then a
 *    "you may put a creature card from among them" [SelectFromCollectionEffect] filtered to
 *    [GameObjectFilter.Creature] and moved to hand.
 */
val EerieGravestone = card("Eerie Gravestone") {
    manaCost = "{2}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw a card.\n" +
        "{1}{B}, Sacrifice this artifact: Mill four cards. You may put a creature card from " +
        "among them into your hand. (To mill four cards, put the top four cards of your " +
        "library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
        description = "When this artifact enters, draw a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.SacrificeSelf)
        effect = Effects.Composite(
            // Mill four: gather top 4, move to graveyard.
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4), isMill = true),
                storeAs = "milled"
            ),
            MoveCollectionEffect(
                from = "milled",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            ),
            // You may put a creature card from among them into your hand.
            SelectFromCollectionEffect(
                from = "milled",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter.Creature,
                storeSelected = "selected",
                showAllCards = true,
                prompt = "You may put a creature card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            ),
            MoveCollectionEffect(
                from = "selected",
                destination = CardDestination.ToZone(Zone.HAND)
            )
        )
        description = "{1}{B}, Sacrifice this artifact: Mill four cards. You may put a creature " +
            "card from among them into your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Lordigan"
        flavorText = "\"You've murdered a mask, but you haven't murdered a man.\"\n—Peter Parker"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7675e91f-dba7-4e64-a7ff-1dd56665a4cc.jpg?1783905305"
    }
}
