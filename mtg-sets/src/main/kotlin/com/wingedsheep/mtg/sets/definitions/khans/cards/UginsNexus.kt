package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventExtraTurns
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWithEffect
import com.wingedsheep.sdk.scripting.effects.TakeExtraTurnEffect

/**
 * Ugin's Nexus
 * {5}
 * Legendary Artifact
 *
 * If a player would begin an extra turn, that player skips that turn instead.
 * If Ugin's Nexus would be put into a graveyard from the battlefield, instead
 * exile it and take an extra turn after this one.
 */
val UginsNexus = card("Ugin's Nexus") {
    manaCost = "{5}"
    typeLine = "Legendary Artifact"
    oracleText = "If a player would begin an extra turn, that player skips that turn instead.\n" +
            "If Ugin's Nexus would be put into a graveyard from the battlefield, instead exile it and take an extra turn after this one."

    // Ability 1: Prevent extra turns
    replacementEffect(PreventExtraTurns())

    // Ability 2: Self-exile replacement + extra turn
    replacementEffect(
        RedirectZoneChangeWithEffect(
            newDestination = Zone.EXILE,
            additionalEffect = TakeExtraTurnEffect(),
            selfOnly = true,
            appliesTo = GameEvent.ZoneChangeEvent(
                filter = GameObjectFilter.Any,
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            )
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "227"
        artist = "Sam Burley"
        flavorText = "All at once Sarkhan's mind fell silent."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94002868-a48a-4ea8-bfce-17257078f5db.jpg?1562790526"
    }
}
