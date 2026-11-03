package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Grand Nagus Zek
 * {2}{B}
 * Legendary Creature — Ferengi Noble
 * 1/1
 *
 * Grand Nagus Zek gets +1/+1 for each Treasure you control.
 * Whenever another creature dies, create a Treasure token. This ability triggers only once each turn.
 */
val GrandNagusZek = card("Grand Nagus Zek") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Ferengi Noble"
    power = 1
    toughness = 1
    oracleText = "Grand Nagus Zek gets +1/+1 for each Treasure you control.\n" +
        "Whenever another creature dies, create a Treasure token. This ability triggers only once each turn. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    val treasures = DynamicAmounts.count(
        Player.You,
        Zone.BATTLEFIELD,
        GameObjectFilter.Artifact.withSubtype(Subtype.TREASURE).youControl()
    )

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = treasures,
            toughnessBonus = treasures
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        oncePerTurn = true
        effect = Effects.CreateTreasure(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Wei Guan"
        flavorText = "\"You always know what to expect when you do business with the Ferengi.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47488f1e-512d-4dc6-a82d-1ead24d3276a.jpg?1793013572"
    }
}
