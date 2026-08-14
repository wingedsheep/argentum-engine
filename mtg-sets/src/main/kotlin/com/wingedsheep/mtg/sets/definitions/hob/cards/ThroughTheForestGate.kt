package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
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
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Through the Forest Gate — The Hobbit #137
 * {6}{G}{G} · Sorcery · Rare
 *
 * Look at the top twenty cards of your library, put any number of land cards from among them
 * onto the battlefield tapped, then shuffle. You gain 8 life.
 *
 * Modeling notes:
 *  - Built from the atomic Gather → Select → Move pipeline (cf. Choco, Seeker of Paradise).
 *    "Look at" is a private gather (`revealed = false`), which leaves the cards in the library —
 *    only the ones selected are moved out, so the trailing shuffle naturally scrambles the
 *    seventeen-or-so cards the player looked at but declined.
 *  - The land pick is a `ChooseAnyNumber` filtered to lands with `showAllCards = true`, so the
 *    player sees all twenty looked-at cards but can only pick the lands ("any number" includes
 *    zero — declining is legal).
 *  - A library holding fewer than twenty cards simply yields what's there; an empty library
 *    yields nothing and the spell still shuffles and gains 8 life.
 */
val ThroughTheForestGate = card("Through the Forest Gate") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top twenty cards of your library, put any number of land cards from " +
        "among them onto the battlefield tapped, then shuffle. You gain 8 life."

    spell {
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(20), player = Player.You),
                storeAs = "looked"
            ),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseAnyNumber,
                filter = GameObjectFilter.Land,
                showAllCards = true,
                storeSelected = "toBattlefield",
                prompt = "Put any number of land cards onto the battlefield tapped",
                selectedLabel = "Onto the battlefield tapped",
                remainderLabel = "Leave in your library"
            ),
            MoveCollectionEffect(
                from = "toBattlefield",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped)
            ),
            ShuffleLibraryEffect(),
            Effects.GainLife(8)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Leon Tukker"
        flavorText = "\"Well, here is Mirkwood—the greatest of the forests of the northern world. " +
            "I hope you like the look of it.\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/880adfc8-69cf-4062-a804-e65b6cb6056d.jpg?1784895046"
    }
}
