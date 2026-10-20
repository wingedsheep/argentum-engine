package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Visions
 * {W}
 * Sorcery
 * Look at the top five cards of target player's library. You may then have that player shuffle
 * that library.
 *
 * The look is a bare gather (as Orcish Spy): [CardSource.TopOfLibrary] only reads the cards, so
 * they stay where they are in their original order. The shuffle is the caster's optional choice,
 * so a controller-decided `May` wraps a shuffle of the target player's library (as Natural Selection).
 */
val Visions = card("Visions") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of target player's library. " +
        "You may then have that player shuffle that library."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            gather(CardSource.TopOfLibrary(5, Player.TargetPlayer))
            run(Effects.May(Effects.ShuffleLibrary(player)))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "NéNé Thomas"
        flavorText = "\"Visions of glory, spare my aching sight,/ Ye unborn ages, crowd not on my soul!\"\n—Thomas Gray, *The Bard*"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21d00299-e183-4b3d-b015-18808e7135b9.jpg?1783948080"
    }
}
