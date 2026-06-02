package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PlayAdditionalLandsEffect

/**
 * Explore {1}{G}
 * Sorcery
 *
 * You may play an additional land this turn.
 * Draw a card.
 */
val Explore = card("Explore") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "You may play an additional land this turn.\nDraw a card."

    spell {
        effect = Effects.Composite(
            listOf(
                PlayAdditionalLandsEffect(count = 1),
                Effects.DrawCards(1),
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Borja Pindado"
        flavorText = "As he crested the final peak, Nidd gasped at the beauty laid out before him. " +
            "Finally, he had found a place to rest his weary legs. A place to call home."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18e5fdee-1ec4-44a2-b84c-35897f1d2912.jpg?1721429259"
        ruling(
            "2017-03-14",
            "The effects of multiple Explores in the same turn are cumulative. They're also cumulative " +
                "with other effects that let you play additional lands.",
        )
        ruling(
            "2017-03-14",
            "Explore's effect allows you to play an additional land during your main phase. " +
                "Doing so follows the normal timing rules for playing lands. You don't get to play a " +
                "land as Explore resolves; Explore fully resolves first and you draw a card.",
        )
        ruling(
            "2017-03-14",
            "If you somehow manage to cast Explore when it's not your turn, you'll draw a card when it " +
                "resolves, but you won't be able to play a land that turn.",
        )
    }
}
