package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Cabal Conditioning
 * {6}{B}
 * Sorcery
 * Any number of target players each discard a number of cards equal to the greatest
 * mana value among permanents you control.
 */
val CabalConditioning = card("Cabal Conditioning") {
    manaCost = "{6}{B}"
    typeLine = "Sorcery"
    oracleText = "Any number of target players each discard a number of cards equal to the greatest mana value among permanents you control."

    spell {
        target("players", TargetPlayer(count = 2, optional = true))
        effect = ForEachTargetEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(
                        DynamicAmounts.battlefield(Player.You).maxManaValue()
                    ),
                    chooser = Chooser.TargetPlayer,
                    storeSelected = "discarded"
                ),
                MoveCollectionEffect(
                    from = "discarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Scott M. Fischer"
        flavorText = "\"Hear only the Cabal's voice. See only the Cabal's way. Speak only the Cabal's word.\" —Cabal mantra"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb81c6e6-fded-4cd3-a6fa-486419a5408a.jpg?1650357097"
    }
}
