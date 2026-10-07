package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

val TeferisPuzzleBox = card("Teferi's Puzzle Box") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "At the beginning of each player's draw step, that player puts the cards in their hand on the bottom of their library in any order, then draws that many cards."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.DRAW)
        effect = Effects.ForEachPlayer(Player.TriggeringPlayer, Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND))
            toLibraryBottom(hand, order = CardOrder.OwnerChooses)
            run(Effects.DrawCards(hand.count))
        })
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1377dab4-b814-46cc-a097-24a3cf8d0f8f.jpg?1783946972"
        ruling("2004-10-04", "You do your normal draw before this ability is put on the stack.")
        ruling("2004-10-04", "If you have more than one of these, each effect triggers separately.")
    }
}
