package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Chittering Rats — Darksteel #39
 * {1}{B}{B} · Creature — Rat · 2/2
 *
 * When this creature enters, target opponent puts a card from their hand on top of their library.
 *
 * Same Gather → Select → Move shape as Chimney Imp's dies trigger: the targeted opponent
 * (`Chooser.TargetPlayer`) picks the card, no reveal, and it goes on top of their own library.
 * An empty hand gathers nothing and the trigger resolves as a no-op.
 */
val ChitteringRats = card("Chittering Rats") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, target opponent puts a card from their hand on top of their library."

    triggeredAbility {
        val opponent = target(Targets.Opponent)
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val ratsHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val ratsTucked = chooseExactly(
                1,
                from = ratsHand,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose a card to put on top of your library"
            )
            toLibraryTop(ratsTucked, opponent.asPlayer, order = CardOrder.Preserve)
        }
        description = "When this creature enters, target opponent puts a card from their hand " +
            "on top of their library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Tom Wänerstrand"
        flavorText = "Bottom feeders sometimes rise to the top."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/980135d5-dfaa-4beb-b4b3-1e256bb46e61.jpg?1783944446"
    }
}
