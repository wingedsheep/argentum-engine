package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.BattlefieldDirection
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Gandalf the White
 * {3}{W}{W}
 * Legendary Creature — Avatar Wizard
 * 4/5
 *
 * Flash
 * You may cast legendary spells and artifact spells as though they had flash.
 * If a legendary permanent or an artifact entering or leaving the battlefield causes a
 * triggered ability of a permanent you control to trigger, that ability triggers an
 * additional time.
 *
 * All three clauses compose from existing primitives:
 *  - Flash keyword (CR 702.8) — `keywords(Keyword.FLASH)`.
 *  - Flash permission static (CR 702.8a) — [GrantFlashToSpellType] with filter
 *    `Artifact ∨ Legendary`, controller-only.
 *  - Trigger-count modifier (CR 603.2d "An ability may state that a triggered ability
 *    triggers additional times") — [AdditionalETBOrLTBTriggers] with the same
 *    `Artifact ∨ Legendary` filter, `mustBeYouControl = false` (the cause permanent
 *    can be either player's — the oracle text only requires the *trigger* to belong
 *    to a permanent Gandalf's controller controls), and both `ENTERING` and `LEAVING`
 *    directions watched.
 */
val GandalfTheWhite = card("Gandalf the White") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 4
    toughness = 5
    oracleText = "Flash\n" +
        "You may cast legendary spells and artifact spells as though they had flash.\n" +
        "If a legendary permanent or an artifact entering or leaving the battlefield causes " +
        "a triggered ability of a permanent you control to trigger, that ability triggers an " +
        "additional time."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Any.legendary(),
            controllerOnly = true
        )
    }

    staticAbility {
        ability = AdditionalETBOrLTBTriggers(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Any.legendary(),
            mustBeYouControl = false,
            directions = setOf(BattlefieldDirection.ENTERING, BattlefieldDirection.LEAVING),
            description = "If a legendary permanent or an artifact entering or leaving the battlefield causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "19"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e384c20b-d0c1-4781-9d11-e89e5a6bf3fc.jpg?1686967821"
    }
}
