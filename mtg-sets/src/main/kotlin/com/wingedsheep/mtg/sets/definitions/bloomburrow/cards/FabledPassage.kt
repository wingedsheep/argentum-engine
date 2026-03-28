package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fabled Passage
 * Land
 *
 * {T}, Sacrifice this land: Search your library for a basic land card, put it
 * onto the battlefield tapped, then shuffle. Then if you control four or more
 * lands, untap that land.
 *
 * Rulings:
 * - The land that you put onto the battlefield will be counted when determining
 *   whether you control four or more lands, but Fabled Passage will not.
 * - If you control four or more lands, the basic land doesn't enter untapped;
 *   it enters tapped and then you untap it.
 */
val FabledPassage = card("Fabled Passage") {
    typeLine = "Land"
    oracleText = "{T}, Sacrifice this land: Search your library for a basic land card, put it onto " +
        "the battlefield tapped, then shuffle. Then if you control four or more lands, untap that land."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = CompositeEffect(
            listOf(
                // Search library for basic land
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                    storeAs = "searchable"
                ),
                SelectFromCollectionEffect(
                    from = "searchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "found"
                ),
                // Put onto battlefield tapped, storing the moved entity for potential untap
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                    storeMovedAs = "movedLand"
                ),
                // Shuffle library
                ShuffleLibraryEffect(),
                // Then if you control 4+ lands, untap that land
                ConditionalEffect(
                    condition = Conditions.ControlLandsAtLeast(4),
                    effect = TapUntapCollectionEffect(collectionName = "movedLand", tap = false)
                )
            )
        )
        manaAbility = false
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Adam Paquette"
        flavorText = "Beyond Valley lies danger—and promise."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8809830f-d8e1-4603-9652-0ad8b00234e9.jpg?1721427315"
        ruling("2020-06-23", "The land that you put onto the battlefield will be counted when determining whether you control four or more lands, but Fabled Passage will not.")
        ruling("2020-06-23", "If you control four or more lands, the basic land doesn't enter untapped; it enters tapped and then you untap it.")
    }
}
