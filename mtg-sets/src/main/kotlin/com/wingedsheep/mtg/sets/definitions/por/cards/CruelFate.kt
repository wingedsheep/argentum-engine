package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Cruel Fate
 * {4}{U}
 * Sorcery
 * Look at the top five cards of target opponent's library. Put one of them into that
 * player's graveyard and the rest on top of their library in any order.
 */
val CruelFate = card("Cruel Fate") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5), Player.ContextPlayer(0)),
                    storeAs = "looked"
                ),
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "toGraveyard",
                    storeRemainder = "toTop",
                    selectedLabel = "Put in graveyard",
                    remainderLabel = "Put on top"
                ),
                MoveCollectionEffect(
                    from = "toGraveyard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0))
                ),
                MoveCollectionEffect(
                    from = "toTop",
                    destination = CardDestination.ToZone(Zone.LIBRARY, Player.ContextPlayer(0), ZonePlacement.Top),
                    order = CardOrder.ControllerChooses
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Adrian Smith"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44bea0d4-946e-4cb8-b6f1-50231d52bfbe.jpg"
    }
}
