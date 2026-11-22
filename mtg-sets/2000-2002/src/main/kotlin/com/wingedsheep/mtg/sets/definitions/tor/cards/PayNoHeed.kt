package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Pay No Heed
 * {W}
 * Instant
 * Prevent all damage a source of your choice would deal this turn.
 *
 * No recipient clause — the chosen source's damage to anything is prevented, so this is the
 * `FromTarget` chosen-source shield Burrenton Forge-Tender uses, without its colour restriction.
 * The spell doesn't target: the source is chosen on resolution (hexproof sources are fine).
 */
val PayNoHeed = card("Pay No Heed") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent all damage a source of your choice would deal this turn."

    spell {
        effect = Effects.PreventDamage(
            direction = PreventionDirection.FromTarget,
            sources = PreventionSourceFilter.Chosen()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Adam Rex"
        flavorText = "\"Inhale life. Exhale pain.\"\n—Mystic elder"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9520a1be-db77-4f31-a67d-6309a3ddc566.jpg?1783945168"
        ruling(
            "2013-07-01",
            "Pay No Heed doesn't target anything. You can choose a source with hexproof, for " +
                "example. You choose the damage source as Pay No Heed resolves."
        )
        ruling(
            "2013-07-01",
            "If the chosen source is a permanent spell, damage dealt later in the turn by the " +
                "permanent it becomes will also be prevented."
        )
    }
}
