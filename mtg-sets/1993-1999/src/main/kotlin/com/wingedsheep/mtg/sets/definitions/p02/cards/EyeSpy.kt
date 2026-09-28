package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Eye Spy
 * {U}
 * Sorcery
 * Look at the top card of target player's library. You may put that card into their graveyard.
 *
 * The "you may" is the selection (`chooseUpToSplit(1)`), the surveil shape aimed at another
 * player's library; a declined choice leaves the card on top. Same pipeline as Lurking Informant.
 */
val EyeSpy = card("Eye Spy") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Look at the top card of target player's library. You may put that card into their graveyard."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val peeked = gather(CardSource.TopOfLibrary(1, player.asPlayer))
            val (toGraveyardCards, toTop) = chooseUpToSplit(
                1,
                from = peeked,
                selectedLabel = "Put into their graveyard",
                remainderLabel = "Leave on top of their library"
            )
            toGraveyard(toGraveyardCards, player.asPlayer)
            toLibraryTop(toTop, player.asPlayer, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee109b81-96ef-494a-9d6d-0ea4a49b76a0.jpg?1783946489"
    }
}
