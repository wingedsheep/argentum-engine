package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ghitu Lavarunner
 * {R}
 * Creature — Human Wizard
 * 1/2
 * As long as there are two or more instant and/or sorcery cards in your graveyard,
 * Ghitu Lavarunner gets +1/+0 and has haste.
 */
val GhituLavarunner = card("Ghitu Lavarunner") {
    manaCost = "{R}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "As long as there are two or more instant and/or sorcery cards in your graveyard, Ghitu Lavarunner gets +1/+0 and has haste."

    val condition = Compare(
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery),
        ComparisonOperator.GTE,
        DynamicAmount.Fixed(2)
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantDynamicStatsEffect(
                filter = GroupFilter.source(),
                powerBonus = DynamicAmount.Fixed(1),
                toughnessBonus = DynamicAmount.Fixed(0)
            ),
            condition = condition
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.source()),
            condition = condition
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Jesper Ejsing"
        flavorText = "\"Tolarians teach the theory of pyromancy. The Ghitu prefer applied research.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c448ba82-a502-459f-9ebc-fc9e85674e6c.jpg?1562742489"
    }
}
