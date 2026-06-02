package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Last March of the Ents
 * {6}{G}{G}
 * Sorcery
 * This spell can't be countered.
 * Draw cards equal to the greatest toughness among creatures you control, then put any number
 * of creature cards from your hand onto the battlefield.
 */
val LastMarchOfTheEnts = card("Last March of the Ents") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "This spell can't be countered.\n" +
        "Draw cards equal to the greatest toughness among creatures you control, then put any number of creature cards from your hand onto the battlefield."

    cantBeCountered = true

    spell {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxToughness()
        ).then(
            Effects.Composite(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature),
                        storeAs = "creatureCards"
                    ),
                    SelectFromCollectionEffect(
                        from = "creatureCards",
                        selection = SelectionMode.ChooseAnyNumber,
                        storeSelected = "toPlay",
                        prompt = "Choose any number of creature cards to put onto the battlefield"
                    ),
                    MoveCollectionEffect(
                        from = "toPlay",
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "172"
        artist = "John Tedrick"
        flavorText = "\"To Isengard with doom we come!\nWith doom we come, with doom we come!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f1b99e0-ffb7-4f98-8ee5-4357bb79dd2e.jpg?1687694570"
    }
}
