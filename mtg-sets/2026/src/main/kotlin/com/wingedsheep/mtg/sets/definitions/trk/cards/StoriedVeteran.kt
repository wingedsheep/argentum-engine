package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Storied Veteran
 * {B}
 * Creature — Klingon Advisor
 * 2/1
 *
 * This creature can't block.
 * You may cast this card from your graveyard. If you do, it enters with a finality counter on it.
 */
val StoriedVeteran = card("Storied Veteran") {
    manaCost = "{B}"
    typeLine = "Creature — Klingon Advisor"
    colorIdentity = "B"
    oracleText = "This creature can't block.\n" +
        "You may cast this card from your graveyard. If you do, it enters with a finality counter on it."
    power = 2
    toughness = 1

    staticAbility {
        ability = CantBlock()
    }

    // You may cast this card from your graveyard...
    staticAbility {
        ability = MayCastSelfFromZones(zones = listOf(Zone.GRAVEYARD))
    }

    // ...If you do, it enters with a finality counter on it.
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.FINALITY,
            count = 1,
            selfOnly = true,
            condition = Conditions.WasCastFromGraveyard
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Ana Teresa Rivera"
        flavorText = "\"There we were at the K-7 cantina. Kirk's savage crew breathing down our necks. " +
            "Their tribbles with fangs as sharp as a *mek'leth*.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b80995c-8e9a-4a9c-8854-59439a58166f.jpg?1792753937"
    }
}
