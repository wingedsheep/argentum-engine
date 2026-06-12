package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Scout the Borders
 * {2}{G}
 * Sorcery
 * Reveal the top five cards of your library. You may put a creature or land card
 * from among them into your hand. Put the rest into your graveyard.
 */
val ScoutTheBorders = card("Scout the Borders") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top five cards of your library. You may put a creature or land card from among them into your hand. Put the rest into your graveyard."

    spell {
        effect = Effects.Pipeline {
            val revealed = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(5)),
                revealed = true,
                name = "revealed"
            )
            val (kept, rest) = chooseUpToSplit(
                1,
                from = revealed,
                filter = GameObjectFilter.Companion.CreatureOrLand,
                selectedLabel = "Put in hand",
                remainderLabel = "Put in graveyard",
                showAllCards = true,
                name = "kept",
                remainderName = "rest"
            )
            move(kept, CardDestination.ToZone(Zone.HAND))
            move(rest, CardDestination.ToZone(Zone.GRAVEYARD))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "James Paick"
        flavorText = "\"I am in my element: the element of surprise.\" —Mogai, Sultai scout"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb15c6c7-8fe6-496c-9977-3e7942b920c4.jpg?1562795470"
    }
}
