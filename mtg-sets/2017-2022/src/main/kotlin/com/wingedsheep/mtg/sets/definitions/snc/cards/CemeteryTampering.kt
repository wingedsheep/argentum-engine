package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cemetery Tampering
 * {2}{B}
 * Enchantment
 *
 * Hideaway 5 (When this enchantment enters, look at the top five cards of your library, exile
 * one face down, then put the rest on the bottom in a random order.)
 * At the beginning of your upkeep, you may mill three cards. Then if there are twenty or more
 * cards in your graveyard, you may play the exiled card without paying its mana cost.
 *
 * Hideaway is the same composition as Fight Rigging / Wiretapping. Only the mill is optional;
 * the graveyard check runs after it whether or not the mill was taken.
 */
val CemeteryTampering = card("Cemetery Tampering") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Hideaway 5 (When this enchantment enters, look at the top five cards of your " +
        "library, exile one face down, then put the rest on the bottom in a random order.)\n" +
        "At the beginning of your upkeep, you may mill three cards. Then if there are twenty or " +
        "more cards in your graveyard, you may play the exiled card without paying its mana cost."

    keywordAbility(KeywordAbility.hideaway(5))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val cemeteryTamperingTop = gather(
                CardSource.TopOfLibrary(
                    count = 5,
                    player = Player.You
                )
            )
            val (cemeteryTamperingPicked, cemeteryTamperingRest) = chooseExactlySplit(
                1,
                from = cemeteryTamperingTop,
                prompt = "Choose a card to exile face down",
                selectedLabel = "Exile face down",
                remainderLabel = "Put on bottom of library"
            )
            exile(cemeteryTamperingPicked, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
            toLibraryBottom(cemeteryTamperingRest, order = CardOrder.Random)
        }
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(Patterns.Library.mill(3)) then
            Effects.If(
                condition = Conditions.CardsInGraveyardAtLeast(20),
                then = Effects.May(
                    Effects.Pipeline {
                        val cemeteryTamperingLinked = gather(CardSource.FromLinkedExile())
                        run(Effects.PlayFromCollectionWithoutPayingCost(cemeteryTamperingLinked))
                    },
                    descriptionOverride = "Play the exiled card without paying its mana cost"
                )
            )
        description = "At the beginning of your upkeep, you may mill three cards. Then if there " +
            "are twenty or more cards in your graveyard, you may play the exiled card without " +
            "paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Taras Susak"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c6e8d37-badd-4b9a-94e5-2d50ec78182c.jpg?1783923138"
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
            "Previously, permanents with hideaway entered the battlefield tapped. This ability has " +
                "been removed from the definition of hideaway. Older cards have received errata to " +
                "have an additional paragraph that reads \"[This permanent] enters the battlefield " +
                "tapped,\" and they now have hideaway 4."
        )
        ruling(
            "2022-04-29",
            "Hideaway now causes you to put the rest of the cards on the bottom of your library " +
                "in a random order instead of any order."
        )
    }
}
