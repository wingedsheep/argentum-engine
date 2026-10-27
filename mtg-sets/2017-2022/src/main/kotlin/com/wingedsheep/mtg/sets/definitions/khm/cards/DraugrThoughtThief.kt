package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Draugr Thought-Thief
 * {2}{U}
 * Creature — Zombie Rogue
 * 3/2
 * When this creature enters, look at the top card of target player's library. You may put that
 * card into their graveyard.
 *
 * The Eye Spy / Lurking Informant pipeline on an enters trigger: the "you may" is the selection
 * (`chooseUpToSplit(1)`), so a declined choice leaves the card on top of the targeted library.
 */
val DraugrThoughtThief = card("Draugr Thought-Thief") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Rogue"
    oracleText = "When this creature enters, look at the top card of target player's library. " +
        "You may put that card into their graveyard."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
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
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Dan Murayama Scott"
        flavorText = "The worst part is that you won't even remember how much he's taken from you."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31da60a2-d4d3-4954-be59-c7ff61fa2905.jpg?1783928265"
    }
}
