package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Induced Amnesia (Rivals of Ixalan — the card's earliest printing)
 * {2}{U}
 * Enchantment
 *
 * When this enchantment enters, target player exiles all cards from their hand face down, then
 * draws that many cards.
 * When this enchantment is put into a graveyard from the battlefield, return the exiled cards to
 * their owner's hand.
 *
 * Implementation:
 *  - The exile is face down with no turn-up procedure ([FaceDownMode.HIDDEN]) and not lookable,
 *    so no player may look at the cards (first ruling). It links the cards to this enchantment
 *    (CR 607 linked abilities), so only the cards this object exiled come back — another Induced
 *    Amnesia, or this card returning as a new object, finds nothing (second ruling).
 *  - "That many" counts the cards actually exiled ([moveTracked]).
 *  - The return is a dies-shaped trigger ("put into a graveyard from the battlefield"), so a
 *    bounce or exile of the enchantment leaves the cards exiled forever. Leaving before the enters
 *    ability resolves makes the return trigger find an empty pile (third ruling).
 *  - The cards all came from one hand, so they share one owner; the return runs as that owner
 *    ([Player.OwnersOfLinkedExile]) so they go to their owner's hand, not this controller's.
 */
val InducedAmnesia = card("Induced Amnesia") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, target player exiles all cards from their hand " +
        "face down, then draws that many cards.\n" +
        "When this enchantment is put into a graveyard from the battlefield, return the exiled " +
        "cards to their owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(Targets.Player)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.TargetPlayer))
            val exiled = moveTracked(
                hand,
                CardDestination.ToZone(Zone.EXILE, Player.TargetPlayer),
                linkToSource = true,
                faceDown = FaceDownMode.HIDDEN,
            )
            run(Effects.DrawCards(exiled.count, EffectTarget.PlayerRef(Player.TargetPlayer)))
        }
        description = "When this enchantment enters, target player exiles all cards from their " +
            "hand face down, then draws that many cards."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ForEachPlayer(
            players = Player.OwnersOfLinkedExile,
            Effects.Pipeline {
                val forgotten = gather(CardSource.FromLinkedExile())
                toHand(forgotten)
            }
        )
        description = "When this enchantment is put into a graveyard from the battlefield, " +
            "return the exiled cards to their owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4ee56f1-9f08-459f-8517-0fe7bc645fa3.jpg?1783935325"
        ruling("2018-01-19", "No player may look at the exiled cards.")
        ruling("2018-01-19", "If Induced Amnesia leaves the battlefield but isn't put into a graveyard, the exiled cards are lost forever. They won't be returned if another Induced Amnesia is put into a graveyard, even if that Induced Amnesia is represented by the same card.")
        ruling("2018-01-19", "If Induced Amnesia leaves the battlefield before its first ability resolves, its second ability will trigger (if appropriate) and do nothing. Then its first ability will resolve, and the exiled cards will be exiled indefinitely.")
    }
}
