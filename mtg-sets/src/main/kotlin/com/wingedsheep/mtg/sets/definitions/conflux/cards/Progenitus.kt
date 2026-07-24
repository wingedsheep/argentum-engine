package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Progenitus — Conflux #121 (canonical printing; reprinted in Foundations #244).
 *
 * Protection from everything is a single parameterized keyword ability. Its graveyard
 * replacement is intrinsic to the card, so it applies from every zone, including when
 * Progenitus is discarded, milled, countered, or would die.
 */
val Progenitus = card("Progenitus") {
    manaCost = "{W}{W}{U}{U}{B}{B}{R}{R}{G}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Hydra Avatar"
    oracleText = "Protection from everything\n" +
        "If Progenitus would be put into a graveyard from anywhere, reveal Progenitus and " +
        "shuffle it into its owner's library instead."
    power = 10
    toughness = 10

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Everything))

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.LIBRARY,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
            shuffleIntoLibrary = true,
            reveal = true,
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "121"
        artist = "Jaime Jones"
        flavorText = "The Soul of the World has returned."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcc764b0-3046-4bde-b424-c0f4e1a6169b.jpg?1783942466"
        ruling(
            "2009-02-01",
            "\"Protection from everything\" means Progenitus can't be blocked, enchanted or " +
                "equipped, targeted, or dealt damage."
        )
        ruling(
            "2009-02-01",
            "Progenitus can still be affected by effects that neither target it nor deal damage to it."
        )
    }
}
