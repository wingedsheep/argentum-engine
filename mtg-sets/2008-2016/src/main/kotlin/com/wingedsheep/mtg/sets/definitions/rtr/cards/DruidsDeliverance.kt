package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Druid's Deliverance
 * {1}{G}
 * Instant
 * Prevent all combat damage that would be dealt to you this turn. Populate. (Create a token that's a
 * copy of a creature token you control.)
 *
 * A controller-only combat shield ([Effects.PreventDamage] with its default `Controller` recipient and
 * `combatOnly`, as on Decorated Griffin) followed by populate — a choice on resolution, never a target,
 * so the spell still resolves (and the fog still applies) when you control no creature token.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val DruidsDeliverance = card("Druid's Deliverance") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt to you this turn. Populate. " +
        "(Create a token that's a copy of a creature token you control.)"

    spell {
        effect = Effects.PreventDamage(combatOnly = true) then
            Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Dan Murayama Scott"
        flavorText = "\"So many oppose us, but we are the reed that bends without breaking.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83b35961-f4d5-4e14-a793-335147110627.jpg?1783940349"
    }
}
