package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Zone

/**
 * Urza's Ruinous Blast
 * {4}{W}
 * Legendary Sorcery
 * (You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)
 * Exile all nonland permanents that aren't legendary.
 */
val UrzasRuinousBlast = card("Urza's Ruinous Blast") {
    manaCost = "{4}{W}"
    typeLine = "Legendary Sorcery"
    oracleText = "(You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)\nExile all nonland permanents that aren't legendary."

    spell {
        castOnlyIf(Conditions.ControlLegendaryCreatureOrPlaneswalker)

        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.NonlandPermanent.nonlegendary()
                ),
                storeAs = "exileAll_gathered"
            ),
            MoveCollectionEffect(
                from = "exileAll_gathered",
                destination = CardDestination.ToZone(Zone.EXILE)
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Noah Bradley"
        flavorText = "Centuries ago, one man's vengeance plunged the world into ice and darkness."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57b50a96-6221-44f0-b54a-76076621047e.jpg?1562736004"
    }
}
