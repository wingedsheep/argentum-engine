package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionReaction

/**
 * New Way Forward — Tarkir: Dragonstorm #211
 * {2}{U}{R}{W} · Instant
 *
 * The next time a source of your choice would deal damage to you this turn, prevent that damage.
 * When damage is prevented this way, New Way Forward deals that much damage to that source's
 * controller and you draw that many cards.
 *
 * Deflecting Palm's prevent-and-reflect chain (a chosen-source prevention shield whose prevented
 * amount feeds [PreventionReaction.DealToSourceController]), plus an extra
 * [PreventionReaction.ControllerDrawsCards] link so you also draw that many cards.
 *
 * Does not target: the source is chosen as New Way Forward resolves.
 */
val NewWayForward = card("New Way Forward") {
    manaCost = "{2}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Instant"
    oracleText = "The next time a source of your choice would deal damage to you this turn, prevent that damage. " +
        "When damage is prevented this way, New Way Forward deals that much damage to that source's controller and you draw that many cards."

    spell {
        effect = Effects.PreventNextDamageFromChosenSourceThen(
            PreventionReaction.DealToSourceController,
            PreventionReaction.ControllerDrawsCards
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9d48f9e-79f0-478c-9db0-ff7ac4a8f401.jpg?1743204834"
        ruling("2025-04-04", "New Way Forward doesn't target any permanent or player. You choose a source as New Way Forward resolves.")
        ruling("2025-04-04", "If another effect (or effects) modifies how much damage the chosen source would deal to you this turn, you choose the order in which any such effects, including the effect of New Way Forward, apply.")
    }
}
