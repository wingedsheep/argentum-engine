package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Escape to the Wilds
 * {3}{R}{G}
 * Sorcery
 *
 * Exile the top five cards of your library. You may play cards exiled this way until the end of
 * your next turn.
 * You may play an additional land this turn.
 *
 * Showdown of the Skalds chapter I's exile-top-N pipeline with the `UntilEndOfNextTurn` expiry,
 * followed by Azusa's Many Journeys' one-turn additional land drop.
 */
val EscapeToTheWilds = card("Escape to the Wilds") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Exile the top five cards of your library. You may play cards exiled this way until the end " +
        "of your next turn.\nYou may play an additional land this turn."

    spell {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(5))
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(from = exiled, expiry = MayPlayExpiry.UntilEndOfNextTurn))
            run(Effects.PlayAdditionalLands(1))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Chris Ostrowski"
        flavorText = "The guards kindled the hearth and locked the door to Ellwen's chamber. By morning, the " +
            "fire was out and Ellwen was gone."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e26c10b-179f-4a6e-bc8d-3ec1d6783fb9.jpg?1783932598"
        ruling(
            "2019-10-04",
            "Escape to the Wilds doesn't change when you can play the exiled cards. For example, if you exile a " +
                "sorcery card, you can cast it only during your main phase when the stack is empty. If you exile " +
                "a land card, you can play it only during your main phase and only if you have an available land " +
                "play remaining."
        )
        ruling("2019-10-04", "Casting an exiled card causes it to leave exile. You can't cast it multiple times.")
        ruling("2019-10-04", "The additional land that you play doesn't have to be from among the exiled cards.")
        ruling("2019-10-04", "If you don't play a card exiled this way, it remains in exile.")
    }
}
