package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

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
    colorIdentity = ""
    oracleText = "{T}, Sacrifice this land: Search your library for a basic land card, put it onto " +
        "the battlefield tapped, then shuffle. Then if you control four or more lands, untap that land."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.Pipeline {
            // Search library for basic land
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                search = true
            )
            val found = chooseUpTo(1, from = searchable)
            // Put onto battlefield tapped, storing the moved entity for potential untap
            val movedLand = moveTracked(
                found,
                CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
            )
            // Shuffle library
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            // Then if you control 4+ lands, untap that land
            run(Effects.If(
                condition = Conditions.ControlLandsAtLeast(4),
                then = Effects.TapCollection(collection = movedLand, tap = false)
            ))
        }
        manaAbility = false
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Howard Lyon"
        flavorText = "From here, the path to anywhere may be found."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b841bfa8-7c17-4df2-8466-780ab9a4a53a.jpg?1783932578"
        ruling("2020-06-23", "The land that you put onto the battlefield will be counted when determining whether you control four or more lands, but Fabled Passage will not.")
        ruling("2020-06-23", "If you control four or more lands, the basic land doesn't enter untapped; it enters tapped and then you untap it.")
    }
}
