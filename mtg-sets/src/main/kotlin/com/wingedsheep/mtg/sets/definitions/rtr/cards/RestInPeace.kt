package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rest in Peace — Return to Ravnica #18 (canonical printing)
 * {1}{W} · Enchantment
 *
 * When this enchantment enters, exile all graveyards.
 * If a card or token would be put into a graveyard from anywhere, exile it instead.
 *
 * The ETB clause is the atomic Gather -> Move pipeline over every player's graveyard
 * (`CardSource.FromZone(GRAVEYARD, Player.Each)` -> `CardDestination.ToZone(EXILE)`). The
 * static clause is the reusable [RedirectZoneChange] replacement (Leyline of the Void / Stone
 * of Erech share it) with an unrestricted `to = GRAVEYARD` event pattern, so it redirects every
 * card *and* token from every zone — the engine matches the filter (`Any`) against whatever
 * would be put into a graveyard.
 */
val RestInPeace = card("Rest in Peace") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile all graveyards.\n" +
        "If a card or token would be put into a graveyard from anywhere, exile it instead."

    // When this enchantment enters, exile all graveyards.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.Each,
                        filter = GameObjectFilter.Any,
                    ),
                    storeAs = "allGraveyards",
                ),
                MoveCollectionEffect(
                    from = "allGraveyards",
                    destination = CardDestination.ToZone(Zone.EXILE),
                ),
            )
        )
    }

    // If a card or token would be put into a graveyard from anywhere, exile it instead.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Terese Nielsen"
        flavorText = "Some corpses the Golgari cannot claim. Some souls the Orzhov cannot shackle."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37c2b1d1-faa0-40fd-82f4-216604ce7635.jpg?1562784882"
    }
}
