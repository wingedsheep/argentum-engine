package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Leyline of the Void — Guildpact #52 (canonical / earliest real-expansion printing, 2006).
 * {2}{B}{B} · Enchantment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * If a card would be put into an opponent's graveyard from anywhere, exile it instead.
 *
 * The opening-hand clause is the shared `mayBeginGameOnBattlefield()` DSL helper (sets the
 * `mayStartOnBattlefield` flag that drives the pre-game yes/no prompt). The static clause is the reusable [RedirectZoneChange] graveyard ->
 * exile replacement (Rest in Peace / Stone of Erech share the type) scoped to an *opponent's*
 * graveyard via `GameObjectFilter.Any.ownedByOpponent()` — the engine evaluates owner != source
 * controller (ZoneMovementUtils `ControllerPredicate.OwnedByOpponent`), so cards headed to the
 * controller's own graveyard are unaffected. Unlike Rest in Peace there is no ETB sweep; Leyline of
 * the Void only redirects future graveyard-bound cards.
 *
 * Later printings (M11 #101, M20 #107, DSK #106, ...) carry only Printing rows; this is the canonical.
 */
val LeylineOfTheVoid = card("Leyline of the Void") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "If a card would be put into an opponent's graveyard from anywhere, exile it instead."

    mayBeginGameOnBattlefield()

    // If a card would be put into an opponent's graveyard from anywhere, exile it instead.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any.ownedByOpponent(),
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Adam Rex"
        flavorText = "Where treachery and oblivion converge."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37dfe8b8-b39e-4e70-9e5b-be42c93b4f70.jpg?1593272209"
    }
}
