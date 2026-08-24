package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Rampart Crawler
 * {B}
 * Creature — Lizard Mercenary
 * 1 / 1
 *
 * Juggernaut's / Bog Rats' blocking restriction, consumed by `CantBeBlockedByRule` in
 * `BlockEvasionRules`. The blocker noun is the bare tribal "Walls", i.e. Wall *permanents* —
 * `GameObjectFilter.Permanent.withSubtype`, not `.Creature` (which the older corpus cards use).
 * `filter` is left at its `GroupFilter.source()` default: the restriction applies to this
 * creature only.
 */
val RampartCrawler = card("Rampart Crawler") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Lizard Mercenary"
    oracleText = "This creature can't be blocked by Walls."
    power = 1
    toughness = 1

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Permanent.withSubtype(Subtype.WALL))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Pete Venters"
        flavorText = "\"Not even the magistrate in his lofty tower is out of our reach.\"\n" +
            "—Cateran overlord"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b60f86f-c78a-4dfb-bb18-e9bcf21b26c4.jpg"
    }
}
