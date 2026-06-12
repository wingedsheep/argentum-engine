package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Crusading Knight
 * {2}{W}{W}
 * Creature — Human Knight
 * 2/2
 * Protection from black
 * This creature gets +1/+1 for each Swamp your opponents control.
 */
val CrusadingKnight = card("Crusading Knight") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Protection from black\nThis creature gets +1/+1 for each Swamp your opponents control."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.BLACK)))

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.Count(
                player = Player.EachOpponent,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Land.withSubtype("Swamp")
            ),
            toughnessBonus = DynamicAmount.Count(
                player = Player.EachOpponent,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Land.withSubtype("Swamp")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"My only dream is to destroy the nightmares of others.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4ab4640-1871-41dd-bd21-64741e21ba37.jpg?1562928291"
    }
}
