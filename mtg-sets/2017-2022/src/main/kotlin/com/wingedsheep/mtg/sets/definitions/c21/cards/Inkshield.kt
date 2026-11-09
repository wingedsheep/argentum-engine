package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Inkshield — Commander 2021 #71 (canonical printing)
 * {3}{W}{B} · Instant
 *
 * Prevent all combat damage that would be dealt to you this turn. For each 1 damage prevented this
 * way, create a 2/1 white and black Inkling creature token with flying.
 *
 * A recipient-side combat-only shield on the controller ("to you") that carries its own reaction:
 * every application of the shield — each combat damage instance it prevents — creates one Inkling
 * per damage prevented ([DynamicAmounts.preventedDamage]), so several attackers add up. Where
 * damage can't be prevented (CR 615.12) the shield still applies but prevents nothing, so the
 * reaction counts 0 and no Inkling is made. Token art resolves
 * through the synced token printings.
 */
val Inkshield = card("Inkshield") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "BW"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt to you this turn. For each 1 damage prevented " +
        "this way, create a 2/1 white and black Inkling creature token with flying."

    spell {
        effect = Effects.PreventDamage(
            combatOnly = true,
            alsoToYou = true,
            onPrevented = Effects.CreateToken(
                count = DynamicAmounts.preventedDamage(),
                power = 2,
                toughness = 1,
                colors = setOf(Color.WHITE, Color.BLACK),
                creatureTypes = setOf("Inkling"),
                keywords = setOf(Keyword.FLYING)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Jarel Threat"
        flavorText = "Silverquill's elite know how to offer a scathing rebuttal without uttering a word."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e1c14a0-cd68-45fc-a127-422ca6113048.jpg?1783927586"
    }
}
