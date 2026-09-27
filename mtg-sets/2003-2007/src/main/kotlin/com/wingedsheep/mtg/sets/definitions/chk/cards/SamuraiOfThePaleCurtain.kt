package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Samurai of the Pale Curtain
 * {W}{W}
 * Creature — Fox Samurai
 * 2/2
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 * If a permanent would be put into a graveyard, exile it instead.
 *
 * The static is the shared [RedirectZoneChange] graveyard -> exile replacement
 * (Rest in Peace, Leyline of the Void) scoped to `from = BATTLEFIELD`: a "permanent" is only an
 * object on the battlefield, so cards milled, discarded, or resolving as spells still reach the
 * graveyard. Tokens are permanents too and are exiled like any other.
 */
val SamuraiOfThePaleCurtain = card("Samurai of the Pale Curtain") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Samurai"
    power = 2
    toughness = 2
    oracleText = "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)\n" +
        "If a permanent would be put into a graveyard, exile it instead."

    keywordAbility(KeywordAbility.bushido(1))

    // If a permanent would be put into a graveyard, exile it instead.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any,
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ead15cf8-f692-4cb9-ac86-0dff00236145.jpg?1783944332"
    }
}
