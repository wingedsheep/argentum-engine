package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

val EvolvingWilds = card("Evolving Wilds") {
    typeLine = "Land"
    oracleText = "{T}, Sacrifice this land: Search your library for a basic land card, put it onto " +
        "the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                    storeAs = "searchable"
                ),
                SelectFromCollectionEffect(
                    from = "searchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "found"
                ),
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
                ),
                ShuffleLibraryEffect()
            )
        )
        manaAbility = false
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "264"
        artist = "Alayna Danner"
        flavorText = "\"I don't think we're going to make it back in time for Introduction to Prophecy.\"\n—Tam, Strixhaven first-year"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c632984-5176-4c37-91df-6577cc294b85.jpg?1767863461"
    }
}
