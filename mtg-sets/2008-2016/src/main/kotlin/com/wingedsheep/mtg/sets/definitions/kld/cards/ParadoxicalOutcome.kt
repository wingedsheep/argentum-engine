package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Paradoxical Outcome
 * {3}{U}
 * Instant
 * Return any number of target nonland, nontoken permanents you control to their owners' hands.
 * Draw a card for each card returned to your hand this way.
 *
 * Each permanent goes to its *owner's* hand, so the draw count is the moved cards narrowed to
 * those you own that actually sit in a hand afterwards: a permanent you control but don't own
 * lands in its owner's hand and draws nothing (2021-03-19 ruling), and one a replacement sent
 * somewhere else never reached your hand.
 */
val ParadoxicalOutcome = card("Paradoxical Outcome") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return any number of target nonland, nontoken permanents you control to their owners' hands. " +
        "Draw a card for each card returned to your hand this way."

    spell {
        targets(
            TargetFilter(GameObjectFilter.NonlandPermanent.nontoken().youControl()),
            optional = true,
            unlimited = true,
        )
        effect = Effects.Pipeline {
            val returned = moveTracked(gather(CardSource.ChosenTargets), CardDestination.ToZone(Zone.HAND))
            val toYourHand = filter(returned, GameObjectFilter.Any.ownedByYou().currentlyIn(Zone.HAND))
            run(Effects.DrawCards(toYourHand.count))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Nils Hamm"
        flavorText = "The results of Rashmi's experimentation forced her to consider the possibility that " +
            "multitudinous realities existed beyond her own."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17e50157-bf49-4c5f-9b8a-bf73484e63a5.jpg?1783937215"
        ruling(
            "2021-03-19",
            "If you control but don't own some of the target permanents, they won't count when determining " +
                "how many cards you draw."
        )
    }
}
