package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Natural Selection
 * {G}
 * Instant
 * Look at the top three cards of target player's library, then put them back in any order. You may
 * have that player shuffle.
 *
 * Gather the top three of the *target's* library and put them back on top of that same library; the
 * default `CardOrder.ControllerChooses` lets the spell's controller pick the order. The shuffle is the
 * controller's optional choice ("you may have that player shuffle"), so a `May` decided by the
 * controller wraps a shuffle of the target player's library.
 */
val NaturalSelection = card("Natural Selection") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Look at the top three cards of target player's library, then put them back in any order. " +
        "You may have that player shuffle."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(3, Player.TargetPlayer))
            toLibraryTop(looked, Player.TargetPlayer)
            run(Effects.May(Effects.ShuffleLibrary(player)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8917dc8-01c0-4e72-9310-c4d501775411.jpg?1783948673"
    }
}
