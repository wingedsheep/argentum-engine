package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.ZoneChangeCause

/**
 * Loxodon Smiter
 * {1}{G}{W}
 * Creature — Elephant Soldier
 * 4/4
 *
 * This spell can't be countered.
 * If a spell or ability an opponent controls causes you to discard this card, put it onto the
 * battlefield instead of putting it into your graveyard.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * - "Can't be countered" is the intrinsic [cantBeCountered] flag (as on Abrupt Decay).
 * - The discard clause is the same card-intrinsic self-replacement Obstinate Baloth uses
 *   ([RedirectZoneChange] with `selfOnly = true`), narrowed by
 *   [ZoneChangeCause.DiscardedByOpponentEffect] so cleanup-step discards and your own discard costs
 *   still go to the graveyard.
 */
val LoxodonSmiter = card("Loxodon Smiter") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elephant Soldier"
    power = 4
    toughness = 4
    oracleText = "This spell can't be countered.\n" +
        "If a spell or ability an opponent controls causes you to discard this card, put it onto the " +
        "battlefield instead of putting it into your graveyard."

    cantBeCountered = true

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.BATTLEFIELD,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any,
                from = Zone.HAND,
                to = Zone.GRAVEYARD
            ),
            selfOnly = true,
            requiredCause = ZoneChangeCause.DiscardedByOpponentEffect
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "178"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69247168-2bfb-4cce-a2a6-61459a0fbce4.jpg?1783940336"
        ruling(
            "2012-10-01",
            "If you discard Loxodon Smiter and put it onto the battlefield, you've still discarded a card. " +
                "Abilities that trigger when you discard a card (such as the one from Liliana's Caress) " +
                "will still trigger."
        )
    }
}
