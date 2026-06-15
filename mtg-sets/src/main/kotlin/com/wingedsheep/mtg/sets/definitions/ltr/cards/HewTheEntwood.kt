package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hew the Entwood
 * {3}{R}{R}
 * Sorcery
 *
 * Sacrifice any number of lands. Reveal the top X cards of your library, where X is the number
 * of lands sacrificed this way. Choose any number of artifact and/or land cards revealed this
 * way. Put all nonland cards chosen this way onto the battlefield, then put all land cards chosen
 * this way onto the battlefield tapped, then put the rest on the bottom of your library in a
 * random order.
 *
 * Composes the existing library pipeline:
 *  1. `SacrificeAnyNumber(Land)` — controller sacrifices 0+ of their lands; the sacrifice
 *     executor records them so X is readable downstream.
 *  2. `GatherCards(TopOfLibrary(X))` where X = `permanentsSacrificedThisWay` + reveal.
 *  3. `SelectFromCollection(any number, filter = Artifact OR Land)` partitions the revealed pile
 *     into `chosen` (the artifact/land cards the player picked) and `rest`.
 *  4. Three filtered `MoveCollection`s: chosen nonland (artifacts that aren't lands) → battlefield
 *     untapped; chosen lands → battlefield tapped; the rest → bottom of library in random order.
 */
val HewTheEntwood = card("Hew the Entwood") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Sacrifice any number of lands. Reveal the top X cards of your library, where X " +
        "is the number of lands sacrificed this way. Choose any number of artifact and/or land " +
        "cards revealed this way. Put all nonland cards chosen this way onto the battlefield, " +
        "then put all land cards chosen this way onto the battlefield tapped, then put the rest " +
        "on the bottom of your library in a random order."

    spell {
        effect = Effects.SacrificeAnyNumber(GameObjectFilter.Land)
            .then(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        DynamicAmounts.permanentsSacrificedThisWay(),
                        Player.You
                    ),
                    storeAs = "revealed"
                )
            )
            .then(RevealCollectionEffect(from = "revealed"))
            .then(
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseAnyNumber,
                    filter = GameObjectFilter.Artifact or GameObjectFilter.Land,
                    storeSelected = "chosen",
                    storeRemainder = "rest",
                    prompt = "Choose any number of artifact and/or land cards to put onto the battlefield"
                )
            )
            .then(
                MoveCollectionEffect(
                    from = "chosen",
                    filter = GameObjectFilter.Nonland,
                    destination = CardDestination.ToZone(
                        Zone.BATTLEFIELD,
                        player = Player.You,
                        placement = ZonePlacement.Default
                    )
                )
            )
            .then(
                MoveCollectionEffect(
                    from = "chosen",
                    filter = GameObjectFilter.Land,
                    destination = CardDestination.ToZone(
                        Zone.BATTLEFIELD,
                        player = Player.You,
                        placement = ZonePlacement.Tapped
                    )
                )
            )
            .then(
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(
                        Zone.LIBRARY,
                        player = Player.You,
                        placement = ZonePlacement.Bottom
                    ),
                    order = CardOrder.Random
                )
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "136"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81940498-a5bf-4bcd-b06d-8816887b2a2b.jpg?1686969039"
    }
}
