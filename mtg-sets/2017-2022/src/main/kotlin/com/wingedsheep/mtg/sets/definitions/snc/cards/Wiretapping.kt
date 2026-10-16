package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawStepFirstCard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wiretapping
 * {4}{U}
 * Enchantment
 *
 * Hideaway 5 (When this enchantment enters, look at the top five cards of your library, exile
 * one face down, then put the rest on the bottom in a random order.)
 * Whenever you draw your first card during each of your draw steps, draw a card. Then if you have
 * nine or more cards in hand, you may play the exiled card without paying its mana cost.
 *
 * Hideaway is the same composition as Fight Rigging. The draw trigger is
 * `Triggers.you.draws(DrawStepFirstCard.ONLY)` — it fires for the first card drawn in each of
 * your draw steps only, so the card Wiretapping draws can't retrigger it.
 */
val Wiretapping = card("Wiretapping") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Hideaway 5 (When this enchantment enters, look at the top five cards of your " +
        "library, exile one face down, then put the rest on the bottom in a random order.)\n" +
        "Whenever you draw your first card during each of your draw steps, draw a card. Then if " +
        "you have nine or more cards in hand, you may play the exiled card without paying its " +
        "mana cost."

    keywordAbility(KeywordAbility.hideaway(5))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val wiretappingTop = gather(
                CardSource.TopOfLibrary(
                    count = 5,
                    player = Player.You
                )
            )
            val (wiretappingPicked, wiretappingRest) = chooseExactlySplit(
                1,
                from = wiretappingTop,
                prompt = "Choose a card to exile face down",
                selectedLabel = "Exile face down",
                remainderLabel = "Put on bottom of library"
            )
            exile(wiretappingPicked, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
            toLibraryBottom(wiretappingRest, order = CardOrder.Random)
        }
    }

    triggeredAbility {
        trigger = Triggers.you.draws(DrawStepFirstCard.ONLY)
        effect = Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.CardsInHandAtLeast(9),
                then = Effects.May(
                    Effects.Pipeline {
                        val wiretappingLinked = gather(CardSource.FromLinkedExile())
                        run(Effects.PlayFromCollectionWithoutPayingCost(wiretappingLinked))
                    },
                    descriptionOverride = "Play the exiled card without paying its mana cost"
                )
            )
        description = "Whenever you draw your first card during each of your draw steps, draw a " +
            "card. Then if you have nine or more cards in hand, you may play the exiled card " +
            "without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/859ed6fc-1f9a-428d-82d8-8e4b5bae6d5a.jpg?1783923136"
        ruling(
            "2022-04-29",
            "\"Hideaway N\" means \"When this permanent enters the battlefield, look at the top N " +
                "cards of your library. Exile one of them face down and put the rest on the bottom " +
                "of your library in a random order. The exiled card gains 'The player who controls " +
                "the permanent that exiled this card may look at this card in the exile zone.'\""
        )
        ruling(
            "2022-04-29",
            "Any player who has controlled a permanent with a hideaway ability since a card was " +
                "exiled with it may look at that card."
        )
        ruling(
            "2022-04-29",
            "Hideaway now causes you to put the rest of the cards on the bottom of your library " +
                "in a random order instead of any order."
        )
    }
}
