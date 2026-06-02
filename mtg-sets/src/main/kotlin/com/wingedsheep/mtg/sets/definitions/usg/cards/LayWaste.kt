package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.dsl.Effects

/**
 * Lay Waste
 * {3}{R}
 * Sorcery
 * Destroy target land.
 * Cycling {2}
 */
val LayWaste = card("Lay Waste") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target land.\nCycling {2}"

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.Land))
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46fa1186-51fa-419a-9cd0-42403d1dd4a7.jpg?1562909719"
    }
}
