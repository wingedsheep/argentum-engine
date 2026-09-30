package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard

/**
 * Six
 * {2}{G}
 * Legendary Creature — Treefolk
 * 2/4
 *
 * Reach
 * Whenever Six attacks, mill three cards. You may put a land card from among them into your hand.
 * During your turn, nonland permanent cards in your graveyard have retrace.
 *
 * Retrace (CR 702.81a) is "you may cast this card from your graveyard by discarding a land card as
 * an additional cost to cast it", so the grant is a during-your-turn graveyard-cast permission
 * over nonland permanent cards that owes that discard.
 */
val Six = card("Six") {
    manaCost = "{2}{G}"
    typeLine = "Legendary Creature — Treefolk"
    power = 2
    toughness = 4
    oracleText = "Reach\n" +
        "Whenever Six attacks, mill three cards. You may put a land card from among them into your hand.\n" +
        "During your turn, nonland permanent cards in your graveyard have retrace. (You may cast " +
        "permanent cards from your graveyard by discarding a land card in addition to paying their " +
        "other costs.)"

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val milled = mill(3)
            val selected = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Land,
                showAllCards = true,
                prompt = "You may put a land card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(selected)
        }
    }

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.NonlandPermanent,
            duringYourTurnOnly = true,
            additionalCost = Costs.additional.DiscardCards(1, GameObjectFilter.Land)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9246b68-580f-4f53-883d-7900880e4b0d.jpg?1783911256"
        ruling(
            "2024-06-07",
            "If a spell you cast with retrace is countered, it's put back into your graveyard. You " +
                "may use the retrace ability to cast it again."
        )
    }
}
