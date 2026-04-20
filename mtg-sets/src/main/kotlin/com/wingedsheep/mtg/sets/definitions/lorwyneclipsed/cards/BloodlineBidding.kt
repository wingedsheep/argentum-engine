package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bloodline Bidding
 * {6}{B}{B}
 * Sorcery
 *
 * Convoke
 * Choose a creature type. Return all creature cards of the chosen type from your graveyard
 * to the battlefield.
 */
val BloodlineBidding = card("Bloodline Bidding") {
    manaCost = "{6}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Choose a creature type. Return all creature cards of the chosen type from your graveyard to the battlefield."

    keywords(Keyword.CONVOKE)

    spell {
        effect = CompositeEffect(
            listOf(
                ChooseCreatureTypeEffect,
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature),
                    storeAs = "graveyardCreatures"
                ),
                SelectFromCollectionEffect(
                    from = "graveyardCreatures",
                    selection = SelectionMode.All,
                    matchChosenCreatureType = true,
                    storeSelected = "chosen"
                ),
                MoveCollectionEffect(
                    from = "chosen",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Drew Baker"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/877d9b75-ad2f-45a6-94d8-68e80d7db789.jpg?1767732723"
    }
}
