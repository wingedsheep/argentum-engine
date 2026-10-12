package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.ZoneChangeCause

/**
 * Obstinate Baloth
 * {2}{G}{G}
 * Creature — Beast
 * 4/4
 *
 * When this creature enters, you gain 4 life.
 * If a spell or ability an opponent controls causes you to discard this card, put it onto the
 * battlefield instead of putting it into your graveyard.
 *
 * - The discard clause is the same card-intrinsic self-replacement Wilt-Leaf Liege uses
 *   ([RedirectZoneChange] with `selfOnly = true`, functioning from hand per CR 113.6m), narrowed by
 *   [ZoneChangeCause.DiscardedByOpponentEffect] so cleanup-step hand-size discards and discards paid
 *   as your own costs still go to the graveyard.
 * - Entering this way is still entering, so the life-gain trigger fires.
 */
val ObstinateBaloth = card("Obstinate Baloth") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, you gain 4 life.\n" +
        "If a spell or ability an opponent controls causes you to discard this card, put it onto the " +
        "battlefield instead of putting it into your graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(4)
    }

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
        collectorNumber = "188"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6694496c-45b9-4ddf-bfcd-b632441b8811.jpg?1783941795"
        ruling(
            "2017-11-17",
            "If you discard Obstinate Baloth and put it onto the battlefield, you've still discarded it. " +
                "Abilities that trigger when you discard a card will still trigger."
        )
    }
}
