package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
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
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Elven Passage — The Hobbit #181
 * Land · Rare
 *
 * {T}, Pay 1 life, Sacrifice this land: Search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle. You may behold an Elf. If you do, untap that land.
 *
 * Fabled Passage's fetch pipeline with the land-count gate swapped for a resolution-time behold:
 * the fetched land is stashed by `storeMovedAs`, so [Effects.Behold]'s `ifBeheld` can untap exactly
 * that land via [TapUntapCollectionEffect]. Behold is optional and may be declined (or be
 * impossible, if the controller neither controls an Elf nor holds an Elf card), in which case the
 * land stays tapped — as it also does when the search finds nothing, since the collection is then
 * empty and the untap is a no-op.
 */
val ElvenPassage = card("Elven Passage") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}, Pay 1 life, Sacrifice this land: Search your library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle. You may behold an Elf. If you do, untap " +
        "that land."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.PayLife(1),
            Costs.SacrificeSelf
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                    storeAs = "passage_searchable"
                ),
                SelectFromCollectionEffect(
                    from = "passage_searchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "passage_found",
                    prompt = "Search for a basic land card to put onto the battlefield tapped"
                ),
                MoveCollectionEffect(
                    from = "passage_found",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                    storeMovedAs = "passage_movedLand"
                ),
                ShuffleLibraryEffect(),
                Effects.Behold(
                    filter = GameObjectFilter.Any.withSubtype(Subtype.ELF),
                    ifBeheld = TapUntapCollectionEffect(collectionName = "passage_movedLand", tap = false)
                )
            )
        )
        manaAbility = false
        description = "Search your library for a basic land card, put it onto the battlefield " +
            "tapped, then shuffle. You may behold an Elf. If you do, untap that land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Shahab Alizadeh"
        flavorText = "Filled with the twinkling lights, laughter, and songs of the Elves."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd1fd2ab-2565-4798-a832-fc849df82f74.jpg?1785323319"
    }
}
