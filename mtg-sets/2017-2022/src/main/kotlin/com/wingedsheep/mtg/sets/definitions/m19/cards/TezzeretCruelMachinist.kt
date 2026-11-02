package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tezzeret, Cruel Machinist
 * {4}{U}{U}
 * Legendary Planeswalker — Tezzeret
 * Starting Loyalty: 4
 *
 * +1: Draw a card.
 * 0: Until your next turn, target artifact you control becomes a 5/5 creature in addition to its
 *    other types.
 * −7: Put any number of cards from your hand onto the battlefield face down. They're 5/5 artifact
 *     creatures.
 */
val TezzeretCruelMachinist = card("Tezzeret, Cruel Machinist") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Tezzeret"
    startingLoyalty = 4
    oracleText = "+1: Draw a card.\n" +
        "0: Until your next turn, target artifact you control becomes a 5/5 creature in addition to its other types.\n" +
        "−7: Put any number of cards from your hand onto the battlefield face down. They're 5/5 artifact creatures."

    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1)
    }

    // Keeps its abilities and other types; base P/T becomes 5/5 (layer 7b).
    loyaltyAbility(0) {
        val artifact = target(TargetFilter.Artifact.youControl())
        effect = Effects.BecomeCreature(
            target = artifact,
            power = 5,
            toughness = 5,
            duration = Duration.UntilYourNextTurn,
        )
    }

    // They enter as face-down 5/5 artifact creatures (CR 708.2a), not as their own cards. Nothing
    // here turns them face up; a card printing morph or disguise still can be.
    loyaltyAbility(-7) {
        effect = Effects.Pipeline {
            val inHand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val chosen = chooseAnyNumber(
                from = inHand,
                prompt = "Choose any number of cards to put onto the battlefield face down",
            )
            move(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                faceDown = FaceDownMode.HIDDEN,
                entersWith = EntersWithCharacteristics(
                    addedCardTypes = setOf(CardType.ARTIFACT),
                    power = 5,
                    toughness = 5,
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "286"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/277dbeea-103c-4c5b-8981-3f5f7e60c66a.jpg?1783934489"
        ruling("2018-07-13", "Cards put onto the battlefield using Tezzeret's third ability enter the battlefield as 5/5 artifact creatures. They don't enter the battlefield with their own characteristics and then become 5/5.")
        ruling("2018-07-13", "If a creature enters the battlefield as a copy of a face-down creature or if a token is created that's a copy of one, that copy has the same characteristics as the face-down creature (in this case, a 5/5 artifact creature with no other characteristics), even though the copy is face up.")
        ruling("2018-07-13", "The Core Set 2019 set includes no way to turn these cards face up. If an effect from cards outside of this set does turn one face up, the effect causing it to be a 5/5 artifact creature ends.")
    }
}
