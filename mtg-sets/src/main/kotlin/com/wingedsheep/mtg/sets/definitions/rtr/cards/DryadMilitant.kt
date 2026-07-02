package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Dryad Militant — Return to Ravnica #214 (canonical printing)
 * {G/W} · Creature — Dryad Soldier · 2/1
 *
 * If an instant or sorcery card would be put into a graveyard from anywhere, exile it instead.
 *
 * A static replacement effect, the instant/sorcery-only sibling of Rest in Peace's blanket
 * "card or token" redirect: the reusable [RedirectZoneChange] with an unrestricted
 * `to = GRAVEYARD` event pattern filtered to `GameObjectFilter.InstantOrSorcery`, so it redirects
 * any player's instant or sorcery *card* headed to any graveyard (from the stack, hand, library,
 * or battlefield) into exile instead. Tokens are never cards, so token spells are unaffected.
 */
val DryadMilitant = card("Dryad Militant") {
    manaCost = "{G/W}"
    colorIdentity = "GW"
    typeLine = "Creature — Dryad Soldier"
    power = 2
    toughness = 1
    oracleText = "({G/W} can be paid with either {G} or {W}.)\n" +
        "If an instant or sorcery card would be put into a graveyard from anywhere, exile it instead."

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.InstantOrSorcery,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Terese Nielsen"
        flavorText = "\"We will defend the Worldsoul from Izzet 'progress' at any cost.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bb8cb8c-0d03-4cbf-b7f2-a97324817698.jpg?1782714188"
    }
}
