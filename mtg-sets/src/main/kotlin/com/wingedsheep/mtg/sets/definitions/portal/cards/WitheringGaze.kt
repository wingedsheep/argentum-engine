package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Withering Gaze
 * {2}{U}
 * Sorcery
 * Target opponent reveals their hand. You draw a card for each Forest and green card in it.
 */
val WitheringGaze = card("Withering Gaze") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = CompositeEffect(
            listOf(
                RevealHandEffect(EffectTarget.ContextTarget(0)),
                DrawCardsEffect(
                    count = DynamicAmount.CountInZone(
                        player = PlayerReference.TargetOpponent,
                        zone = ZoneReference.Hand,
                        filter = CountFilter.Or(
                            listOf(
                                CountFilter.HasSubtype("Forest"),
                                CountFilter.CardColor(Color.GREEN)
                            )
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e952a48-9e60-4fce-8423-7f0bafd29bb1.jpg"
    }
}
