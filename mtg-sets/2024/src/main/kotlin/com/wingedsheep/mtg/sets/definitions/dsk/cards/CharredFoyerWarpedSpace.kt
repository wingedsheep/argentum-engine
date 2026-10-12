package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.core.Step

/**
 * Charred Foyer // Warped Space (DSK 129) — split-layout Room (CR 709.5).
 *
 * Charred Foyer {3}{R} — Enchantment — Room
 *   At the beginning of your upkeep, exile the top card of your library. You may play it this turn.
 *
 * Warped Space {4}{R}{R} — Enchantment — Room
 *   Once each turn, you may pay {0} rather than pay the mana cost for a spell you cast from exile.
 *
 * Cast each half separately; the cast face enters unlocked, the other locked. Pay the locked
 * face's printed mana cost as a sorcery-speed special action to unlock it (CR 709.5e).
 *
 * Notes:
 * - Charred Foyer's upkeep ability is the standard "impulse draw" ([Patterns.Exile.impulse]) with
 *   an end-of-turn play window.
 * - Warped Space is the [MayCastWithoutPayingManaCost] free-cast static, gated to spells cast from
 *   exile (`fromZones = setOf(Zone.EXILE)`) and to once per turn (`oncePerTurn`). "Pay {0}" is an alternative cost
 *   of nothing, i.e. casting without paying the mana cost (CR 118.9 — additional/mandatory costs
 *   still apply, X is 0). It pairs naturally with Charred Foyer's impulse-exiled cards but applies
 *   to any spell cast from exile (foretell, adventure-in-exile, etc.).
 */
val CharredFoyerWarpedSpace = card("Charred Foyer // Warped Space") {
    layout = CardLayout.SPLIT
    colorIdentity = "R"

    face("Charred Foyer") {
        manaCost = "{3}{R}"
        typeLine = "Enchantment — Room"
        oracleText = "At the beginning of your upkeep, exile the top card of your library. You may play it this turn."

        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.EndOfTurn)
            description = "At the beginning of your upkeep, exile the top card of your library. You may play it this turn."
        }
    }

    face("Warped Space") {
        manaCost = "{4}{R}{R}"
        typeLine = "Enchantment — Room"
        oracleText = "Once each turn, you may pay {0} rather than pay the mana cost for a spell you cast from exile."

        staticAbility {
            ability = MayCastWithoutPayingManaCost(
                controllerOnly = true,
                oncePerTurn = true,
                fromZones = setOf(Zone.EXILE),
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "129"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a128e6d1-b90f-45a1-b587-f8c29bd0ec8c.jpg?1726867813"
        ruling("2024-09-20", "You pay all costs and follow all timing rules for cards played with the permission granted by Charred Foyer's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-09-20", "If you cast a spell for an alternative cost of {0}, you can't pay any other alternative costs. You can, however, pay additional costs, such as kicker. If the card has any mandatory additional costs, you must pay those.")
    }
}
