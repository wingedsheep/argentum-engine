package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Vortex Runner — Strixhaven: School of Mages #60 (canonical printing)
 * {2}{U} · Creature — Human Wizard · 2/3
 *
 * As long as you control eight or more lands, this creature gets +1/+0 and can't be blocked.
 *
 * Two self-scoped statics gated by the same [Conditions.ControlLandsAtLeast] — Augmenter
 * Pugilist's conditional [ModifyStats] and a conditional [CantBeBlocked] (Gutter Skulker's shape).
 */
val VortexRunner = card("Vortex Runner") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "As long as you control eight or more lands, this creature gets +1/+0 and can't be blocked."

    staticAbility {
        ability = ModifyStats(1, 0, Filters.Self)
        condition = Conditions.ControlLandsAtLeast(8)
    }

    staticAbility {
        ability = CantBeBlocked(filter = GroupFilter.source())
        condition = Conditions.ControlLandsAtLeast(8)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Billy Christian"
        flavorText = "\"By using the vortex, I can study for at least fifteen extra minutes between every " +
            "class. I wasn't being reckless, Dean Imbraham. I was being efficient.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bab4a68d-5d26-42d8-bc51-15e3c1f95ebe.jpg?1783927372"
    }
}
