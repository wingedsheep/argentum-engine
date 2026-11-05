package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shadow of the Grave — Amonkhet #107
 * {1}{B} · Instant
 *
 * Return to your hand all cards in your graveyard that you cycled or discarded this turn.
 *
 * Cycling discards the card (CR 702.29a), so one discarded-this-turn filter covers both verbs —
 * any discard counts, including one an opponent's effect forced. An uncast madness card that went
 * exile → graveyard still counts (the card's ruling); one that left the graveyard since doesn't.
 */
val ShadowOfTheGrave = card("Shadow of the Grave") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Return to your hand all cards in your graveyard that you cycled or discarded this turn."

    spell {
        effect = Effects.Pipeline {
            val discarded = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Any.discardedThisTurn(),
                ),
            )
            toHand(discarded)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Darek Zabrocki"
        flavorText = "\"Death has seeped into every part of this plane. I can feel it on my skin and " +
            "taste it like sand in my mouth.\"\n—Nissa Revane"
    }
}
