package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hanabi Blast — Champions of Kamigawa #170
 * {1}{R}{R} · Instant
 *
 * Hanabi Blast deals 2 damage to any target. Return Hanabi Blast to its owner's hand, then
 * discard a card at random.
 *
 * - The return happens *during* resolution: `CardSource.Self` gathers the resolving spell off the
 *   stack and moves it to its **owner's** hand (`Player.OwnerOfSource`), so the CR 608.2n
 *   "put it into its owner's graveyard" step then finds nothing to move.
 * - The random discard is by the spell's controller, after the return — Hanabi Blast itself can be
 *   the card discarded. If another player casts a Hanabi Blast you own, it returns to your hand and
 *   that player discards (2004-12-01 ruling).
 * - If the target is illegal on resolution the spell doesn't resolve: it goes to the graveyard and
 *   nothing is discarded.
 */
val HanabiBlast = card("Hanabi Blast") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Hanabi Blast deals 2 damage to any target. Return Hanabi Blast to its owner's hand, " +
        "then discard a card at random."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t) then
            Effects.Pipeline {
                toHand(gather(CardSource.Self), Player.OwnerOfSource)
            } then
            Patterns.Hand.discardRandom(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Paolo Parente"
        flavorText = "The most powerful of akki fire spells were developed at the cost of blood, toil, " +
            "tears, sweat, and usually a nose or two."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/881fecf4-8c14-4614-84bd-c1a3dcdbb5ff.jpg?1783944300"
        ruling(
            "2004-12-01",
            "If another player casts a Hanabi Blast that you own, it returns to your hand, and then " +
                "that player discards a card at random."
        )
    }
}
