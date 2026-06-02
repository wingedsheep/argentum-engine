package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Head Games
 * {3}{B}{B}
 * Sorcery
 * Target opponent puts the cards from their hand on top of their library.
 * Search that player's library for that many cards. The player puts those
 * cards into their hand, then shuffles.
 */
val HeadGames = card("Head Games") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent puts the cards from their hand on top of their library. Search that player's library for that many cards. The player puts those cards into their hand, then shuffles."

    spell {
        target = TargetOpponent()
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "opponentHand"
                ),
                MoveCollectionEffect(
                    from = "opponentHand",
                    destination = CardDestination.ToZone(Zone.LIBRARY, Player.ContextPlayer(0), ZonePlacement.Top)
                ),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.ContextPlayer(0)),
                    storeAs = "searchable"
                ),
                SelectFromCollectionEffect(
                    from = "searchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.VariableReference("opponentHand_count")),
                    chooser = Chooser.Controller,
                    storeSelected = "found"
                ),
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.HAND, Player.ContextPlayer(0))
                ),
                ShuffleLibraryEffect(EffectTarget.ContextTarget(0))
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Terese Nielsen"
        flavorText = "\"Don't worry. I'm not going to deprive you of your memories. I'm just going to replace them.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86ecc098-aa2b-4bae-80d5-4d02128ef837.jpg?1562926804"
    }
}
