package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Goblin Tomb Raider
 * {R}
 * Creature — Goblin Pirate
 * 1/2
 * As long as you control an artifact, this creature gets +1/+0 and has haste.
 */
val GoblinTombRaider = card("Goblin Tomb Raider") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Pirate"
    power = 1
    toughness = 2
    oracleText = "As long as you control an artifact, this creature gets +1/+0 and has haste."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantDynamicStatsEffect(
                filter = GroupFilter.source(),
                powerBonus = DynamicAmount.Fixed(1),
                toughnessBonus = DynamicAmount.Fixed(0)
            ),
            condition = Conditions.ControlArtifact
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.source()),
            condition = Conditions.ControlArtifact
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/018160fe-f602-43f5-8495-241a08eaa69c.jpg?1782694488"
    }
}
