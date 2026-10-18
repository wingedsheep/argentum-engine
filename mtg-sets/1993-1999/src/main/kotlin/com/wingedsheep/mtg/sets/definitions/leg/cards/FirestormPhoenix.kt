package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Firestorm Phoenix
 * {4}{R}{R}
 * Creature — Phoenix
 * 3/2
 *
 * Flying
 * If this creature would die, return it to its owner's hand instead. Until that player's next
 * turn, that player plays with that card revealed in their hand and can't play it.
 *
 * The whole second paragraph is one replacement effect: the reveal and the lock are its rider,
 * pinned to the card as the new object in its owner's hand. "That player" is the owner, so the
 * window closes on the owner's next turn even when an opponent controlled the Phoenix.
 */
val FirestormPhoenix = card("Firestorm Phoenix") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "If this creature would die, return it to its owner's hand instead. Until that player's next turn, " +
        "that player plays with that card revealed in their hand and can't play it."

    keywords(Keyword.FLYING)

    replacementEffect(
        RedirectZoneChangeWith(
            newDestination = Zone.HAND,
            additionalEffect = Effects.RevealCardInHand(EffectTarget.Self, Duration.UntilAffectedOwnersNextTurn) then
                Effects.CantPlayCardsFromHand(EffectTarget.Self, Duration.UntilAffectedOwnersNextTurn),
            selfOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any,
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Jeff A. Menges"
        flavorText = "\"The bird of wonder dies, the maiden phoenix,/ Her ashes new-create another heir/ " +
            "As great in admiration as herself.\" —William Shakespeare, King Henry the Eighth"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3247a7dd-f48c-4cb4-8475-4864acccef7a.jpg?1783948056"
        ruling("2009-10-01", "The Firestorm Phoenix you return to your hand is tracked by its ability. As long as it remains in your hand, that particular card can't be played before your next turn begins, but a different Firestorm Phoenix in your hand can.")
        ruling("2004-10-04", "It may not be cast again until your next turn, but it can be put onto the battlefield by other effects.")
        ruling("2004-10-04", "The \"rebirth\" is a replacement effect.")
    }
}
